package tr.olgun.alkis;

import android.app.*;
import android.content.*;
import android.media.*;
import android.hardware.*;
import android.os.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Listener extends Service implements SensorEventListener {
    private SensorManager sensors;
    private Pickup pickup;
    static final String QUIET="quiet", OFF="off";
    static volatile boolean active, ringing;
    static volatile String error="";
    private final Handler main=new Handler(Looper.getMainLooper());
    private final AtomicBoolean running=new AtomicBoolean();
    private AudioRecord mic;
    private AudioTrack bell;
    private PowerManager.WakeLock wake;
    private final Runnable silence=this::quiet;

    private PendingIntent command(String action) {
        return PendingIntent.getService(this, action.hashCode(), new Intent(this,Listener.class).setAction(action),
                PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private Notification note() {
        Notification.Builder b=new Notification.Builder(this,"listen")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Alkışla Bul")
            .setContentText(ringing?"Telefon burada!":"İki alkış dinleniyor")
            .setOnlyAlertOnce(true).setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,Home.class),PendingIntent.FLAG_IMMUTABLE));
        if(ringing) b.addAction(0,"Sustur",command(QUIET));
        b.addAction(0,"Dinlemeyi kapat",command(OFF));
        return b.build();
    }
    public int onStartCommand(Intent i,int flags,int id) {
        String action=i==null?null:i.getAction();
        if(OFF.equals(action)){stopSelf();return START_NOT_STICKY;}
        if(QUIET.equals(action)){quiet();if(!running.get())stopSelf();return running.get()?START_STICKY:START_NOT_STICKY;}
        if(running.get()) return START_STICKY;
        getSystemService(NotificationManager.class).createNotificationChannel(
            new NotificationChannel("listen","Alkış dinleme",NotificationManager.IMPORTANCE_LOW));
        try {
            startForeground(1,note());
            int size=AudioRecord.getMinBufferSize(16000,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
            mic=new AudioRecord(MediaRecorder.AudioSource.MIC,16000,AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,Math.max(size,4096));
            mic.startRecording();
            if(mic.getRecordingState()!=AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException();
            wake=getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Alkis:Listening");
            wake.acquire();
            error=""; active=true; running.set(true);
            new Thread(this::listen,"Alkis-Mic").start();
        } catch(RuntimeException e){error="Mikrofon açılamadı. Mikrofon iznini ve erişimini kontrol edin.";stopSelf();}
        return running.get()?START_STICKY:START_NOT_STICKY;
    }
    private void listen(){
        AudioRecord input=mic;
        Claps detector=new Claps();
        short[] frame=new short[160];
        int filled=0;
        try {
            while(running.get()){
                int n=input.read(frame,filled,frame.length-filled);
                if(n<=0) throw new IllegalStateException();
                filled+=n;
                if(filled!=frame.length) continue;
                filled=0;
                if(ringing) {detector=new Claps(); continue;}
                if(detector.accept(frame,SystemClock.elapsedRealtime())) main.post(this::ring);
            }
        } catch(RuntimeException e){
            if(running.get()) main.post(()->{error="Mikrofon bağlantısı kesildi. Dinlemeyi yeniden açın.";stopSelf();});
        } finally {input.release();}
    }
    private void ring(){
        if(!running.get()||ringing) return;
        try {
            int rate=16000;
            short[] sound=new short[rate*2];
            for(int i=0;i<sound.length;i++){
                double t=i/(double)rate, beat=t%0.5;
                if(beat<0.34) sound[i]=(short)(21000*Math.min(1,Math.min(beat,0.34-beat)*100)*
                        Math.sin(2*Math.PI*(t<1?880:1174)*t));
            }
            bell=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(sound.length*2).build();
            if(bell.write(sound,0,sound.length)!=sound.length || bell.setLoopPoints(0,sound.length,-1)!=AudioTrack.SUCCESS)
                throw new IllegalStateException();
            ringing=true; bell.play();
            sensors=getSystemService(SensorManager.class);
            Sensor accel=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if(accel!=null){
                pickup=new Pickup();
                sensors.registerListener(this,accel,SensorManager.SENSOR_DELAY_GAME);
            }
            getSystemService(NotificationManager.class).notify(1,note());
            main.postDelayed(silence,30000);
        }catch(RuntimeException e){quiet();error="Alarm sesi başlatılamadı.";stopSelf();}
    }
    public void onSensorChanged(SensorEvent e){
        if(ringing && pickup!=null && pickup.moved(e.timestamp,e.values[0],e.values[1],e.values[2])) quiet();
    }
    public void onAccuracyChanged(Sensor sensor,int accuracy){}
    private void quiet(){
        if(sensors!=null)sensors.unregisterListener(this);
        pickup=null;
        main.removeCallbacks(silence);
        if(bell!=null){bell.release();bell=null;}
        ringing=false;
        if(active) getSystemService(NotificationManager.class).notify(1,note());
    }
    public void onDestroy(){
        boolean hadReader=running.getAndSet(false);
        active=false;
        quiet();
        if(mic!=null){
            try{mic.stop();}catch(RuntimeException ignored){}
            if(!hadReader) mic.release();
        }
        if(wake!=null && wake.isHeld()) wake.release();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    public IBinder onBind(Intent i){return null;}
}
