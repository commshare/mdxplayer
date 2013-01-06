package com.bkc.android.mdxplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class FileDiag extends Activity implements OnClickListener, OnItemClickListener {

	// �\���p�A�_�v�^
	SimpleAdapter adpt;
	ArrayList<HashMap<String, Object>> items;
	
	
	FileListObject fobj = null;
    static String KEY_DIRMODE = "dirMode";
    
    private boolean dirMode = false;
    private String lastPath = "";
    
    
@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // �t�@�C���_�C�A���O�̕\��
        setContentView(R.layout.filediag);
        
        // Intent�̎擾
		Intent intent = getIntent();	
		dirMode = intent.getBooleanExtra( KEY_DIRMODE, false ); 

		if ( dirMode == false )
		{
			// �t�@�C�����X�g�I�u�W�F�N�g�̎擾 
	        fobj = FileListObject.getInst();
		}
		else
		{
			// ���t�@�C�����X�g
			fobj = FileListObject.getTemporalInst();
		}

        loadPref();
        
        if ( getDirectory( lastPath ) < 0 &&
        		getDirectory( Environment.getExternalStorageDirectory().getAbsolutePath() ) < 0)
        {
        	Toast.makeText(FileDiag.this, "Can't access SD Card!! \nPlease make sure unmounted!", Toast.LENGTH_LONG).show();
        	fobj.current_path = "";
        }
        
        
        Button fileOk = (Button)this.findViewById(R.id.file_ok);
        if ( dirMode == false )
               	fileOk.setVisibility(Button.GONE);

       	fileOk.setOnClickListener( this );
        	
    }
	
	
	@Override
	protected void onDestroy() {
		savePref();
		super.onDestroy();
	}
	
	private void savePref()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (pref == null) return;
		
		SharedPreferences.Editor edit = pref.edit();
		
		if (edit == null) return;
		
		edit.putString( Player.KEY_LASTPATH, fobj.current_path );
		edit.commit();
	}
	
	private void savePrefForPCM()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (pref == null) return;
		
		SharedPreferences.Editor edit = pref.edit();
		
		if (edit == null) return;
		
		edit.putString( Player.KEY_PCMPATH, fobj.current_path );
		edit.commit();
	}

	
	private void loadPref()
	{
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);   
	
		if (pref != null)
			lastPath = pref.getString( Player.KEY_LASTPATH , "");
		else
			lastPath = "";
	}
	
	private void setCurrentPathInfo()
	{
        TextView pathInfo = (TextView)this.findViewById(R.id.fileDiagText2);
        pathInfo.setText( String.format("�t�H���_:%s",fobj.current_path ));
	}
	

	// �f�B���N�g�����J��
	private int getDirectory( String path )
	{
        if ( path.equals("") || fobj.openDirectory( path ) < 0 )
        {
        	fobj.current_path = "";
            makeFileList();
        	return -1;
        }
        
        makeFileList();
        setCurrentPathInfo();
        return 0;
	}
	
	// �\���p�t�@�C�����X�g�̍쐬
	private void makeFileList()
	{
        // ���X�g�r���[�̏�����
        ListView lv = (ListView) this.findViewById(R.id.filelist);
        lv.setOnItemClickListener(this);

        // �z��̏�����
		items = new ArrayList<HashMap<String, Object>>();
    	
    	for (String name : fobj.getNameList())
        {
    		String data;
            HashMap<String, Object> map = new HashMap<String, Object>(); 
    		String title = fobj.getTitleFromFile( name );
    		Integer len = fobj.getLengthFromFile( name );
                      
    		data = "";

    		if ( title == null )
    			map.put("title", name);
    		else 
    		{
    			map.put("title", title );
    		
    			if ( len == null )
    				data = String.format("%s", name );
    			else
    				data = String.format("%s Time:%02d:%02d", name , len / 60, len % 60  );
    		}
    		map.put("data" , data);
    		
    		items.add(map);           
        }
    	
    	adpt = new SimpleAdapter(this , items , R.layout.list ,
    			new String[] { "title","data" } ,
    			new int[] { android.R.id.text1 , android.R.id.text2 } );
    	
        lv.setAdapter(adpt);

	}
	
	private void finishForDirMode()
	{
		savePrefForPCM();
		Intent intent = getIntent();
		setResult(RESULT_OK,intent);
		finish();
	}
 
	public void onClick(View v)
	{
		switch(v.getId())
		{
			case R.id.file_ok:
				finishForDirMode();
			break;
		}
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) 
	{
		File file = fobj.getFileList().get(pos);
		
		if (file.isDirectory())
		{
			// �f�B���N�g�����J��
			getDirectory( file.getAbsolutePath() );
		}
		else
		{
			if ( dirMode == true )
			{
				finishForDirMode();
				return;
			}
			// �t�@�C�����J��
			fobj.position = pos;
			fobj.path = file.getAbsolutePath();
			
			Intent intent = getIntent();
			setResult(RESULT_OK,intent);
			finish();
		}
	}
}
