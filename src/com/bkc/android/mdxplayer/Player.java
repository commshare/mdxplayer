package com.bkc.android.mdxplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
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
	static TextView time_view;
	static TextView title_view;
	static TextView file_view;
	static TextView vol_view;
	static SeekBar  seek_view;

	private String app_title;
	private String app_version;

	private boolean isStartService = false;

	private final static String TAG = "Player";
	private static final int REQ_FILE = 0;

	final Handler ui_handler = new Handler();
	
	private long log_date = 0;
	static boolean pauseDiag = false;

	
	private FileListObject fobj = null;
	private PCMService pcmService = new PCMService();
	private StringBuilder infoSB = new StringBuilder();
	
	private EXUncaughtExceptionHandler exHandler;
	
	// ������
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        Resources res = getResources();
        
        // �^�C�g���쐬
        app_title = String.format("%s %s",res.getString(R.string.app_name),res.getString(R.string.app_version));
        
        exHandler = new EXUncaughtExceptionHandler(this);
        
        // ��O�n���h���̐ݒ�        
		Thread.setDefaultUncaughtExceptionHandler(exHandler);

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
        ((TextView)findViewById(R.id.filename_value)).setOnClickListener(this);

        time_view.setOnClickListener(this);    
  
        ((ImageButton)findViewById(R.id.play_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.rev_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.ff_btn)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.stop_btn)).setOnClickListener(this);

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
			case R.id.menu_showInfo:
	    		dispInfoDiag();
	    	break;

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
			content = String.format("Starting Up: %s #mdxplayer",app_title );
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
		dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		dialog.show();

	}
	
    void doCheckIntent()
    {
    	if (PCMBound == null)
    		return;
    	
		String action = getIntent().getAction();
		
		if (action == null)
			return;
		
    	Log.d(TAG,"action:" + action);

    	Uri uri = getIntent().getData();

		if (Intent.ACTION_VIEW.equals(action) && uri != null)
		{
			String URI = uri.toString();
			
			Log.d(TAG,"scheme:" + uri.getScheme() + " string:" + URI);
					
			if (fobj != null)
			{
				Log.d(TAG,"doIntent:");
				getIntent().setAction("");
				
				doPlayFile(getIntent().getData().getPath());
			}
		}
    }
	
    @Override
    protected void onNewIntent(Intent intent) 
    {
    	Log.d(TAG,"onNewIntent");
    	
    	super.onNewIntent(intent);
    	setIntent(intent);
    	doCheckIntent();
    }

	// �t�@�C���I�u�W�F�N�g�̐ݒ�
	private void setFileObject()
	{
        fobj = PCMBound.getFobj();
        
        // �t�@�C���I�u�W�F�N�g��V���ɍ쐬
        if (fobj == null)
        {
            fobj = FileListObject.getInst();
            fobj.setContext(getApplicationContext());
        	PCMBound.setFobj(fobj);
        	PCMBound.setTitle(app_title);
        }
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
		
		setFileObject();
		doCheckIntent();
		
        doStartUIThread();

    	PCMBound.doUpdate();

    	checkFileDiag();

    	loadPref();

    }
    
	private void checkFileDiag() {
		if (returnFileDiag)
		{
			returnFileDiag = false;
			
			if (isSongRequest)
			{
				isSongRequest = false;
				loadRequestSong();
			}
			
			if (pauseDiag)
				PCMBound.doSetPause(false);
		}		
	}

	private void loadRequestSong() 
	{
		doPlayFile(fobj.current_filepath);		
	}

	private void doPlayFile(String path) 
	{
		PCMBound.doStop();

    	File file = new File(path);
        fobj.openDirectory(file.getParent());
        fobj.setCurrentFilePath(file.getPath());
        
        PCMBound.setFobj(fobj);
        
		PCMBound.doPlaySong();
		
		PCMBound.doUpdate();
	}

	// �ؒf�ʒm(��{�I�ɌĂ΂�Ȃ�)
    public void notifyDisconnected()
    {
    	PCMBound = null;
		Log.d(TAG,"notifyDisconnected");
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
	static private PCMRender PCMBound = null;	
    
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
    private boolean uiLoop = false;
    private Thread uiThread = null;
    
    private boolean isSongRequest = false;
	private boolean returnFileDiag = false;
	private long uiID = 0;
       
    private void doStartUIThread()
    {
    	if (uiLoop)
    		return;
    	
    	uiLoop = true;
    	uiThread = new Thread() 
    	{
    		@Override
    		public void run()
    		{
				while (uiLoop) {
					ui_handler.post(new Runnable() {
						@Override
						public void run() {
							updateInfo();
						}
					});
					try {
						Thread.sleep(25);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
    			}
    		}
    	};
    	
    	uiID = uiThread.getId();
		Log.d(TAG,"start UI.. id:" + uiID);
		uiThread.start();
    }
    
    // �^�C�}�[�̃L�����Z��
    private void doStopUIThread()
    {
		Log.d(TAG,"stop UI.. id:" + uiID);
    	if (uiThread == null)
    		return;
    	
    	uiLoop = false;
    	while(uiThread.isAlive());
    	
    	uiThread = null;
    	
    	Log.d(TAG,"stopped UI id:" + uiID);
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
		if (PCMBound == null)
			return;
		
		updateTime();

		// ���ԏ��ȊO
		if ( !PCMBound.isUpdate() )
			return;
		
		
		if ( PCMBound.isPlay() && !PCMBound.getPause() )
			((ImageButton)findViewById(R.id.play_btn)).setImageResource(R.drawable.pause
);
		else
			((ImageButton)findViewById(R.id.play_btn)).setImageResource(R.drawable.play);
			   	
        title_view.setText( PCMBound.getTitle() );
        file_view.setText( fobj.current_filepath );
        
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append("Volume:").append(PCMBound.getVolume()).append(" ");
        sb.append("PCM:").append(PCMBound.getPCMRate()).append("Hz ");

    	vol_view.setText(sb); 
	}

	// ���݂̃A�v���P�[�V�����ݒ��ǂݏo��
	public void loadAppPref()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref == null)
        	return;
        
        app_version = pref.getString(Setting.APPVER, "");
        log_date = pref.getLong(Setting.LOGDATE, 0);
	}
	
	// ���݂̃A�v���P�[�V�����ݒ�������o��
	public void saveAppPref()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref == null)
        	return;
        SharedPreferences.Editor edit = pref.edit();
        if (edit == null)
        	return;

        edit.putString(Setting.APPVER,app_version);
        edit.putLong(Setting.LOGDATE,log_date);
        
        edit.commit();
	}
	
	// ���݂̐ݒ��ǂݏo��
	public void loadPref()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref == null)
        	return;
        
        pauseDiag = pref.getBoolean(Setting.PAUSE_DIAG, false);
        
        if (PCMBound == null)
        	return;
        
        if (fobj != null)
        {
        	// �V�K�I�u�W�F�N�g�ł������ꍇ�͐ݒ肷��
        	if (!PCMBound.isPlayed())
            {
        		fobj.openDirectory(pref.getString(Setting.LASTPATH, ""));
                fobj.setCurrentFilePath(pref.getString(Setting.LASTFILE, ""));
            }
        }
        
        PCMBound.setVolume(pref.getInt(Setting.VOLUME, 100));
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
        
        if (PCMBound == null)
        	return;
        
        if (fobj != null && PCMBound.isPlayed())
        {
        	edit.putString(Setting.LASTPATH, fobj.current_dir);
        	edit.putString(Setting.LASTFILE, fobj.current_filepath);
        }
        
        edit.putInt(Setting.VOLUME, PCMBound.getVolume());
    	edit.commit();
	}
	
    private void dispInfoDiag()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        StringBuilder sb = new StringBuilder();
        sb.setLength(0);
        sb.append(getString(R.string.app_name)).append(" ")
        .append(getString(R.string.app_version)).append("\n\n");
        
        try {
            Resources res = getResources();
            InputStream text = res.openRawResource(R.raw.changelog);
            
            byte[] data = new byte[text.available()];
            text.read(data);
            text.close();
            
            sb.append(new String(data));
            
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        builder.setMessage(sb)
        .setCancelable(false)
        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                           {
            public void onClick(DialogInterface dialog,int id)
            {
                dialog.cancel();
            }
        });
        
        AlertDialog alert = builder.create();
        alert.show();
    }

    
	/////////////////////////////
    // Activity�J��
    @Override
    public void onPause()
    {
    	Log.d(TAG,"onPause");
    	// ���݂̐ݒ��ۑ�����
    	savePref();
        pcmService.doUnbindService(this);
        PCMBound = null;
		doStopUIThread();
		
		super.onPause();
    	saveAppPref();
    }
    
    @Override
    public void onResume()
    {
    	Log.d(TAG,"onResume");
        // �T�[�r�X�ւ̐ڑ�
        pcmService.doBindService( this , new Intent(Player.this , PCMRender.class ) , this );
    	super.onResume();
    	
    	// �G���[���O�̕\���ʒm�ƍŐV�o�[�W�����̕\��
    	loadAppPref();
    	
    	long lastLogMod = exHandler.getLastModLog();
    	
    	// �G���[���O���X�V����Ă���
    	if (lastLogMod != 0 && log_date != lastLogMod)
    	{
    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
     		
    		builder.setMessage(R.string.log_found)
    		.setCancelable(false)
    		.setPositiveButton("OK", new DialogInterface.OnClickListener()
    		{
    				public void onClick(DialogInterface dialog,int id)
    				{
    					startHelpActivity();
    				}
    		})
    		.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
    		{
    				public void onClick(DialogInterface dialog,int id)
    				{
    					dialog.cancel();
    				}
    		});
    		
    		AlertDialog alert = builder.create();
    		alert.show();
    		
    		log_date = lastLogMod;
    	}
    	
    	// �o�[�W������񂪍X�V����Ă���
    	String app_current_ver = getString(R.string.app_version);
    	
    	if (!app_current_ver.equals(app_version))
    	{
    		dispInfoDiag();
    		app_version = app_current_ver; 		
    	}
    	
    	saveAppPref();
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
    	if (pauseDiag) PCMBound.doSetPause(true);
		Intent intent = new Intent(Player.this, FileDiag.class);
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
			PCMBound.doUpdate();
			break;
		case R.id.ff_btn:
			PCMBound.doPlayNextSong();
			PCMBound.doUpdate();
			break;
		case R.id.time_value:
			PCMBound.doSetLoop();
			PCMBound.doUpdate();
			break;
		case R.id.filename_value:
		case R.id.title_value:
			openSongSelector();
			break;
		}
	}

	// Activity����̋A�҂Ə���
	public void onActivityResult(int reqCode,int result,Intent intent)
	{
		// �t�@�C���_�C�A���O����
		if (reqCode == REQ_FILE)
		{
			returnFileDiag = true;
			if (result == RESULT_OK)
				isSongRequest = true;
		}
	}
}