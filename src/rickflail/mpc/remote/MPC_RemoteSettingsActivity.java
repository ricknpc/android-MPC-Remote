package rickflail.mpc.remote;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MPC_RemoteSettingsActivity extends Activity {
	
	SharedPreferences prefs;
	
	LinearLayout settings;
	
	EditText txtServer;
	EditText txtPort;
	CheckBox cbScreen;
	CheckBox cbPortrait;

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
	}
	
	@Override
	public void onResume() {
		super.onResume();
        
        prefs = getSharedPreferences(getString(R.string.app_settings_file), Context.MODE_PRIVATE);
        String server = prefs.getString("server", "unset");
        String port = prefs.getString("port", "unset");

    	Button btnContinue = (Button) findViewById(R.id.buttonContinue);
        
        if (server == "unset" || port == "unset") {
        	btnContinue.setVisibility(View.VISIBLE);
        	server = getString(R.string.default_server);
        	port = getString(R.string.default_port);
        }
        
        btnContinue.setOnClickListener(continueClick(this));
        
        Boolean keepScreenOn = prefs.getBoolean("keepScreenOn", false);
        settings = (LinearLayout) findViewById(R.id.settings);
        settings.setKeepScreenOn(keepScreenOn);
        
        Boolean lockPortrait = prefs.getBoolean("lockPortrait", false);
        if (lockPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        
        cbScreen = (CheckBox) findViewById(R.id.checkBoxKeepScreenOn);
        cbScreen.setChecked(keepScreenOn);
        cbScreen.setOnCheckedChangeListener(toggleKeepScreenOn());
        
        cbPortrait = (CheckBox) findViewById(R.id.checkBoxLockPortrait);
        cbPortrait.setChecked(lockPortrait);
        cbPortrait.setOnCheckedChangeListener(toggleLockPortrait(this));
        
        txtServer = (EditText) findViewById(R.id.textFieldServer);
        txtPort = (EditText) findViewById(R.id.textFieldPort);
        
        txtServer.setText(server);
        txtPort.setText(port);		
	}
	
	@Override
    public void onPause() {
		SharedPreferences.Editor editor = prefs.edit();
		
		editor.putString("server", txtServer.getText().toString());
		editor.putString("port", txtPort.getText().toString());
		editor.putBoolean("keepScreenOn", cbScreen.isChecked());
		editor.putBoolean("lockPortrait", cbPortrait.isChecked());
		
		editor.commit();
		
    	super.onPause();
    }
	
	public OnCheckedChangeListener toggleKeepScreenOn() {
    	return new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				settings.setKeepScreenOn(isChecked);
			}
		};
    }
	
	public OnCheckedChangeListener toggleLockPortrait(final Activity activity) {
    	return new CompoundButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				else activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
			}
		};
    }
	
	public OnClickListener continueClick(final Activity activity) {
		return new View.OnClickListener() {
			public void onClick(View view) {
				activity.finish();
			}
		};
	}

}
