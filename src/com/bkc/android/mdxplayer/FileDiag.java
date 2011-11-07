package com.bkc.android.mdxplayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class FileDiag extends Activity implements OnClickListener, OnItemClickListener {

	// �\���p�A�_�v�^
	SimpleAdapter adpt;
	ArrayList<HashMap<String, Object>> items;
	
	
	FileListObject fobj = null;
    String KEY_LASTPATH = "lastPath";
    
@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // �t�@�C���_�C�A���O�̕\��
        setContentView(R.layout.filediag);
        
        // �t�@�C�����X�g�I�u�W�F�N�g�̎擾 
        fobj = FileListObject.getInst();
      
        getLastDirectory();

        
        // �O���X�g���[�W�̃p�X
        String abspath = Environment.getExternalStorageDirectory().getAbsolutePath();
        
        // ���łɃf�B���N�g�����ǂ܂�Ă���ꍇ�͏����X�V���Ȃ�
        if (! fobj.current_path.equals(""))
        	abspath = fobj.current_path;
 
        if (fobj.openDirectory(abspath) < 0 || fobj.getNameList().size() < 1)
        {
        	
        	Toast.makeText(FileDiag.this, "Can't access SD Card!! \nPlease make sure unmounted!", Toast.LENGTH_LONG).show();
        	fobj.current_path = "";
        }        	
        makeFileList();

        // ((Button)this.findViewById(R.id.file_cancel)).setOnClickListener( this );        
    }
	
	
	@Override
	protected void onDestroy() {
		SharedPreferences.Editor edit = getSharedPreferences("mdxplay",MODE_PRIVATE).edit();
		edit.putString( KEY_LASTPATH, fobj.current_path );
		edit.commit();

		super.onDestroy();
	}

	// �Ō�ɊJ�����f�B���N�g�����J��
	private void getLastDirectory()
	{
		SharedPreferences pref = getSharedPreferences("mdxplay",MODE_PRIVATE);    
		
		fobj.current_path = pref.getString( KEY_LASTPATH , "");
        if (fobj.openDirectory( fobj.current_path ) < 0)
        	fobj.current_path = "";
        
        makeFileList();
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
 
	public void onClick(View v)
	{
/*		switch(v.getId())
		{
		case R.id.file_cancel:
			Intent intent = new Intent();
			setResult(RESULT_CANCELED,intent);
			finish();
			break;
		}*/
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) 
	{
		File file = fobj.getFileList().get(pos);
		
		if (file.isDirectory())
		{
			// �f�B���N�g�����J��
			fobj.openDirectory(file.getAbsolutePath());
			makeFileList();
		}
		else
		{
			Intent intent = getIntent();
			
			// �t�@�C�����J��
			fobj.position = pos;
			fobj.path = file.getAbsolutePath();
			
			setResult(RESULT_OK,intent);
			finish();
		}
	}
}
