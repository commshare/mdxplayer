package com.bkc.android.mdxplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class PCMService
{
	private String TAG = "PCMService";
	private boolean isBindService = false;
	private NotifyBind notify;
	
    // �o�C���h
    public void doBindService( Context ctx , Intent intent , NotifyBind destNotify )
    {
    	if ( ! isBindService )
    	{
    		Log.d(TAG, "BindService");
    		ctx.bindService( intent , PCMConnect , Context.BIND_AUTO_CREATE  );
    		isBindService = true;
    		notify = destNotify;
    	}
    }
    

    // �A���o�C���h
    public void doUnbindService( Context ctx )
    {
    	if ( isBindService )
    	{
    		Log.d(TAG, "UnbindService");
        	ctx.unbindService(PCMConnect);
        	isBindService = false;  	
    	}
    }
    
    // �o�C���h�p�N���X
    private ServiceConnection PCMConnect = new ServiceConnection()
    {
    	// �ڑ�
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			notify.notifyConnected( ((PCMRender.LocalBinder)service).getService() );
		}

		// �ؒf
		@Override
		public void onServiceDisconnected(ComponentName className) {
    		Log.d(TAG, "onServiceDisconnected");
    		notify.notifyDisconnected();
			isBindService = false;
		}
    };
}
