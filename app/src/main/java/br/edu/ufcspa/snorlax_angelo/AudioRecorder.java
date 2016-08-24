package br.edu.ufcspa.snorlax_angelo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StatFs;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.view.Menu;
import android.view.MenuItem;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import ufcspa.edu.br.sono_angelo_v2.R;

public class AudioRecorder extends AppCompatActivity {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "Snore_angELO";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final long BYTES_HORA = 320197200;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private Thread processingThread = null;
    private boolean isRecording = false;
    private boolean isProcessing = false;
    private String fileToprocess = "";

    private Chronometer cronometro;
    private Button btn_gravacao;
    private TextView txt_status;
    private TextView txt_cap2;

    private AlertDialog alerta;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recorder);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);

        cronometro = (Chronometer) findViewById(R.id.cronometro);

        btn_gravacao = ((Button)findViewById(R.id.btn_gravacao));
        btn_gravacao.setOnClickListener(btnClick);

        txt_status = ((TextView)findViewById(R.id.txt_status));

        builder = new AlertDialog.Builder(this);
        builder.setTitle("Snore | angELO");
        builder.setMessage("Gravação finalizada e salva na pasta Snore_angELO!");
        builder.setCancelable(false);
        //define um botão como positivo
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });
        alerta = builder.create();

        txt_cap2 = ((TextView)findViewById(R.id.txt_cap2));
        txt_cap2.setText("  " + calculaCapHoras()+" horas");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_audio_recorder, menu);
        return true;

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Inflate the menu; this adds items to the action bar if it is present.

        switch (item.getItemId()){
            case R.id.action_gravacoes:
                Toast.makeText(AudioRecorder.this, "Falta desenvolver...", Toast.LENGTH_SHORT).show();
            /*case R.id.action_grafico:
                Intent intent2 = new Intent(this, TelaGrafico.class); //Cria intent detalhes
                startActivity(intent2); //Ativa a nova intent
                return true;*/

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume() {
        super.onResume();
        txt_cap2.setText("  " + calculaCapHoras() + " horas");
    }

    private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + "Final_" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,System.currentTimeMillis()+".raw");

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis()+".raw");
    }

    private String getFinalTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                getAudioData();
            }
        },"AudioRecorder Thread");

        recordingThread.start();

        processingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                processAudioData();
            }
        },"Process Audio Data Thread");

        processingThread.start();
    }

    private void getAudioData(){
        long record_size = 60000; //1 minute

        fileToprocess = "";
        isProcessing = true;
        while(isRecording) {
            fileToprocess = writeAudioDataToFile(record_size);
        }
        isProcessing = false;
    }

    private void processAudioData(){

        //Create final processed audio file
        FileOutputStream audioFinal = null;
        String filename = getFinalTempFilename();

        try{
            audioFinal = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileInputStream inAudioData = null;

        while (isProcessing || fileToprocess.compareToIgnoreCase("") > 0){
            if (fileToprocess.compareToIgnoreCase("") > 0){ //exist a file to process

                byte[] data = new byte[bufferSize];
                System.out.println("*** PROCESSANDO NOVO TEMP AUDIO");
                try{
                    inAudioData = new FileInputStream(fileToprocess);

                    while(inAudioData.read(data) != -1){
                        audioFinal.write(data);
                    }

                    inAudioData.close();
                    deleteTempFile(fileToprocess);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                fileToprocess = "";
            }

        }

        try{
            audioFinal.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Create final audio file
        copyWaveFile(getFinalTempFilename(),getFilename());
        deleteFinalTempFile();

    }

    private String writeAudioDataToFile(long record_size){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;
        Long minuteIni;
        Long minuteAtu;
        boolean limiteTime = true;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        System.out.println("*** INICIANDO GRAVAÇÃO");
        if(null != os){
            minuteIni = cronometro.getDrawingTime();
            while(isRecording && limiteTime){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {

                        minuteAtu = cronometro.getDrawingTime();
                        if ((minuteAtu - minuteIni) > 60000){
                            System.out.println("*** UM MINUTO: " + cronometro.getDrawingTime());
                            limiteTime = false;
                        }
                        else{
                            os.write(data);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return filename;
    }

    private void stopRecording(){
        if(null != recorder){
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;

            processingThread = null;
        }

    }

    private void deleteFinalTempFile() {
        File file = new File(getFinalTempFilename());
        file.delete();
    }

    private void deleteTempFile(String filename) {
        File file = new File(filename);
        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1; //2
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(!isRecording) {
                AppLog.logString("Start Recording");
                startRecording();
                cronometro.setBase(SystemClock.elapsedRealtime());
                cronometro.start();
                btn_gravacao.setText(getString(R.string.btn_parar));
                txt_status.setText(getString(R.string.granvando));
                Toast.makeText(AudioRecorder.this, "Gravação Iniciada!", Toast.LENGTH_SHORT).show();
            }
            else{
                AppLog.logString("Stop Recording");
                stopRecording();
                cronometro.stop();

                alerta.show();

                cronometro.setBase(SystemClock.elapsedRealtime());

                btn_gravacao.setText(getString(R.string.btn_inicar));
                txt_status.setText(getString(R.string.inicar_captura));

            }

        }
    };

    public static float megabytesAvailable() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        return bytesAvailable / (1024.f * 1024.f);
    }

    public static float bytesAvailable() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        return bytesAvailable;
    }

    public static int calculaCapHoras() {
        int horas = 0;
        horas = (int)Math.floor((bytesAvailable()/BYTES_HORA));
        return horas;
    }

}
