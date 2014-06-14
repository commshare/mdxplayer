package com.bkc.android.mdxplayer;

import java.util.Timer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class KeyView extends Activity implements NotifyBind 
{
	
	private PCMService pcmService = new PCMService();
	private PCMRender PCMBound = null;
	
	private final static String TAG = "KeyView";

	private Bitmap whiteBitmap = null;
	private Bitmap blackBitmap = null;
	
	int volume = 0;
	int pcmrate = 0;
	
	boolean drawFlag = false;

	class KeySurfaceView extends SurfaceView implements SurfaceHolder.Callback,Runnable
	{
		
		SurfaceView psv;
		
		int colorFont = Color.rgb( 0x80 , 0x80 , 0xff );
		int colorKeyOn = Color.rgb( 0xff , 0x00 , 0x00 );
		
		Timer updateTimer;

		int maxChannels = 16;
		int toneInOctave = 12;
		int maxOctave = 10;
		int maxTones = maxOctave * toneInOctave;
		
		String[] noteName = { 
				"c ","c+","d ","d+","e " ,
				"f ","f+","g ","g+","a ","a","a+","b "
		};
		
		int songPos = 0;
		int songLen = 312;
		int songMaxTone = 0;
		int songNotes[] = new int[32];
		int songLastNotes[] = new int[32];

		int key_w = 42;
		int key_h = 16;
		
		int white_w = 6;
		int white_h = 16;
		int black_w = (int)4.5;
		int black_h = (int)9.6;
		int kp[] = {0,3,6,9,12,18,21,24,27,30,33,36};
		int bn[] = {0,1,0,1,0,0,1,0,1,0,1,0};
		
		private Thread thread = null;
		
		private int songCount = 0;
		private String songTitle = "";
				
		String timeInfoString;
		
		private long lastTime = 0;
		private long delayMS = 15;
						
		// �R���X�g���N�^
		public KeySurfaceView(Context context , SurfaceView sv)
		{
			super(context);
			sv.getHolder().addCallback(this);
			psv = sv;			
		}

		// �T�[�t�F�X�̍쐬��
		@Override
		public void surfaceCreated(SurfaceHolder holder) 
		{			
			drawInit();
			thread = new Thread(this);
		}
		
		@Override
		public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2,
				int arg3) {
			if (thread != null)
				thread.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			
			thread = null;
		}
		
		// �ݒ�擾
		private void getSettings()
		{
	    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(KeyView.this);
	    	delayMS = Integer.parseInt(sp.getString("delay_key", "15"));
		}
		
		// �X���b�h�p
		public void run()
		{
			
			while(thread != null)
			{
				if (drawFlag)
					drawKey();
			}
		}
		
		private String getCurrentTimeString()
		{
			timeInfoString = 
				String.format(
					"%02d:%02d / %02d:%02d Volume:%d PCM:%dHz" ,
					songPos / 60 , 
					songPos % 60 ,
					songLen / 60 ,
					songLen % 60 , volume , pcmrate );
			
			return timeInfoString;
		}

		
		public void drawBitmap( Bitmap bitmap , Canvas canvas , int x , int y , Paint paint )
		{
			canvas.drawBitmap(bitmap, x, y, paint);
		}

		private void drawKeyRect( Canvas canvas , int x , int y , int note )
		{
			int w,h;
			
			if (note < 0)
				return;
			
			note %= toneInOctave;
			
			w = white_w;
			h = white_h;
			
			if ( bn [ note ] != 0)
			{
				w = black_w;
				h = black_h;
			}
			
			x += kp[ note ];
			
			Paint paint = new Paint();
			paint.setColor( colorKeyOn );
			
			canvas.drawRect(x, y, x + w, y + h, paint);
		}
		
		private void drawKeyboard ( Canvas canvas , int x , int y , int note , Paint paint )
		{
			int xx,i;
			int oct = 0,tone = 0;
		
			// note < 0 �͕\�����Ȃ� = �x��
			if ( note >= 0 )
			{
				oct = note / toneInOctave;
				tone = note % toneInOctave;
			}
			
			for ( i = 0; i < maxOctave; i++ )
			{
				xx = x + (key_w * i);
				drawBitmap ( whiteBitmap  , canvas , xx , y , paint );
				if ( note >= 0 && oct == i && bn[ tone ] == 0)
					drawKeyRect( canvas , xx , y , note );

				drawBitmap ( blackBitmap , canvas , xx , y , paint );
				 
				if ( note >= 0 && oct == i && bn[ tone ] != 0)
					drawKeyRect( canvas , xx , y , note );
			}			
		}

		Paint clearPaint = new Paint();
		
		private void clearLine( Canvas canvas , int y, int height )
		{
			canvas.drawRect(0 , y , canvas.getWidth(), y + height , clearPaint);			
		}
		
		// �����_��������𓾂�
		public void checkUpdate()
		{
			if ( PCMBound == null )
				return;

			int count = PCMBound.getSongCount();
			
			// �Ȃ��ύX���ꂽ
			if (count != songCount)
			{
				getSettings();
				songCount = count;
				songTitle = PCMBound.getTitle();
				songLen = PCMBound.getLen();
				songMaxTone = PCMBound.getTracks();
				volume = PCMBound.getVolume();
				pcmrate = PCMBound.getPCMRate();
				
				drawInit();
				for ( int i = 0; i < songMaxTone; i++)
				{
					songLastNotes[i] = -2;
					songNotes[i] = -1;
				}
			}
			songPos = PCMBound.getPos();
			PCMBound.getCurrentNotes( songNotes  , songMaxTone );	
		}
		
		public void clearCanvas()
		{
			SurfaceHolder holder = psv.getHolder();
			for ( int i = 0; i < 2; i++ )
			{
				Canvas canvas = holder.lockCanvas();	
				canvas.drawColor( Color.BLACK );
				holder.unlockCanvasAndPost( canvas );
			}
		}
		
		public void drawInit()
		{
			clearPaint.setColor( Color.BLACK );

			paint.setAntiAlias( false );
			paint.setColor( colorFont );
			paint.setTextSize( 20 );

			clearCanvas();
		}

		Paint paint = new Paint();
		
		public void drawKey()
		{
			int key_xstep = 20;
			int line_step = 24;
			int draw_h = 0;
			checkUpdate();
			
			SurfaceHolder holder = psv.getHolder();
			Canvas canvas = holder.lockCanvas();

			draw_h = canvas.getHeight();

			clearLine( canvas, 0 , line_step * 2 ); 
			
			String infoTime = getCurrentTimeString();
			canvas.drawText( 
					songTitle,
					0, 
					line_step,
					paint );

			canvas.drawText( 
					infoTime,
					0, 
					line_step * 2,
					paint );
			
			int key_space = 4;
			int key_ww = (key_w * maxOctave);

			int str_h = 20;
			int str_x = 0;
			int key_x = 0;

			int str_iy = ((int)line_step * 2) + str_h;
			
			int key_iy = str_iy + key_space;		
			int key_y = key_iy;
			int str_y = str_iy;
			
			// �`�����l���������L�[�{�[�h��`��
			for (int i = 0; i < songMaxTone; i++ )
			{
				int data = songNotes[i];
				
				/* �_�u���t���[���o�b�t�@�ŕ\���������H�I
				if (songLastNotes[i] == data)
				{
					str_y = str_y + key_h + 24;
					key_y = key_y + key_h + 24;
					continue;
				} */
				
				songLastNotes[i] = data;
				
				canvas.drawRect(str_x, str_y - str_h, str_x + key_ww, str_y + key_space, clearPaint);
				// clearLine( canvas, str_y - 20, 26 );
								
				String noteInfo = "rest";

				if (data >= 0)
					noteInfo = getString(R.string.note_info, 
							data / toneInOctave ,
							noteName[data % toneInOctave]
						);
				
											
				canvas.drawText ( getString(R.string.ch_info,
						i + 1,
						noteInfo ) , str_x , str_y, paint );
				
				str_y = str_y + key_h + line_step;
				
				drawKeyboard( canvas , key_x , key_y , data , paint );
				key_y = key_y + key_h + line_step;
				
				// ��ʂ��͂ݏo��̂Ŏ��̗��
				if (key_y + key_h >= draw_h )
				{
					str_x += key_ww + key_xstep;
					key_x = str_x;
					
					str_y = str_iy; 
					key_y = key_iy;
				}
			}
			
			holder.unlockCanvasAndPost(canvas);	

			long currentTime = System.currentTimeMillis();
			long diffTime = currentTime - lastTime;
			if (diffTime < delayMS && diffTime > 0)
			{
				try {
					Thread.sleep( delayMS - diffTime );
				} catch(Exception e) {}

				lastTime += delayMS;
			} else
				lastTime = currentTime;

		}
	}
	
	void loadBitmap()
	{
		if (whiteBitmap != null)
			return;
		
		Resources r = this.getResources();
		
		// �N���b�V���e�X�g�p�R�[�h
		// int h = whiteBitmap.getHeight();
		
		whiteBitmap = 
			BitmapFactory.decodeResource(
					r, R.drawable.kbd_white_42x16 );
		
		blackBitmap = 
			BitmapFactory.decodeResource(
					r , R.drawable.kbd_black_42x16 );
		
		Log.d(TAG , 
				String.format("blackBitmap w:%d h:%d",
						blackBitmap.getWidth(),
						blackBitmap.getHeight()
						));
	
		whiteBitmap.prepareToDraw();
		blackBitmap.prepareToDraw();
	}
	
	private int width;
	private int height;
	private int leftSide;
	private int rightSide;
	private int topSide;	
	private int bottomSide;
	
	@SuppressWarnings("deprecation")
	private void getScreenSize()
	{
		Display disp = ((WindowManager)getSystemService(WINDOW_SERVICE))
					.getDefaultDisplay();
		
		width  = disp.getWidth();
		height = disp.getHeight();
		
		leftSide   = (int)(width * 0.2);
		rightSide  = (int)(width * 0.8);
		topSide    = (int)(height * 0.2);
		bottomSide = (int)(height * 0.8);
	}
	
    @Override
	public boolean onTouchEvent(MotionEvent event) {
    	float x = event.getX();
    	float y = event.getY();
    	
    	if (event.getAction() == MotionEvent.ACTION_DOWN && PCMBound != null )
    	{
    		if ( x <= leftSide )
    		{
    			PCMBound.doPlayPrevSong();
    			PCMBound.doUpdate();
    		}
    		if ( x >= rightSide )
    		{
    			PCMBound.doPlayNextSong();
    			PCMBound.doUpdate();    			
    		}
    		if ( y <= topSide )
    		{
    			PCMBound.doVolumeUp();
    			PCMBound.doUpdate();
    			volume = PCMBound.getVolume();
    		}
    		
    		if ( y >= bottomSide )
    		{
    			PCMBound.doVolumeDown();
    			PCMBound.doUpdate();
    			volume = PCMBound.getVolume();
    		}
    	}
    	
		return super.onTouchEvent(event);
	}


	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        loadBitmap();
    	Log.d(TAG,"onCreate");
    }
    
    @Override
	protected void onResume() 
    {
    	setContentView(R.layout.keyview);
        
        SurfaceView sv = (SurfaceView)findViewById( R.id.sv1 );
        new KeySurfaceView(this , sv);
        pcmService.doBindService( this , new Intent( KeyView.this , PCMRender.class ) , this );
        
        getScreenSize();

		super.onResume();
		
		drawFlag = true;
	}

	@Override
    public void onPause()
    {
    	Log.d(TAG,"onPause");
    	drawFlag = false;
    	
		pcmService.doUnbindService( this );
		super.onPause();
    }

    @Override
    public void onDestroy()
    {
    	Log.d(TAG,"onDestory");
		pcmService.doUnbindService( this );
		super.onDestroy();
    }
    


	@Override
	public void notifyConnected(PCMRender pcm) {
		PCMBound = pcm;
	}


	@Override
	public void notifyDisconnected() {
		PCMBound = null;
	}
}