package com.bkc.android.mdxplayer;

import java.util.Timer;
import java.util.TimerTask;

import com.bkc.android.mdxplayer.R;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.SeekBar;

public class Player extends Activity implements OnClickListener , NotifyBind
{
	// UI�֘A
	TextView time_view;
	TextView title_view;
	TextView file_view;
	TextView vol_view;
	SeekBar  seek_view;
	
	private boolean isStartService = false;
	private boolean isStoped = false;

	String TAG = "MDXPlayer";
	String MDXPlayerTitle;
	
	String KEY_FILEOBJ = "fobj";
	String KEY_LASTFILE = "lastFile";
	String KEY_LASTPATH = "lastPath";
	String KEY_VOL = "volkey";

	SharedPreferences pref;
	final Handler ui_handler = new Handler();
	
	private FileListObject fobj = null;
	private PCMService pcmService = new PCMService();
	
	// ������
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Resources res = getResources();
        
        // �^�C�g���쐬
        MDXPlayerTitle = String.format("%s %s",res.getString(R.string.app_name),res.getString(R.string.app_version));
        
        
        // ��O�n���h���̐ݒ�        
		Thread.setDefaultUncaughtExceptionHandler(
				new EXUncaughtExceptionHandler(this));

        setContentView(R.layout.main);
        
//		Log.d(TAG,"onCreate");

        // �T�[�r�X�ւ̐ڑ�
        doStartService();
        pcmService.doBindService( this , new Intent(Player.this , PCMRender.class ) , this );
        
        fobj = FileListObject.getInst();
        fobj.setContext( getApplicationContext() );
        
        time_view  = (TextView)findViewById(R.id.time_value);
        title_view = (TextView)findViewById(R.id.title_value);
        file_view  = (TextView)findViewById(R.id.filename_value);
        vol_view   = (TextView)findViewById(R.id.volume_value);
        seek_view  = (SeekBar)findViewById(R.id.seektime);
        
        ((TextView)findViewById(R.id.title_value)).setOnClickListener(this);
        
        
        time_view.setOnClickListener(this);    
  
