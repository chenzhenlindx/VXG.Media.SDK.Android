/*
 *
  * Copyright (c) 2011-2017 VXG Inc.
 *
 */


package veg.mediaplayer.sdk.testmulti;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import android.preference.PreferenceManager;

import veg.mediaplayer.sdk.MediaPlayer;
import veg.mediaplayer.sdk.MediaPlayer.PlayerNotifyCodes;
import veg.mediaplayer.sdk.MediaPlayer.VideoShot;


public class MainActivity extends Activity implements OnClickListener
{
    private static final String TAG 	 = "MediaPlayerTest";
    
	public  static AutoCompleteTextView	edtIpAddress;
	public  static ArrayAdapter<String> edtIpAddressAdapter;
	public  static Set<String>			edtIpAddressHistory;
	private Button						btnConnect;
	private Button						btnHistory;
	
	private SharedPreferences 			settings;
    private SharedPreferences.Editor 	editor;

    private boolean 					playing = false;
    private MainActivity 				mthis = null;

	private ArrayList<Player> 			players = null;
	private LinearLayout 				playerLayoutList = null;
	private ScrollView					mainScrollView = null;
    
	public ScaleGestureDetector 		detectors = null;	

    private MulticastLock 				multicastLock = null;
    

    
    @Override
    public void onCreate(Bundle savedInstanceState) 
	{
		String  strUrl;

		setTitle(R.string.app_name);
		super.onCreate(savedInstanceState);

		WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		multicastLock = wifi.createMulticastLock("multicastLock");
		multicastLock.setReferenceCounted(true);
		multicastLock.acquire();
		
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
		
		setContentView(R.layout.main);
		mthis = this;
		
		settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

		SharedSettings.getInstance(this).loadPrefSettings();
		SharedSettings.getInstance().savePrefSettings();
		
		playerLayoutList = (LinearLayout) findViewById(R.id.playerLayoutList);
		playerLayoutList = (LinearLayout) findViewById(R.id.playerLayoutList);
		players = new ArrayList<Player>();
		for (int i = 0; i < 4; i++)
		{
			Player player 		= new Player(this);
			player.getPlayer().toggleMute(i == 0);
			players.add(player);
			
			playerLayoutList.addView(player);			
		}


        
		strUrl = settings.getString("connectionUrl", "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov");		 
		
		HashSet<String> tempHistory = new HashSet<String>();
		tempHistory.add("rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov");
		tempHistory.add("http://devimages.apple.com/iphone/samples/bipbop/gear1/prog_index.m3u8");
			
		edtIpAddressHistory = settings.getStringSet("connectionHistory", tempHistory);

		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
	
		edtIpAddress = (AutoCompleteTextView)findViewById(R.id.edit_ipaddress);
		edtIpAddress.setText(strUrl);

		edtIpAddress.setOnEditorActionListener(new OnEditorActionListener() 
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId,	KeyEvent event) 
			{
				if (event != null&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) 
				{
					InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					in.hideSoftInputFromWindow(edtIpAddress.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					return true;
	
				}
				return false;
			}
		});

		btnHistory = (Button)findViewById(R.id.button_history);

