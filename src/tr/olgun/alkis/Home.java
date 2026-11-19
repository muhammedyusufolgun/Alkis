package tr.olgun.alkis;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;

public class Home extends Activity {
    private final int teal=0xff11686c, ink=0xff153639;
    private TextView status;
    private Button toggle, silence;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){render();ui.postDelayed(this,400);}};
    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private TextView label(String s,int size,int color){
        TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;
    }
    private void space(LinearLayout l,int n){l.addView(new Space(this),1,dp(n));}
    private Button button(LinearLayout l,int color){
        Button v=new Button(this);
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(14));
        v.setBackground(shape);v.setTextColor(-1);v.setAllCaps(false);v.setTextSize(17);
        l.addView(v,-1,dp(56));return v;
    }
    public void onCreate(Bundle state){
        super.onCreate(state);
        setVolumeControlStream(android.media.AudioManager.STREAM_ALARM);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xfff2f7f5);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(dp(28),dp(24),dp(28),dp(24)); scroll.addView(l);setContentView(scroll);
        ImageView icon=new ImageView(this);icon.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));LinearLayout.LayoutParams iconSize=new LinearLayout.LayoutParams(dp(64),dp(64));
        iconSize.gravity=Gravity.CENTER_HORIZONTAL;
        l.addView(icon,iconSize);space(l,20);
        TextView title=label("Alkışla Bul",36,ink);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);l.addView(title,-1,-2);
        space(l,8);l.addView(label("Dinlemeyi açın. Telefonun çalması için bir saniye içinde iki kez alkışlayın.",18,ink));
        space(l,26);status=label("",16,teal);l.addView(status);space(l,10);
        toggle=button(l,teal);toggle.setOnClickListener(v->{if(Listener.active)stopService(new Intent(this,Listener.class));else requestStart();});
        space(l,8);silence=button(l,0xff384c57);silence.setText("Alarmı sustur");silence.setOnClickListener(v->startService(new Intent(this,Listener.class).setAction(Listener.QUIET)));
        space(l,16);l.addView(label("Dinleme, ekran kilitliyken de sürer. Alarmı susturmak için telefonu elinize alın.",14,0xff4a6263));
        space(l,24);TextView author=label("Muhammed Yusuf OLGUN",13,0xff526b6a);
        author.setGravity(Gravity.CENTER);l.addView(author,-1,-2);
    }
    private void requestStart(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1);return;
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED &&
                !getPreferences(0).getBoolean("asked",false)){
            getPreferences(0).edit().putBoolean("asked",true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},2);return;
        }
        startForegroundService(new Intent(this,Listener.class));
    }
    public void onRequestPermissionsResult(int code,String[] p,int[] r){
        super.onRequestPermissionsResult(code,p,r);
        if(r.length==0)return;
        if(code==1 && r[0]!=PackageManager.PERMISSION_GRANTED){Listener.error="Dinlemek için mikrofon izni gerekir. İzni uygulama ayarlarından açabilirsiniz.";return;}
        if(code==1||code==2) requestStart();
    }
    private void render(){
        status.setText(Listener.ringing?"Alarm çalıyor":Listener.active?"Dinleme açık":Listener.error.isEmpty()?"Dinleme kapalı":Listener.error);
        toggle.setText(Listener.active?"Dinlemeyi kapat":"Dinlemeyi aç");
        silence.setVisibility(Listener.ringing?View.VISIBLE:View.GONE);
    }
    protected void onResume(){super.onResume();refresh.run();}
    protected void onPause(){ui.removeCallbacks(refresh);super.onPause();}
}
