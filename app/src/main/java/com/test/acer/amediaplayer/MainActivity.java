package com.test.acer.amediaplayer;

/**
 * Created by acer on 2017/3/20.
 * release on 2017/4/27
 * vision:0.6
 * time:4
 */

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //普通变量
    private static final String TAG = "MainActivity";
    private String path;
    private String titleSaved="none";
    private String artistSaved="none";
    private Long songIdSaved=-1l;
    private Long albumIdSaved=-1l;
    public static boolean haveBitmap=false;
    private boolean connected=false;
    private Bitmap bitmap;
    public static Bitmap bitmapSmall;

    //与服务有关变量
    private MusicService.MusicBinder mBinder;
    private MusicService musicService;
    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder=(MusicService.MusicBinder)service;
            musicService=mBinder.getMusicService();
            connected=true;
//            Log.d("Connection","musicService get MusicService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService=null;
        }
    };

    //与view相关变量
    private Button play;
    private Button pause;
    private Button stop;
    private Button choose;
    private Button quit;
    private ToggleButton choosePatternButton;
    public static TextView status;
    private TextView artist;
    private TextView progress;
    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    private SeekBar seekBar;
    private ImageView image;

    //多线程相关变量
    private Handler mHandler=new Handler();
    private Runnable mRunnable=new Runnable() {
        @Override
        public void run() {
//            Log.d(TAG,"run........................................");
            if(connected){
                if (musicService.path != null) {
                    progress.setText(time.format(musicService.mediaPlayer.getCurrentPosition())+"/"+time.format(musicService.mediaPlayer.getDuration()));
                    seekBar.setProgress(musicService.mediaPlayer.getCurrentPosition());
                    seekBar.setMax(musicService.mediaPlayer.getDuration());
                    choosePatternButton.setChecked(musicService.mediaPlayer.isLooping());
                }
            }
            mHandler.postDelayed(mRunnable,100);
        }
    };
    private  QuitReceiver quitReceiver=new QuitReceiver();

    //重写创建函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.d("MainActivity","MainActivity create start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
        registerView();

        //绑定服务
        Intent bindIntent=new Intent(this,MusicService.class);
        bindService(bindIntent,connection,BIND_AUTO_CREATE);

        //读取上次退出时的状态并复原
        setPlayer();

        //注册quit监听器
        registerQuitReceiver();

        //检验权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

