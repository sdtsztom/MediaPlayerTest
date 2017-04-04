package com.test.acer.amediaplayer;

/**
 * Created by acer on 2017/3/20.
 * release on 2017/4/3
 * vision:0.4
 * time:1
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private MediaPlayer mediaPlayer=new MediaPlayer();
    String path;

    //重写创建函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button play = (Button) findViewById(R.id.play);
        Button pause = (Button) findViewById(R.id.pause);
        Button stop = (Button) findViewById(R.id.stop);
        Button choose=(Button)findViewById(R.id.choose);
        ToggleButton choosePatternButton=(ToggleButton)findViewById(R.id.choosePattern);
        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);
        choose.setOnClickListener(this);
        choosePatternButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    mediaPlayer.setLooping(true);  //切换播放模式为单曲循环
                }else{
                    mediaPlayer.setLooping(false);  //切换播放模式为单曲播放
                }
            }
        });

        //检验权限
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
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
            case R.id.exit:
                finish();
                break;
            default:
        }
        return true;
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

    //选择音乐路径
    public void chooseMusic(){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(intent.CATEGORY_OPENABLE);
        try{
            startActivityForResult(Intent.createChooser(intent,"请选择一个音乐文件"),1);
        }catch(android.content.ActivityNotFoundException e){
            Toast.makeText(this,"请安装文件管理器",Toast.LENGTH_SHORT).show();
        }
    }

    //取得与设置音乐路径
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
                    try{
                        if(path!=null){
                            Boolean looping=mediaPlayer.isLooping();
                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(path);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            mediaPlayer.setLooping(looping);
                        }
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                }
        }

    }

    //onclick监听器
    @Override
    public void onClick(View v){
            switch(v.getId()){
                case R.id.play:
                    if(path==null){
                        Toast.makeText(this,"请选择音乐",Toast.LENGTH_SHORT).show();
                    }else if(!mediaPlayer.isPlaying()){
                        mediaPlayer.start();
                    }
                    break;
                case R.id.pause:
                    if(mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                    }
                    break;
                case R.id.stop:
                    if(mediaPlayer.isPlaying()){
                        mediaPlayer.seekTo(0);
                        mediaPlayer.pause();
                    }
                    break;
                case R.id.choose:
                    chooseMusic();
                default:
                    break;
            }
        }

    //将返回键效果改为进入后台
    @Override
    public void onBackPressed(){
            moveTaskToBack(false);
        }

    //摧毁事件
    @Override
    protected void onDestroy(){
            super.onDestroy();
            if(mediaPlayer!=null){
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }
}
