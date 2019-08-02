package com.example.hotspotshare;
import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    TextView wifiDisplayView;
    TextView wifiStateView;
    TextView ipListView;

    TextView tvAudioCurtentTime;
    TextView tvAudioTotalTime;
    SeekBar  sbAudioBar;

    TextView mobileDataState;
    Switch wifiSwitch;
    Button playButton;
    Button pauseButton;
    Button stopButton;

    boolean isAllNecessaryPermissionsEnabled = false;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private HotspotService hotspotService;
    boolean isServiceHasBeenOpened = false;
    boolean isSeekbarChaning = false;  //互斥变量，防止进度条和定时器冲突。

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAllNecessaryPermissions();

        initWifiHotspotDisplay();
        enableMobileData(true);
        setMobileDataState(true);

        File srcFile = new File(Environment.getExternalStorageDirectory(), "Dream_It_Possible.flac");  //  audio source file path
        File destDir = FileOperation.getFilePath(this,"sonic_audio"); // destination directory file path
        FileOperation.copyFileToAppDirectory(srcFile, destDir);

        initMediaPlayer();

        mobileDataState.setText(SsidAndPreshareKeyToJNI.transmitSsidAndPreshareKeyToJNI("! Intel"));

        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked && isAllNecessaryPermissionsEnabled) {
                    isServiceHasBeenOpened = true;
                    Intent bindIntent = new Intent(MainActivity.this, HotspotService.class);
                    bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
                }
                else if(isServiceHasBeenOpened) {
                    wifiStateView.setText("HotspotState:Closed\n");
                    wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");
                    unbindService(serviceConnection);
                    isServiceHasBeenOpened = false;
                }
            }
        });
    }


    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HotspotService.HotspotBinder binder = (HotspotService.HotspotBinder) service;
            hotspotService = binder.getService();

            hotspotService.setOnDatCallback(new HotspotService.OnDataCallback() {
                @Override
                public void onDataChange(final HotspotService.HotspotStateInfo hotspotStateInfo) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (hotspotStateInfo.hotSpotEnabledState) {
                                wifiStateView.setText("HotspotState:Open\n");
                                wifiDisplayView.setText("Ssid:" + hotspotStateInfo.SSID + "\n" + "Pwd:" + hotspotStateInfo.preShareKey);
                            }
                            else {
                                wifiStateView.setText("HotspotState:Closed\n");
                                wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");
                            }
                            ipListAndNumDisplay();
                        }
                    });
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            hotspotService = null;
        }
    };

    public ArrayList<String> getConnectedIP(){
        ArrayList<String> connectedIp=new ArrayList<String>();
        try {
            BufferedReader br=new BufferedReader(new FileReader(
                    "/proc/net/arp"));
            String line;
            while ((line=br.readLine())!=null){
                String[] splitted=line.split(" +");
                if (splitted.length>=4){
                    String ip=splitted[0];
                    if (!ip.equalsIgnoreCase("ip")){
                        connectedIp.add(ip);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connectedIp;
    }

    public void setMobileDataState(boolean enabled) {
        TelephonyManager telephonyService = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled",boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyService, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean enableMobileData(boolean on) {
        try {
            ConnectivityManager mConnectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Method method = mConnectivityManager.getClass().getMethod("setMobileDataEnabled", boolean.class);
            method.invoke(mConnectivityManager, on);
            return true;
        } catch (IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void initWifiHotspotDisplay(){
        wifiDisplayView = findViewById(R.id.wifi_display);
        wifiStateView = findViewById(R.id.wifi_state);
        ipListView = findViewById(R.id.ip_list);
        wifiSwitch = findViewById(R.id.wifi_switch);
        mobileDataState = findViewById(R.id.mobile_data_state);

        playButton =  findViewById(R.id.button_play);
        pauseButton = findViewById(R.id.button_pause);
        stopButton =  findViewById(R.id.button_stop);
        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        wifiStateView.setText("HotspotState:Closed\n");
        wifiDisplayView.setText("Ssid:"+"null"+"\n"+"Pwd:"+"null");
        ipListView.setText("Device Num:"+ getConnectedIP().size()+"\n");
    }

    void ipListAndNumDisplay(){
        ipListView.setText("Device Num:"+ getConnectedIP().size()+"\n");
        for(int i=0;i<getConnectedIP().size();i++){
            ipListView.append("IP:"+getConnectedIP().get(i)+"\n");
        }
    }

    private void requestAllNecessaryPermissions() {
        List<String> permissionList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1002);
        } else {
            isAllNecessaryPermissionsEnabled = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1002:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED){
                            Toast.makeText(MainActivity.this, permissions[i] + " Permission Refused!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    isAllNecessaryPermissionsEnabled = true;
                }
                break;
        }
    }

    public void initMediaPlayer(){
        audioWedgtLink();
        File file = new File(this.getExternalFilesDir("sonic_audio"), "Dream_It_Possible.flac");
        try {
            mediaPlayer.setDataSource(file.getPath()); // set the audio file path
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.setOnPreparedListener(preparedListener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int audioDuration = mediaPlayer.getDuration();
        int audioCurrentPosition = mediaPlayer.getCurrentPosition();

        tvAudioCurtentTime.setText(formatTime(audioCurrentPosition));
        tvAudioTotalTime.setText(formatTime(audioDuration));
    }

    boolean mediaPreparedStateFlag;
    MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPreparedStateFlag = true;
            Log.e("MediaPlayer","Audio Prepared");
        }
    };

    public void audioWedgtLink()
    {
        tvAudioCurtentTime = findViewById(R.id.audio_current_time);
        tvAudioTotalTime = findViewById(R.id.audio_total_time);
        sbAudioBar = findViewById(R.id.audio_seekbar);

        sbAudioBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sbAudioBar, int progress, boolean fromUser) {
                int audioDuration = mediaPlayer.getDuration();//获取音乐总时长
                int position = mediaPlayer.getCurrentPosition();//获取当前播放的位置
                tvAudioCurtentTime.setText(formatTime(position));//开始时间
                tvAudioTotalTime.setText(formatTime(audioDuration));//总时长
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekbarChaning = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekbarChaning = false;
                mediaPlayer.seekTo(seekBar.getProgress());//在当前位置播放
                tvAudioCurtentTime.setText(formatTime(mediaPlayer.getCurrentPosition()/1000));
            }
        });
    }

    public String calculateTime(int time){
        int minute;
        int second;
        if(time > 60){
            minute = time / 60;
            second = time % 60;
            if(minute >= 0 && minute < 10){
                if(second >= 0 && second < 10){
                    return "0"+minute+":"+"0"+second;
                }else {
                    return "0"+minute+":"+second;
                }
            }else {
                if(second >= 0 && second < 10){
                    return minute+":"+"0"+second;
                }else {
                    return minute+":"+second;
                }
            }
        }else if(time < 60){
            second = time;
            if(second >= 0 && second < 10){
                return "00:"+"0"+second;
            }else {
                return "00:"+ second;
            }
        }
        return null;
    }

    private String formatTime(int length){
        Date date = new Date(length);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm:ss");
        String totalTime = simpleDateFormat.format(date);
        return totalTime;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_play:
                if (!mediaPlayer.isPlaying() && mediaPreparedStateFlag) {
                    mediaPlayer.start(); // just when the hotspot has been opened,the audio can be played

                    int duration = mediaPlayer.getDuration();//get the audio total time length
                    sbAudioBar.setMax(duration);//set the max value of the seekbar
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if(!isSeekbarChaning){
                                sbAudioBar.setProgress(mediaPlayer.getCurrentPosition());
                            }
                        }
                    },0,50);
                }
                break;

            case R.id.button_pause:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                break;

            case R.id.button_stop:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.reset();
                    initMediaPlayer();
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }
}