//        Log.d("MainActivity","MainActivity create finish");
    }

   //重写resume函数
    @Override
    protected void onResume() {
        super.onResume();
        mHandler.post(mRunnable);
    }

    //创建菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main,menu);
        return true;
    }

    //给菜单添加函数
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.about:
                Intent intent=new Intent(MainActivity.this,AboutActivity.class);
                startActivity(intent);
                break;
            default:
        }
        return true;
    }

    //连接各个View组件
    void bindView(){
        play = (Button) findViewById(R.id.play);
        pause = (Button) findViewById(R.id.pause);
        stop = (Button) findViewById(R.id.stop);
        choose=(Button)findViewById(R.id.choose);
        quit=(Button)findViewById(R.id.quit);
        choosePatternButton=(ToggleButton)findViewById(R.id.choosePattern);
        status=(TextView)findViewById(R.id.status);
        artist=(TextView)findViewById(R.id.artist);
        progress=(TextView)findViewById(R.id.progress);
        seekBar=(SeekBar)findViewById(R.id.seekbar);
        image=(ImageView)findViewById(R.id.image);
    }

    //注册各个View组件的listener
    void registerView(){
        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);
        choose.setOnClickListener(this);
        quit.setOnClickListener(this);
        choosePatternButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    musicService.mediaPlayer.setLooping(true);  //切换播放模式为单曲循环
                }else{
                    musicService.mediaPlayer.setLooping(false);  //切换播放模式为单曲播放
                }
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicService.mediaPlayer.seekTo(seekBar.getProgress());
            }
        });
    }

    //运行时申请权限
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int []grantResults){
            switch (requestCode){
                case 1:
                    if(grantResults.length>0&&grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
                default:
            }
        }

    //调用文件选择器选择音乐路径
    public void chooseMusic(){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try{
            startActivityForResult(Intent.createChooser(intent,"请选择一个音乐文件"),1);
        }catch(android.content.ActivityNotFoundException e){
            Toast.makeText(this,"请安装文件管理器",Toast.LENGTH_SHORT).show();
        }
    }

    //取得路径并调用服务设置音乐路径与播放音乐
    @Override
    public void onActivityResult(int requestCode,int resultCode,Intent data){
        switch(requestCode){
            case 1:
                if(resultCode== Activity.RESULT_OK){
                    Uri uri=data.getData();
                    Cursor cursor=this.getContentResolver().query(uri,null,null,null,null);
                    if(cursor.moveToFirst()) {
                        path = cursor.getString(cursor.getColumnIndex("_data"));
                    }
                    if(path!=null){
                        musicService.path=path;
                        musicService.setDatabase();

                        //获取设置歌曲信息
                        cursor=this.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,MediaStore.Audio.Media.DATA+"=?",new String[]{path},null);
                        if(cursor.moveToFirst()){
                            titleSaved=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                            artistSaved=cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                            songIdSaved=cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                            albumIdSaved=cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                            bitmap=Util.getArtwork(MainActivity.this,songIdSaved,albumIdSaved,false,false);
                            bitmapSmall=Util.getArtwork(MainActivity.this,songIdSaved,albumIdSaved,false,true);
                            if(bitmap!=null){
                                haveBitmap=true;
                                image.setImageBitmap(bitmap);
                                musicService.updateNotification(false);
                            }else{
                                haveBitmap=false;
                                image.setImageResource(R.drawable.album);
                                musicService.updateNotification(true);
                            }
                            artist.setText(titleSaved+"/"+artistSaved);
                        }
                   }
                }
        }

    }

    //play功能函数
    void play(){
        musicService.play();
        status.setText("playing");
    }

    //pause功能函数

    void pause(){
        musicService.pause();
        status.setText("pause");
    }

    //stop功能函数
    void stop(){
        musicService.stop();
        status.setText("stop");
    }

    //onclick监听器
    @Override
    public void onClick(View v){
            switch(v.getId()){
                case R.id.play:
                    musicService.play();
                    break;
                case R.id.pause:
                    musicService.pause();
                    break;
                case R.id.stop:
                    musicService.stop();
                    break;
                case R.id.choose:
                    chooseMusic();
                    break;
                case R.id.quit:
                    finish();
                    break;
                default:
                    break;
            }
        }

    //注册quit监听器
    void registerQuitReceiver(){
        IntentFilter quitIntentFilter=new IntentFilter();
        quitIntentFilter.addAction("quit");
        registerReceiver(quitReceiver,quitIntentFilter);
    }

    //将返回键效果改为进入后台
    @Override
    public void onBackPressed(){
            moveTaskToBack(false);
        }

    //创建quit监听类
    class QuitReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

    //保存状态
    void save(){
        SharedPreferences.Editor editor=this.getSharedPreferences("information",MODE_PRIVATE).edit();
//        Log.d(TAG,"editor start....................................");
        editor.putString("title",titleSaved);
        editor.putString("artist",artistSaved);
        editor.putLong("songId",songIdSaved);
        editor.putLong("albumId",albumIdSaved);
        editor.putBoolean("haveBitmap",haveBitmap);
        editor.apply();
//        Log.d(TAG,"editor finish....................................");
    }

    // 读取上次状态并设置
    void setPlayer(){
        File f=new File("/data/data/"+getPackageName()+"/shared_prefs/information.xml");
        if(f.exists()) {
            status.setText("pause");
            SharedPreferences pref=getSharedPreferences("information",MODE_PRIVATE);
            titleSaved=pref.getString("title","none");
            artistSaved=pref.getString("artist","none");
            songIdSaved=pref.getLong("songId",-1);
            albumIdSaved=pref.getLong("albumId",-1);
            haveBitmap=pref.getBoolean("haveBitmap",false);
            artist.setText(pref.getString("title","none")+"/"+pref.getString("artist","none"));
//            Log.d(TAG,haveBitmap+"..........................................................");
            if(haveBitmap){
                bitmap=Util.getArtwork(MainActivity.this,songIdSaved,albumIdSaved,false,false);
                bitmapSmall=Util.getArtwork(MainActivity.this,songIdSaved,albumIdSaved,false,true);
                image.setImageBitmap(bitmap);
            }
        }
    }

    //重写onStop函数
    @Override
    protected void onStop() {
        super.onStop();
        //回调mRunnable接口
        mHandler.removeCallbacks(mRunnable);
    }

    //重写摧毁函数
    @Override
    protected void onDestroy(){
        //保存状态
        save();

        super.onDestroy();

        //解绑服务
        unbindService(connection);

        //解绑quit广播监听器
        if(quitReceiver!=null){
            unregisterReceiver(quitReceiver);
        }
    }
}
