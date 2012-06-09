package com.bkc.android.mdxplayer;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import com.bkc.android.mdxplayer.R;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.SeekBar.OnSeekBarChangeListener;
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

	String TAG = "MDXPlayer";
	String MDXPlayerTitle;

	static String KEY_PCMPATH  = "PCMPath";   
	static String KEY_LASTFILE = "lastFile";
	static String KEY_LASTPATH = "lastPath";
	static String KEY_VOLUME = "volume";

	final Handler ui_handler = new Handler();
	
	private FileListObject fobj = null;
	private PCMService pcmService = new PCMService();
	private StringBuilder infoSB = new StringBuilder();
	
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
        
		Log.d(TAG,"onCreate");

        // �T�[�r�X�ւ̐ڑ�
        doStartService();
        pcmService.doBindService( this , new Intent(Player.this , PCMRender.class ) , this );
        
        fobj = FileListObject.getInst();
        fobj.setContext( getApplicationContext() );
        
        // �r���[�̎擾�Ɛݒ�
        
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
        ((ImageButton)findViewById(R.id.stop_btn)).setOnClickListener(this);

        loadPref();
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
			case R.id.menuItem04:
				openSongSelector();
			return true;
			case R.id.menuItem05:
				// share
				startShare();
			return true;
			case R.id.menuItem06:
				// volume
				startVolumeDialog();
			return true;
		}
		return false;
	}
	
	// ���L����
	public void startShare()
	{
		Intent intent = new Intent ( android.content.Intent.ACTION_SEND );
		intent.setType("text/plain");
		
		String content;
		
		// �Đ���
		if (PCMBound != null && PCMBound.isPlay())
		{
			content = String.format("Now Playing: %s #mdxplayer",PCMBound.getTitle() );
		}
		else
		{
			content = String.format("Starting Up: %s #mdxplayer",MDXPlayerTitle );
		}
		
		intent.putExtra( Intent.EXTRA_TEXT, content );
		startActivity ( 
				Intent.createChooser ( 
						intent , getString ( R.string.share_string ) 
					) 
		);

	}
	
	// �_�C�A���O�\��
	public void startVolumeDialog()
	{
		Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.voldiag);
		dialog.setTitle(getString(R.string.vol_string));
		
		SeekBar  volSeek = (SeekBar) dialog.findViewById(R.id.volseek1);
		
		volSeek.setMax( 100 );
		
		if ( PCMBound != null )
			volSeek.setProgress( PCMBound.getVolume() );
		else
			volSeek.setProgress( 100 );
		
		volSeek.setOnSeekBarChangeListener(
				new OnSeekBarChangeListener()
				{
		            public void onStopTrackingTouch(SeekBar seekbar) {
		            }
		 
		            public void onStartTrackingTouch(SeekBar seekbar) {
		            }
		 
		            public void onProgressChanged(SeekBar seekbar,
		                    int vol, boolean flag) 
		            {
		            	PCMBound.setVolume(vol);
		            }
				}
		);
		
		dialog.setCanceledOnTouchOutside( true );
		
	
		// �_�C�A���O�ő剻
		dialog.getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		dialog.show();

	}

	// �ڑ��ʒm
	public void notifyConnected( PCMRender pcm )
    {
    	PCMBound = pcm;
    	
		Log.d(TAG,"notifyConnected");
		
		Intent MyIntent = new Intent( this,Player.class );
		
		PendingIntent cbIntent = 
			PendingIntent.getActivity(
					this , 
					0 ,
					MyIntent ,
					0 );		
		
		PCMBound.setCallbackIntent( cbIntent );
		
        if (PCMBound.isPlayed())
        {
        	fobj = PCMBound.getFobj();
        }
        else
        {
        	PCMBound.setFobj( fobj );
        	loadPref();
        	PCMBound.setTitle( MDXPlayerTitle );
        }

		if (Intent.ACTION_VIEW.equals(getIntent().getAction()))
		{
	        if ( fobj != null )
	        {
				PCMBound.doStop();

	        	File file = new File(getIntent().getData().getPath());
	            fobj.openDirectory( file.getParent() );
	            fobj.setCurrentFilePath( file.getPath() );
				PCMBound.doPlaySong();
	        }
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
    	boolean isStop = false;

    	Log.d(TAG,"onDestroy");	
		
		if ( PCMBound != null )
		{
			// ��~��?
			isStop = PCMBound.getPause();

			// �Đ����Ă��Ȃ�
			if ( ! PCMBound.isPlay() ) 
				isStop = true;
		}
		pcmService.doUnbindService( this );
		
		if ( isStop )
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
    	        
        if ( loop )
        {
        	infoSB.setLength(0);
        	infoSB.append(String.format("%02d:%02d", pos/60,pos%60));
        	infoSB.append(" / ");
        	infoSB.append("--:--");
        }
        else
        {
        	infoSB.setLength(0);
        	infoSB.append(String.format("%02d:%02d", pos/60,pos%60));
        	infoSB.append(" / ");
        	infoSB.append(String.format("%02d:%02d", len/60,len%60));
        }
        
	   	time_view.setText( infoSB );
	   	
        seek_view.setMax( len );
        seek_view.setProgress( pos );
    }
    
    // ���X�V
	private void updateInfo()
	{
		
		if ( !PCMBound.isUpdate() )
			return;
		
		updateTime();
		
		if ( PCMBound.isPlay() && !PCMBound.getPause() )
			((ImageButton)findViewById(R.id.play_btn)).setImageResource( R.drawable.pause );
		else
			((ImageButton)findViewById(R.id.play_btn)).setImageResource( R.drawable.play );
			   	
        title_view.setText( PCMBound.getTitle() );
        file_view.setText( fobj.path );
        
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append("Volume:").append(PCMBound.getVolume()).append(" ");
        sb.append("PCM:").append(PCMBound.getPCMRate()).append("Hz ");

    	vol_view.setText(sb); 
	}
	
	// ���݂̐ݒ��ǂݏo��
	public void loadPref()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref == null)
        	return;
        
        if ( fobj != null )
        {
            fobj.openDirectory( pref.getString(KEY_LASTPATH, "") );
            fobj.setCurrentFilePath( pref.getString(KEY_LASTFILE, "") );
        }
        if ( PCMBound != null )
        {
        	PCMBound.setVolume( pref.getInt( KEY_VOLUME , 100) );
        }
	}


	// ���݂̐ݒ��ۑ�����
	public void savePref()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref == null)
        	return;
        
        SharedPreferences.Editor edit = pref.edit();
        
        if (edit == null)
        	return;
        
        if ( fobj != null )
        {
        	edit.putString( KEY_LASTPATH, fobj.current_path );
        	edit.putString( KEY_LASTFILE, fobj.path );
        }
        if ( PCMBound != null )
        {
        	edit.putInt( KEY_VOLUME , PCMBound.getVolume() );
        }
    	edit.commit();
	}
    
	/////////////////////////////
    // Activity�J��
    @Override
    public void onPause()
    {
    	Log.d(TAG,"onPause");
    	// ���݂̐ݒ��ۑ�����
    	savePref();
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
			if (PCMBound.isLoaded())
			{
				PCMBound.doPause();				
			}
			else
			{
				PCMBound.doPlaySong();
			}
			PCMBound.doUpdate();
		break;
		case R.id.stop_btn:
			PCMBound.doStop();
		break;
		case R.id.rev_btn:
			PCMBound.doPlayPrevSong();
			updateInfo();
			break;
		case R.id.ff_btn:
			PCMBound.doPlayNextSong();
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
				savePref();
				
				PCMBound.setFobj( fobj );
				PCMBound.doPlaySong();
			}
			else
			{
				PCMBound.doSetPause(false);
			}
		}
	}
}