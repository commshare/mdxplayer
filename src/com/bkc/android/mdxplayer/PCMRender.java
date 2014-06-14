package com.bkc.android.mdxplayer;

//
// to make a header,run the following command in console
// javah -classpath bin/classes -d jni com.bkc.android.mdxplayer.PCMRender  -J-Dfile.encoding=UTF8
//
// Note : alternative way
// 2>&1 | iconv -f sjis
//

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class PCMRender extends Service
{

	private native void    sdrv_setrate(int rate);
	private native void    sdrv_setpcmdir(String pcmdir);
	
	private native boolean sdrv_open(String path);
	private native void    sdrv_close();
	private native void    sdrv_render(short[] data,int samples);
	private native byte[]  sdrv_title();
	private native int     sdrv_length();

	private native void    sdrv_dofade(int sec);
	private native boolean sdrv_isfaded();
	private native int     sdrv_num_tracks();
	private native int     sdrv_get_note(int[] data,int len);
	
	
	private final static String TAG = "PCMRender";

	FileListObject fobj = null;
	
	static 
	{
		System.loadLibrary("mdxmini");
	}
	
    private PCMThread c_runner;
    
	private boolean isPlaying = false;
	private boolean isPausing = false;
	private boolean isLoadFile = false;
	private boolean isLoadedFile = false;
	
	static private boolean isUpdating = false;
	
	private boolean isPlayedOnce = false;
	private boolean isStopping = false;

	
	private int     song_count = 0;
	private String  song_title;
	private int     song_len = 0;
	private int     song_pos = 0;
	private long    song_framepos = 0;
	private int     song_vol = 100;
	private float   song_volstep = 0;
	private boolean song_loop_inf = false;
	private String  song_pcmpath = "";

	private int pcm_rate = 0;
	
	private NotificationManager notifMan;
	private Notification notif;
	private PendingIntent cbIntent;
	
	Handler handler = null;
	
	// �J�n����
	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(TAG, "onCreate");
		handler = new Handler();
		notifMan = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
	
		startPCMdriver();
	}

    // �I������
	@Override
    public void onDestroy()
    {
		Log.d(TAG,"onDestroy");

		closeFile();
		
        c_runner.stop_flag = true;        
    	super.onDestroy();
    }
	
	/////////////////////////////
	// �T�[�r�X�p

	public class LocalBinder extends Binder {
        PCMRender getService() {
            return PCMRender.this;
        }
    }
	
    private final IBinder mBinder = new LocalBinder();


	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}
	///////////////////////////////
	// �C���^�[�t�F�[�X
	
    public FileListObject getFobj() {
		return fobj;
	}

	public void setFobj(FileListObject fobj) {
		this.fobj = fobj;
	}
	
	// �ʒm�\��
	public void setNotif(String msg)
	{
		notif = new Notification(
				R.drawable.icon,
				msg,
				System.currentTimeMillis()
		);

		notif.setLatestEventInfo(
				getApplicationContext(),
				getString(R.string.app_name),
				msg,
				cbIntent );
		
		notif.flags = Notification.FLAG_ONGOING_EVENT;

		notifMan.notify( R.string.app_name , notif );		
	}
	
	
	// �Ăяo���C���e���g�̐ݒ�
	public void setCallbackIntent(PendingIntent intent)
	{
		cbIntent = intent;
	}
	
    // �t�@�C�������
    private void closeFile()
    {
    	if ( isPlaying )
    	{
        	Log.d(TAG, "closeFile");
    		isPlaying = false;
    		sdrv_close();
    	}
    }


	// �Ȃ̍Đ����J�n����
    public void doPlaySong()
    {
    	Log.d(TAG,"doPlaySong");
    	
    	song_title = "Loading...";
    	
    	isUpdating = true;

    	isPausing = true;
    	isLoadFile = true;
    	isPlayedOnce = true;
    	isLoadedFile = false;
    	isStopping = false;

    }
    
    // �Ȃ��~
    public void doStop()
    {
    	Log.d(TAG,"doStop");
    	// �ʒm�̃L�����Z��
    	notifMan.cancel( R.string.app_name );
    	
    	
    	isUpdating = true;
    	isPausing = true;
    	song_pos = 0;

    	isStopping = true;
    	isLoadedFile = false;
    }
    
    // ���̋Ȃ��Đ�
    public void doPlayNextSong()
    {
		isPausing = true;
		fobj.getNextSong(1);
		doPlaySong();
    }
    
    // �O�̋Ȃ��Đ�
    public void doPlayPrevSong()
    {
		isPausing = true;
		fobj.getNextSong(-1);
		doPlaySong();
    }
    
    // �|�[�Y�̐ݒ�
    public void doSetPause( boolean flag )
    {
    	isPausing = flag;
    }

    // �|�[�Y�̐ݒ�
    public void doPause()
    {
    	isPausing = !isPausing;
    }
    
    // �|�[�Y�̊m�F
    public boolean getPause()
    {
    	return isPausing;
    }
       
    // �{�����[����������
    public void doVolumeDown()
    {
		if (song_vol - 10 > 0)
			song_vol -= 10;
		else
			song_vol = 0;
    }

    // �{�����[�����グ��
    public void doVolumeUp()
    {
    	if (song_vol + 10 < 100)
			song_vol += 10;
		else
			song_vol = 100;
    }
    
    // ���݂̃{�����[���𓾂�
    public int getVolume()
    {
    	return song_vol;
    }
    // �{�����[�����Z�b�g����
    public void setVolume(int vol)
    {
    	song_vol = vol;
    }
    // ���[�v�ݒ�
    public void doSetLoop()
    {
    	song_loop_inf = !song_loop_inf;
    	isUpdating = true;
    }
    // ���[�v�ݒ�̊m�F
    public boolean getLoop()
    {
    	return song_loop_inf;
    }
    
    // �Ȃ̒������擾(�b�P��)
    public int getLen()
    {
    	return song_len;
    }
    
    // �Ȃ̈ʒu���擾(�b�P��)
    public int getPos()
    {
    	return song_pos;
    }
    
    // ���݂܂ł̍Đ��Ȑ�
    public int getSongCount()
    {
    	return song_count;
    }

    // �Ȗ�
    public String getTitle()
    {
    	return song_title;
    }
    
    // ���݂̃g���b�N��
    public int getTracks()
    {
    	return sdrv_num_tracks();
    }

    // �Đ����g��
    public int getPCMRate()
    {
    	return pcm_rate;
    }
    
    // ���K�f�[�^�o�b�t�@
    Queue<Long> NoteTimeStack = new LinkedList<Long> ();
    Queue<Integer[]> NoteDataStack = new LinkedList<Integer[]> ();

    long currentNoteTime = 0;
    Integer[] currentNoteData = null;

    long nextNoteTime = 0;
    Integer[] nextNoteData = null;

    // ���݂̉��K�f�[�^�𓾂�
    public void getCurrentNotes(int[] data,int len)
    {
    	// ���̉��K��ǂݏo��
    	while ( song_framepos >= nextNoteTime )
    	{
    		if ( NoteTimeStack.size() == 0 )
    			break;
    		
    		currentNoteTime = nextNoteTime;
    		currentNoteData = nextNoteData;
    		
    		nextNoteTime = NoteTimeStack.poll();
    		nextNoteData = NoteDataStack.poll();
    	}
    	
    	if ( currentNoteData == null )
    		return;
    	
    	for ( int i = 0; i < len; i++ )
    	{
    		if ( currentNoteData[i] != null )
    			data[i] = currentNoteData[i];    	
    	}
    }    
    
    // �Ȗ��̐ݒ�
    public void setTitle(String title)
    {
    	song_title = title;
    	isUpdating = true;
    }
    
    // �X�V�̊m�F
    public boolean isUpdate()
    {
    	if (isUpdating)
    	{
    		isUpdating = false;
    		return true;
    	}
    	return false;
    }
    
    // �A�b�v�f�[�g����
    public void doUpdate()
    {
    	isUpdating = true;
    }
    
    // �Đ����Ă��邩
    public boolean isPlay()
    {
    	return isPlaying;
    }
    
    // ��x�Đ�������
    public boolean isPlayed()
    {
    	return isPlayedOnce;
    }
    
    // �t�@�C���͓ǂݏo���ꂽ��
    public boolean isLoaded()
    {
    	return isLoadedFile;
    }

    // PCM�Đ��X���b�h�̍쐬
    private void startPCMdriver()
    {
    	Log.d(TAG,"startPCMdriver");
    	sdrv_setpcmdir("");
    	
    	// �I�[�f�B�I�X���b�h
        c_runner = new PCMThread() 
        {
        	//////////////////////////////
            // �I�[�f�B�I�n�[�h�E�F�A�֘A
            AudioTrack at = null;
            private int    atBufSize = 0;
            private int    atVol = 0;
            private int    atBufPos = 0;
            private int    atMinBuf = 0;
            
            private int    atRate = 0; // ���[�g
            private int    atBufBlocks = 0; // �u���b�N��
            private int    atUpdateFrame = 0; 
            
            // �Ȃ̓��̈ʒu(�P��:�t���[��)
            private long   atSongHeadFrame = 0;
            // �o�b�t�@�̏������݈ʒu(�P��:�t���[��)
            private long   atWriteFrame    = 0;
            
            
            short atPCM[] = null;
            private boolean atPlay = false;
            private boolean atUpdateConfig = true;
              
            // �I�[�f�B�I������
            private void audioInit()
            {
            	int rate = atRate;
            	
            	int ch_bit = AudioFormat.ENCODING_PCM_16BIT;
            	int ch_out = AudioFormat.CHANNEL_OUT_STEREO;
            	// int ch_out = AudioFormat.CHANNEL_OUT_STEREO; later 2.0
            	
            	if (at != null)
            		return;
            	
            	atMinBuf = AudioTrack.getMinBufferSize( rate, ch_out , ch_bit );
            	atBufSize = atMinBuf * atBufBlocks;
                
                at = new AudioTrack( 
                		AudioManager.STREAM_MUSIC , rate , 
                		ch_out , ch_bit ,  // 
                		atBufSize , AudioTrack.MODE_STREAM );
                
                if (at == null)
                	return;
                                
                song_volstep = AudioTrack.getMaxVolume() - AudioTrack.getMinVolume();
                song_volstep /= 100;

                song_len = 0;
                song_pos = 0;

                pcm_rate = rate;

                // rate / bufsize = 21.53
                // 46.44
                atRate = rate;
                atUpdateFrame = rate / 4;
                atBufPos = 0;
                atPlay = false;
                atUpdateConfig = false;
                atWriteFrame = 0;

                atPCM = new short[ atBufSize * 2];
            }
            
            // �I�[�f�B�I���ʐݒ�
            private void audioSetVolume( int volume )
            {
            	if (at == null || atVol == volume )
            		return;

            	float vol = song_volstep * song_vol;
            	
            	at.setStereoVolume(vol,vol);
            	isUpdating = true;
            	atVol = volume;
            }
            
            // �I�[�f�B�I�I������
            private void audioFree()
            {
            	if (at == null)
            		return;
            	
            	// AudioTrack�̔j���͕�����̎��s�Ń����������肸�ɃN���b�V������ꍇ������
            	atVol = 0;
            	
            	at.setStereoVolume( 0.0f , 0.0f );
            	at.flush();
            	at.stop();
        		try {            	
        			while( at.getPlayState() == AudioTrack.PLAYSTATE_PLAYING )
        			{
        				Thread.sleep(10);
        			}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

            	at.release();
            	
            	at = null;
            }
            
            // �I�[�f�B�I�\���ݒ�
            private void audioSetConfig()
            {
            	int freq,buf;
            	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PCMRender.this);
            	
            	// �f�t�H���g�ݒ�
            	buf  = Integer.parseInt(sp.getString("buf_key", getString(R.string.def_buf)));
            	freq = Integer.parseInt(sp.getString("freq_key", getString(R.string.def_freq)));
            	            	
            	if (atRate != freq || atBufBlocks != buf )
            	{
            		atUpdateConfig = true;
            	}
            	atRate = freq;
            	atBufBlocks = buf;
            }
            
            private int audioCurrentPos()
            {
            	if (at == null)
            		return 0;
            	
            	return at.getPlaybackHeadPosition();
            }
        	
            
            //�@�t�@�C����ǂݍ���
            private void loadFile()
            {
            	// �Đ����Ȃ��U����

            	closeFile();
            	
            	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PCMRender.this);
            	
            	if (sp != null)
            	{
            		song_pcmpath = sp.getString( Setting.PCMPATH, "" );
            		sdrv_setpcmdir( song_pcmpath );
            	}

            	// �t�@�C���`�F�b�N
            	File file = new File( fobj.current_filepath );

            	// ���݂��Ȃ� or �J���Ȃ�
            	if ( !file.exists() || sdrv_open ( fobj.current_filepath ) )            	
            	{
            		handler.post(new Runnable()
            		{
            			@Override
            			public void run() 
            			{
            				String msg = getString(R.string.openerror, fobj.current_filepath);
            				Toast.makeText(PCMRender.this, msg, Toast.LENGTH_LONG ).show();
            			}
            		});
            		isLoadFile = false;
            		return;
            	}
            	
            	// �^�C�g�����Ȃ̎��Ԃ��擾
            	song_title = getMDXTitle();
            	song_pos = 0;
            	song_len = sdrv_length();

            	// song_len = 10; // for debug
            	
            	// Log.d(TAG, song_title + " length = " + song_title.length());
            	if (song_title.length() == 0)
            		song_title = file.getName();
            	
            	// �ʒm
            	setNotif(song_title);
            	
            	fobj.setCurrentSongTitle(song_title);
            	fobj.setCurrentSongLen(song_len);
            
            	isUpdating = true;
                isPlaying = true;
                isPausing = false;
                isLoadFile = false;
                isLoadedFile = true;
                
                song_count ++;
                
                Log.d(TAG,"start play " + song_count + " id:" + c_runner.getId());
                Log.d(TAG,"id:" + this.hashCode() + " playing:" + isPlaying);
            }

        	// �^�C�g���擾
            private String getMDXTitle()
            {
        		String title = "";
        		try {
        			// note : sdrv_title returned without null termination.
        			// String makes "len = 1" string if it has null term.
        			
        			title = new String(sdrv_title(),"SJIS");
        		} catch (UnsupportedEncodingException e)
        		{
        		}
        		return title;
            }

			@Override
        	public void run()
        	{
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

				// audioSetConfig();
				// audioInit();
				
                int outframes = 0;
                // �T�C�Y���傫����UI�t���[�Y�o�O����������
                int atPackSize = 2048;
                
                // long lastTime = SystemClock.uptimeMillis();
                // long diffTime = 0;
                
                long oldPos = 0;                
        		int[] notes = new int[64];

        		Log.d(TAG, "run id:" + c_runner.getId());
        		Log.d(TAG, "id:" + this.hashCode());
                
                stop_flag = false;
                                
                while( ! stop_flag )
                {
                	// ���݂̉��ʂ��Z�b�g����
                    audioSetVolume( song_vol );

                	if ( isLoadFile )
                	{
                        Log.d(TAG,"loadFile id:" + c_runner.getId());

                		// �t�@�C���̍Đ�����
        				audioSetConfig();
        				
        				if (atUpdateConfig)
        				{
                       		audioFree();
                       		audioInit();	
        				}
                		
                		// ���g���̐ݒ�
                		sdrv_setrate( atRate );
                		atBufPos = 0;
                		loadFile();
                		
                		// ���݂̍Đ��ʒu�̐ݒ�
                		oldPos = audioCurrentPos();
                		atSongHeadFrame = atWriteFrame;
                    	song_framepos = oldPos;
                	}
                	
                	if ( isStopping )
                	{
                		isStopping = false;
                		closeFile();                		
                	}
                		
                	if ( isPausing || !isPlaying )
                	{
                		try {
                			sleep(100);
                		} catch (InterruptedException e) {}
                		oldPos = audioCurrentPos();
                		// lastTime = SystemClock.uptimeMillis();
                		continue;
                	}

                	if ( stop_flag )
                		break;

                	if ( isPlaying && at != null )
                	{
                		sdrv_render(atPCM, atPackSize / 2);
                		int bufsize = at.write(atPCM, 0, atPackSize);

                		// �o�b�t�@�ʒu�ւ̉��Z
                		atBufPos += bufsize;
                		atWriteFrame += (bufsize / 2);
                		
                		
                		// �o�b�t�@�ɒǉ�
                		if (NoteDataStack.size() < 64)
                		{
                    		// ���݂̉��K�f�[�^���擾
                    		int len = getTracks();
                    		Integer[] notes_obj = new Integer[64];
                    		
                    		sdrv_get_note(notes, len);
                    		
                			// ������ϊ�...
                			for ( int i = 0; i < len; i++ )
                			{
                				notes_obj[i] = notes[i];
                			}

                			NoteDataStack.add( notes_obj );
                			NoteTimeStack.add( atWriteFrame );
                		}

                		// �o�b�t�@���������ꂽ��Đ��J�n  		
                		if (!atPlay && atBufPos >= atMinBuf)
                		{
                            Log.d(TAG,"at.play");
                			atPlay = true;
                			at.play();
                		}

                		// ���݂̍Đ��|�W�V����
                		long curPos = at.getPlaybackHeadPosition();
                		song_framepos += curPos - oldPos;
                		outframes += curPos - oldPos;
                		oldPos = curPos;
                	}

                	// long currTime = SystemClock.uptimeMillis();
                	// diffTime += ( currTime - lastTime );
                	// lastTime = currTime;

                	// �Đ��|�W�V������������x�i�񂾂���X�V
                	
                	while( outframes >= atUpdateFrame )
                	{
                		outframes -= atUpdateFrame;
                		
                		// �������[�v�łȂ���΃t�F�C�h�A�E�g�����s
                		if ( !song_loop_inf && song_pos > (song_len - 3) )
                			sdrv_dofade( 3 );
                		
                		if ( song_loop_inf || song_pos < song_len )
                		{
                			// ���݂̍Đ��ʒu����Ȃ̕b�����Z�o
                			song_pos = (int) (( oldPos - atSongHeadFrame ) / atRate);

                			// �}�C�i�X�̕b���͒���
                			if (song_pos < 0)
                				song_pos = 0;                			
                		}
                		else
                		{
                			if ( sdrv_isfaded() )
                				doPlayNextSong();
                		}
                	}        		  		
                }                

            	audioFree();
                stop_flag = false;
//                Log.d("thread","finished");        		
        	}
        };   
        
        c_runner.start();
    }
}
