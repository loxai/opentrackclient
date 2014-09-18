package com.loxai.opentrack;


import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	SensorServer server = null;
	EditText ipAddressEditText;
	EditText portEditText;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		ipAddressEditText = (EditText)findViewById(R.id.ipAddressEditText);
		portEditText = (EditText)findViewById(R.id.portEditText);
		ToggleButton streamSwitch = (ToggleButton)findViewById(R.id.streamToggleButton);
		streamSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton toggle, boolean checked) {
				boolean result = false;
				if (checked){
					result = startServer();
				} else{
					stopServer();
				}
				toggle.setChecked(result);
			}
			
		});
		OnClickListener trinusClick = new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				try {
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.loxai.trinus.test")));
				} catch (android.content.ActivityNotFoundException anfe) {
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.loxai.trinus.test")));
				}
			}
			
		};
		ImageView trinusImage = (ImageView)findViewById(R.id.trinusImage);
		trinusImage.setOnClickListener(trinusClick);
		TextView trinusText = (TextView)findViewById(R.id.trinusText);
		trinusText.setOnClickListener(trinusClick);
		
		loadSettings();
		return true;
	}
	@Override
	protected void onPause(){
		super.onPause();
		saveSettings();
	}
	public void userMessage(final String msg, final int duration){
		runOnUiThread(new Runnable(){
			public void run(){
				Toast.makeText(MainActivity.this, msg, duration).show();
			}
		});
	}
	private boolean startServer(){
		boolean result = false;
		
		SensorManager sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

		try{
			if (server == null)
				server = new SensorServer(sensorManager, ipAddressEditText.getText().toString(), portEditText.getText().toString(), 10);
			server.begin();
			result = true;
		}catch(Exception e){
			userMessage("Unable to initialise server: " + e.getMessage(), Toast.LENGTH_LONG);
		}
		
		return result;
	}
	private void stopServer(){
		if (server != null){
			server.end();
			server = null;
		}
	}
	private void loadSettings(){
		SharedPreferences sp= getSharedPreferences("prefs", Context.MODE_PRIVATE);
		EditText ipAddressEditText = (EditText)findViewById(R.id.ipAddressEditText);
		EditText portEditText = (EditText)findViewById(R.id.portEditText);

		ipAddressEditText.setText(sp.getString("ipText", "192.168.0.1"));
		portEditText.setText(sp.getString("portText", "5555"));
	}
	private void saveSettings(){
		SharedPreferences sp= getSharedPreferences("prefs", Context.MODE_PRIVATE);
		EditText ipAddressEditText = (EditText)findViewById(R.id.ipAddressEditText);
		EditText portEditText = (EditText)findViewById(R.id.portEditText);
		
		Editor editor = sp.edit();
		editor.putString("ipText", ipAddressEditText.getText().toString());
		editor.putString("portText", portEditText.getText().toString());
		editor.commit();
	}	
}
