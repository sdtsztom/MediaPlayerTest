package com.test.acer.amediaplayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;

public class MusicService extends Service {

    //普通变量
    private static final String TAG = "MusicService";
    MediaPlayer mediaPlayer=new MediaPlayer();
    String path;

    //通知栏相关变量
    RemoteViews remoteViews;
    Notification notification;
    Intent intent;
    Intent intentPlay;
    Intent intentPause;
    Intent intentQuit;
    PendingIntent pi;
    PendingIntent piPlay;
    PendingIntent piPause;
    PendingIntent piQuit;

    //广播相关变量
    PlayReceiver playReceiver=new PlayReceiver();
    PauseReceiver pauseReceiver=new PauseReceiver();

    //服务初始化函数
    public MusicService() {
//        Log.d("MusicService","MusicService initialize");
    }

    //创建MusicBinder实例
    private MusicBinder mBinder=new MusicBinder();

    //定义Binder的子类MusicBinder类
    class MusicBinder extends Binder {
        MusicService getMusicService(){
            return MusicService.this;
        }
    }

    //重写绑定函数
    @Override
    public IBinder onBind(Intent intent) {
       return mBinder;
    }

    //重写创建函数
    @Override
    public void onCreate(){
//        Log.d(TAG,"MusicService create start................................");
        super.onCreate();
        notification();
        registerReceiver();
        setPlayer();
//        Log.d(TAG,"MusicService create finish.....................................");
    }

    //创建通知栏
    void notification(){
//        Log.d(TAG,"notification start.............................");
        intent=new Intent(this,MainActivity.class);
        intentPlay=new Intent("play");
        intentPause=new Intent("pause");
        intentQuit=new Intent("quit");
        pi=PendingIntent.getActivity(this,0,intent,0);
        piPlay=PendingIntent.getBroadcast(this,0,intentPlay,0);
        piPause=PendingIntent.getBroadcast(this,0,intentPause,0);
        piQuit=PendingIntent.getBroadcast(this,0,intentQuit,0);
        remoteViews=new RemoteViews(getPackageName(),R.layout.notifiction);
        remoteViews.setOnClickPendingIntent(R.id.nplay,piPlay);
        remoteViews.setOnClickPendingIntent(R.id.npause,piPause);
        remoteViews.setOnClickPendingIntent(R.id.nquit,piQuit);
        notification=new NotificationCompat.Builder(this)
                .setContentIntent(pi)
                .setSmallIcon(R.drawable.album)
                .setContent(remoteViews)
                .build();
        startForeground(1,notification);
//        Log.d(TAG,"notification finish.............................");
    }

    //更新通知栏
    void updateNotification(boolean def){
        if(def){
            remoteViews.setImageViewResource(R.id.nImage,R.drawable.album);
            notification=new NotificationCompat.Builder(this)
                    .setContentIntent(pi)
                    .setSmallIcon(R.drawable.album)
                    .setContent(remoteViews)
                    .build();
            startForeground(1,notification);
        }else{
            remoteViews.setImageViewBitmap(R.id.nImage,MainActivity.bitmapSmall);
            notification=new NotificationCompat.Builder(this)
                    .setContentIntent(pi)
                    .setSmallIcon(R.drawable.album)
                    .setContent(remoteViews)
                    .build();
            startForeground(1,notification);
        }
    }

    //创建play监听类
    class PlayReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            play();
            MainActivity.status.setText("playing");
        }
    }

    //创建pause监听类
    class PauseReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            pause();
            MainActivity.status.setText("pause");
        }
    }

    //注册广播监听器
    void registerReceiver(){
        IntentFilter playIntentFilter=new IntentFilter();
        IntentFilter pauseIntentFilter=new IntentFilter();
        playIntentFilter.addAction("play");
        pauseIntentFilter.addAction("pause");
        registerReceiver(playReceiver,playIntentFilter);
        registerReceiver(pauseReceiver,pauseIntentFilter);
    }

    //设置音乐播放器路径并初始化和播放
    void setDatabase(){
//        Log.d("MusicService","MusicService.setDatabase start");
        try {
            boolean looping = mediaPlayer.isLooping();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
//                    Log.d("mediaPlayer","mediaPlayer prepare finish");
                }
            });
            mediaPlayer.setLooping(looping);
        }catch(Exception e){
            e.printStackTrace();
        }
//        Log.d("MusicService","MusicService.setDatabase finish");
    }

    //play函数
    void play(){
        if(path==null){
            Toast.makeText(this,"请选择音乐",Toast.LENGTH_SHORT).show();
        }else if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }

    //pause函数
    void pause(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    //stop函数
    void stop(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.seekTo(0);
            mediaPlayer.pause();
        }
    }

    //使用sharedPreferences保存数据
    void save(){
        SharedPreferences.Editor editor=this.getSharedPreferences("status",MODE_PRIVATE).edit();
//        Log.d(TAG,"editor start....................................");
        editor.putString("path",path);
        editor.putInt("playPosition",mediaPlayer.getCurrentPosition());
        editor.putBoolean("playLooping",mediaPlayer.isLooping());
        editor.apply();
//        Log.d(TAG,"editor finish....................................");
    }

    //读取sharedPreferences数据并设置状态
    void setPlayer(){
//        Log.d("MusicService","MusicService.setPlayer start");
        new Thread(new Runnable() {
            @Override
            public void run() {
                File f=new File("/data/data/"+getPackageName()+"/shared_prefs/status.xml");
                if(f.exists()){
                    final SharedPreferences pref=getSharedPreferences("status",MODE_PRIVATE);
                    path=pref.getString("path",null);
                    try {
                        mediaPlayer.setDataSource(path);
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.start();
                                mp.seekTo(pref.getInt("playPosition",0));
                                mp.setLooping(pref.getBoolean("playLooping",false));
                                mp.pause();
//                                Log.d("mediaPlayer","mediaPlayer prepare finish");
                            }
                        });
                    }catch(Exception e){
                        e.printStackTrace();
                    }
//                    Log.d(TAG,pref.getBoolean("haveBitmap",false)+"");
                    if(MainActivity.haveBitmap){
                        updateNotification(false);
                    }else {
                        updateNotification(true);
//                        Log.d(TAG,"updateNotification true.............................");
                    }
                }
            }
        }).start();
//        Log.d("MusicService","MusicService.setPlayer finish");
    }

    //重写摧毁函数
    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy start..............................");
        save();
        super.onDestroy();
        //释放音乐播放器
        if(mediaPlayer!=null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        //解绑play广播监听器
        if(playReceiver!=null){
            unregisterReceiver(playReceiver);
        }
        //解绑pause广播监听器
        if(pauseReceiver!=null){
            unregisterReceiver(pauseReceiver);
        }
        Log.d(TAG,"onDestroy start..............................");
    }

}