		// Array of choices
		btnHistory.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v) 
			{
				InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				in.hideSoftInputFromWindow(MainActivity.edtIpAddress.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				
				if (edtIpAddressHistory.size() <= 0)
					return;

				MainActivity.edtIpAddressAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.history_item, new ArrayList<String>(edtIpAddressHistory));
				MainActivity.edtIpAddress.setAdapter(MainActivity.edtIpAddressAdapter);
				MainActivity.edtIpAddress.showDropDown();
			}   
		});

		btnConnect = (Button)findViewById(R.id.button_connect);
        btnConnect.setOnClickListener(this);
        
        LinearLayout layout = (LinearLayout) findViewById(R.id.main_view);
        layout.setOnTouchListener(new OnTouchListener() 
		{
			public boolean onTouch(View v, MotionEvent event) 
			{
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				if (getWindow() != null && getWindow().getCurrentFocus() != null && getWindow().getCurrentFocus().getWindowToken() != null)
					inputManager.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
				return true;
			}
		});
        
		mainScrollView = (ScrollView) findViewById(R.id.mainScrollView);
		mainScrollView.post(new Runnable() {            
		    @Override
		    public void run() {
		    	mainScrollView.fullScroll(View.FOCUS_DOWN);              
		    }
		});
		
		setShowControls();
        
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
    	super.onConfigurationChanged(newConfig);

    	mainScrollView.post(new Runnable() 
    	{            
		    @Override
		    public void run() 
		    {
		    	mainScrollView.fullScroll(View.FOCUS_DOWN);              
		    }
		});
    }
    
    public void onClick(View v) 
	{
		String url  = edtIpAddress.getText().toString();
		if (url.isEmpty())
			return;

		if (!edtIpAddressHistory.contains(url))
			edtIpAddressHistory.add(url);
		
		SharedSettings.getInstance().loadPrefSettings();

		if (!playing)
		{
	        for (int i=0; i < players.size(); i++) 
	        {
				 players.get(i).Open(url);
	        }
//			players.get(0).Open("rtsp://172.29.1.164:556/realplay://43100dd7e48d45038c9bea7920591d83:MAIN:TCP?cnid=1&pnid=0&token=&auth=30&redirect=0&transcode=0&resolution=2&bitrate=100&framerate=10&videotype=2&systemformat=2");
//			players.get(1).Open("rtsp://172.29.1.164:556/realplay://91a47c70915e436fbf50b23f47629092:MAIN:TCP?cnid=1&pnid=0&token=&auth=30&redirect=0&transcode=0&resolution=2&bitrate=100&framerate=10&videotype=2&systemformat=2");
//			players.get(2).Open("rtsp://172.29.1.164:556/realplay://69919b66dd3a4e18bd0681dc9742482d:MAIN:TCP?cnid=1&pnid=0&token=&auth=30&redirect=0&transcode=0&resolution=2&bitrate=100&framerate=10&videotype=2&systemformat=2");
//			players.get(3).Open("rtsp://172.29.1.164:556/realplay://8f5cf6b17e6d474989c0b8196ca8f146:MAIN:TCP?cnid=1&pnid=0&token=&auth=30&redirect=0&transcode=0&resolution=2&bitrate=100&framerate=10&videotype=2&systemformat=2");
	        btnConnect.setText("Disconnect");
	        playing = true;
		}
		else
		{
	        for (int i=0; i < players.size(); i++) 
	        {
				players.get(i).Close();
	        }
			setUIDisconnected();
		}
        
    }
 
	protected void onPause()
	{
		Log.e("SDL", "onPause()");
		super.onPause();

		editor = settings.edit();
		editor.putString("connectionUrl", edtIpAddress.getText().toString());

		editor.putStringSet("connectionHistory", edtIpAddressHistory);
		editor.commit();
		
        for (int i=0; i < players.size(); i++) 
        	players.get(i).getPlayer().onPause();
	}

	@Override
  	protected void onResume() 
	{
		Log.e("SDL", "onResume()");
		super.onResume();
        for (int i=0; i < players.size(); i++) 
        	players.get(i).getPlayer().onResume();
		btnConnect.performClick();
  	}

  	@Override
	protected void onStart() 
  	{
      	Log.e("SDL", "onStart()");
		super.onStart();
        for (int i=0; i < players.size(); i++) 
        	players.get(i).getPlayer().onStart();
	}

  	@Override
	protected void onStop() 
  	{
  		Log.e("SDL", "onStop()");
		super.onStop();
        for (int i=0; i < players.size(); i++) 
        	players.get(i).getPlayer().onStop();

	}

    @Override
    public void onBackPressed() 
    {
	    for (int i=0; i < players.size(); i++) 
	    {
	    	players.get(i).Close();
			playerLayoutList.removeView(players.get(i));			
	    }
	    
	    super.onBackPressed();
    }
  	
  	@Override
  	public void onWindowFocusChanged(boolean hasFocus) 
  	{
  		Log.e("SDL", "onWindowFocusChanged(): " + hasFocus);
  		super.onWindowFocusChanged(hasFocus);
        for (int i=0; i < players.size(); i++) 
        	players.get(i).getPlayer().onWindowFocusChanged(hasFocus);
  	}

  	@Override
  	public void onLowMemory() 
  	{
  		Log.e("SDL", "onLowMemory()");
  		super.onLowMemory();
        for (int i=0; i < players.size(); i++) 
        	players.get(i).getPlayer().onLowMemory();
  	}

  	@Override
  	protected void onDestroy() 
  	{
  		Log.e("SDL", "onDestroy()");
        for (int i=0; i < players.size(); i++) 
        	players.get(i).getPlayer().onDestroy();
        
		System.gc();
		
		if (multicastLock != null) {
		    multicastLock.release();
		    multicastLock = null;
		}		
		super.onDestroy();
   	}	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)  
	{
		switch (item.getItemId())  
		{
			case R.id.main_opt_settings:   
		
				SharedSettings.getInstance().loadPrefSettings();

				Intent intentSettings = new Intent(MainActivity.this, PreferencesActivity.class);     
				startActivity(intentSettings);

				break;
			case R.id.main_opt_clearhistory:     
			
				new AlertDialog.Builder(this)
				.setTitle("Clear History")
				.setMessage("Do you really want to delete the history?")
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int which) 
					{

						edtIpAddressHistory.clear();

					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int which) 
					{
						// do nothing
					}
				}).show();
				break;
			case R.id.main_opt_exit:
				finish();
				break;

		}
		return true;
	}

	protected void setUIDisconnected()
	{
		setTitle(R.string.app_name);
		btnConnect.setText("Connect");
		playing = false;
	}

	protected void setHideControls()
	{
		ActionBar actionBar = getActionBar();
		actionBar.hide(); // slides out

		edtIpAddress.setVisibility(View.GONE);
		btnHistory.setVisibility(View.GONE);
		btnConnect.setVisibility(View.GONE);
	}

	protected void setShowControls()
	{
		ActionBar actionBar = getActionBar();
		actionBar.show(); // slides out
		setTitle(R.string.app_name);
		
		edtIpAddress.setVisibility(View.VISIBLE);
		btnHistory.setVisibility(View.VISIBLE);
		btnConnect.setVisibility(View.VISIBLE);
		
		mainScrollView.post(new Runnable() {            
		    @Override
		    public void run() {
		    	mainScrollView.fullScroll(View.FOCUS_DOWN);              
		    }
		});
		
	}
	
}