        ((ImageButton)findViewById(R.id.play_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.rev_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.ff_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.voldown_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.volup_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.stop_btn)).setOnClickListener(this);

        
        pref = getSharedPreferences("mdxplay",MODE_PRIVATE);
        fobj.openDirectory( pref.getString(KEY_LASTPATH, "") );
        fobj.setCurrentFilePath( pref.getString(KEY_LASTFILE, "") );
 
    }
    
    // ���j���[�쐬
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate( R.menu.extmenu, menu );
    	return true;
	}

    // ���j���[�I��
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch ( item.getItemId() )
		{
			case R.id.menuItem01:
				startHelpActivity();
			return true;
			case R.id.menuItem02:
				startPrefActivity();
			return true;
			case R.id.menuItem03:				
				startKeyViewActivity();
			return true;
		}
		return false;
	}

	// �ڑ��ʒm
	public void notifyConnected( PCMRender pcm )
    {
    	PCMBound = pcm;
    	
		Log.d(TAG,"notifyConnected");
        if (PCMBound.isPlayed())
        {
        	fobj = PCMBound.getFobj();
        	if ( PCMBound.isPlay() )
        		setPauseButton( true );
        }
        else
        {
        	PCMBound.setFobj( fobj );
        	PCMBound.setVolume( pref.getInt( KEY_VOL , 100) );
        	PCMBound.setTitle( MDXPlayerTitle );
        }
    	PCMBound.doUpdate();
        doScheduleUpdateTimer();
    }
    
	// �ؒf�ʒm
    public void notifyDisconnected()
    {
    	PCMBound = null;
		Log.d(TAG,"notifyDisconnected");
		doCancelTimer();	
    }
   
    // �I������
    @Override
    public void onDestroy()
    {
		Log.d(TAG,"onDestroy");	
		
		pcmService.doUnbindService( this );
		
		if ( isStoped )
		{
			Log.d(TAG,"StopService");
			doStopService();
		}
    	super.onDestroy();
    }
	
    
    
    ////////////////////////////////////////
    // �T�[�r�X�֘A
	private PCMRender PCMBound = null;	
    
    // �T�[�r�X�̊J�n
    private void doStartService()
    {
        if ( ! isStartService )
        {
    		Log.d(TAG, "StartService");

        	startService(new Intent(Player.this , PCMRender.class ) );
        	isStartService = true;
        }
    }
    // �T�[�r�X�̏I��
    private void doStopService()
    {
    	if ( isStartService )
        {
    		Log.d(TAG, "stopService");

        	stopService(new Intent(Player.this , PCMRender.class ) );
        	isStartService = false;
        }
    }


    //////////////////////////////////////
    // ��ʊ֘A
    private Timer DispTimer = null;
       
    private void doScheduleUpdateTimer()
    {
    	TimerTask uiTask = new TimerTask() 
    	{
    		@Override
    		public void run()
    		{
        		ui_handler.post(new Runnable()
        		{
        			@Override
        			public void run() 
        			{
        				updateInfo();
        			}
        		});
    		}
    	};
    	
		Log.d(TAG,"Scheduling timer..");
    	DispTimer = new Timer(true);
    	DispTimer.scheduleAtFixedRate(uiTask,0,200);    	
    }
    
    // �^�C�}�[�̃L�����Z��
    private void doCancelTimer()
    {
		Log.d(TAG,"Cancelled timer..");
    	if ( DispTimer == null )
    		return;

    	DispTimer.cancel();
    	DispTimer = null;
    	
    	Log.d(TAG,"Cancelled");
    }
    
    // ��ʕ\��
    private void updateTime()
    {
    	int len = PCMBound.getLen();
    	int pos = PCMBound.getPos();
    	boolean loop = PCMBound.getLoop();
    	
        seek_view.setMax( len );
        seek_view.setProgress( pos );
        
        String time;
        
        if ( loop )
        {
        	time = String.format( "%02d:%02d / --:--" , 
    	   			pos / 60 , pos % 60 );
        }
        else
        {
        	time = String.format( "%02d:%02d / %02d:%02d" , 
	   			pos / 60 , pos % 60 ,
	   			len / 60 , len % 60 );
        }
        
	   	time_view.setText( time );
    }
    
    // ���X�V
	private void updateInfo()
	{
		if ( !PCMBound.isUpdate() )
			return;
		
		updateTime();
	   	
        title_view.setText( PCMBound.getTitle() );
        file_view.setText( fobj.path );
        
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append("Volume:").append(PCMBound.getVolume()).append(" ");
        sb.append("PCM:").append(PCMBound.getPCMRate()).append("Hz ");

    	vol_view.setText(sb); 
	}
    
	/////////////////////////////
    // Activity�J��
    @Override
    public void onPause()
    {
    	Log.d(TAG,"onPause");
    	// ���݂̐ݒ��ۑ�����
		SharedPreferences.Editor edit = pref.edit();
		edit.putString( KEY_LASTPATH, fobj.current_path );
		edit.putString( KEY_LASTFILE, fobj.path );
		edit.putInt( KEY_VOL , PCMBound.getVolume() );
		edit.commit();
		pcmService.doUnbindService( this );
		doCancelTimer();	    
		
		super.onPause();
    }
    
    @Override
    public void onResume()
    {
    	Log.d(TAG,"onResume");
 
        // �T�[�r�X�ւ̐ڑ�
        pcmService.doBindService( this , new Intent(Player.this , PCMRender.class ) , this );

    	super.onResume();
    }
    @Override
    public void onStart()
    {
    	Log.d(TAG,"onStart");
    	super.onStart();
    }
    
    @Override
    public void onStop()
    {
    	Log.d(TAG,"onStop");
    	super.onStop();
    }
    
    // �ȑI�����J��
    private void openSongSelector()
    {
    	PCMBound.doSetPause(true);
		Intent intent = new Intent(Player.this , FileDiag.class );
		startActivityForResult(intent,0);
    }
    
    // �Đ��{�^���̕ύX
    private void setPauseButton( boolean pause_flag )
    {
		if ( pause_flag )
			((ImageButton)findViewById(R.id.play_btn)).setImageResource( R.drawable.pause );
		else
			((ImageButton)findViewById(R.id.play_btn)).setImageResource( R.drawable.play );	
    }
    
    // �w���v��ʂ̕\��
    public void startHelpActivity()
    {
		Intent intent = new Intent(Player.this , HelpActivity.class );
    	startActivityForResult(intent,0);
    }

    // �ݒ��ʂ̕\��
    public void startPrefActivity()
    {
		Intent intent = new Intent(Player.this , Setting.class );
    	startActivityForResult(intent,0);
    }
    
    // �L�[��ʂ̕\��
    public void startKeyViewActivity()
    {
		Intent intent = new Intent(Player.this , KeyView.class );
    	startActivityForResult(intent,0);
    }

    // �{�^���N���b�N���o
	@Override
	public void onClick(View v)
	{
		switch(v.getId())
		{
		case R.id.play_btn:
			isStoped = false;
			if (PCMBound.isLoaded())
			{
				PCMBound.doPause();				
				setPauseButton( ! PCMBound.getPause() );
			}
			else
			{
				PCMBound.doPlaySong();
				setPauseButton( true );			
			}
			PCMBound.doUpdate();
		break;
		case R.id.stop_btn:
			isStoped = true;
			setPauseButton( false );
			PCMBound.doStop();
		break;			
		case R.id.voldown_btn:
			PCMBound.doVolumeDown();
			PCMBound.doUpdate();
		break;
		case R.id.volup_btn:
			PCMBound.doVolumeUp();
			PCMBound.doUpdate();
		break;			
		case R.id.rev_btn:
			PCMBound.doPlayPrevSong();
			setPauseButton( true );
			updateInfo();
			break;
		case R.id.ff_btn:
			PCMBound.doPlayNextSong();
			setPauseButton( true );
			PCMBound.doUpdate();
			break;
		case R.id.time_value:
			PCMBound.doSetLoop();
			updateInfo();
			break;
		case R.id.title_value:
			openSongSelector();
			break;
		}
	}

	// Activity����̋A�҂Ə���
	public void onActivityResult(int reqCode,int result,Intent intent)
	{
		// �t�@�C���_�C�A���O����
		if (reqCode == 0)
		{
			if ( result == RESULT_OK )	
			{					
				SharedPreferences.Editor edit = pref.edit();
				edit.putString( KEY_LASTFILE, fobj.path );
				edit.commit();
				
				PCMBound.setFobj( fobj );
				PCMBound.doPlaySong();
				setPauseButton( true );
			}
			else
			{
				PCMBound.doSetPause(false);
			}
		}
	}
}