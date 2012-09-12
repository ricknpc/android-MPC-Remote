package rickflail.mpc.remote;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.view.WindowManager;

public class MPC_RemoteActivity extends Activity {

	Boolean isPaused = false;
	Boolean lastRequestSuccessful = false;
	Boolean keepScreenOn = false;
	Boolean lockPortrait = false;
	int failedRequests = 0;
	int maxFailedRequests = 10;
	int visibleRequests = 0;
	
	SharedPreferences prefs;
	Handler resultsTimer;
	SendCommandTask sendCommandTask;
	GetVarsTask getVarsTask;
	
	Intent settingsIntent;
	
	String server;
	String port;
	
	ImageButton btnJumpBack;
	ImageButton btnStop;
	ImageButton btnPlay;
	ImageButton btnPause;
	ImageButton btnJumpForward;
	
	ImageButton btnPrev;
	ImageButton btnLJumpBack;
	ImageButton btnLJumpForward;
	ImageButton btnNext;
	
	ImageButton btnFullscreen;
	ImageButton btnScreenshot;
	ImageButton btnSubtitles;
	ImageButton btnFramestep;
	
	Button btnRetry;

	TextView valStatus;
	TextView valFile;
	TextView valTime;

	ProgressBar barNetwork;
	ProgressBar barTime;
	
	LinearLayout main;
	LinearLayout areaExtra;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.main);
        
        settingsIntent = new Intent(this, MPC_RemoteSettingsActivity.class);
        
        /*MAIN CONTROLS*/
        btnJumpBack = (ImageButton) findViewById(R.id.buttonJumpBack);
        btnJumpBack.setOnClickListener(sendCommand(getString(R.string.mjumpback)));
        
        btnStop = (ImageButton) findViewById(R.id.buttonStop);
        btnStop.setOnClickListener(sendCommand(getString(R.string.stop)));
        
        btnPlay = (ImageButton) findViewById(R.id.buttonPlay);
        btnPlay.setOnClickListener(sendCommand(getString(R.string.play)));
        
        btnPause = (ImageButton) findViewById(R.id.buttonPause);
        btnPause.setOnClickListener(sendCommand(getString(R.string.pause)));
        
        btnJumpForward = (ImageButton) findViewById(R.id.buttonJumpForward);
        btnJumpForward.setOnClickListener(sendCommand(getString(R.string.mjumpforward)));
        
        /*EXTRA CONTROLS*/
        btnPrev = (ImageButton) findViewById(R.id.buttonPrev);
        btnPrev.setOnClickListener(sendCommand(getString(R.string.prev)));
        
        btnLJumpBack = (ImageButton) findViewById(R.id.buttonLargeJumpBack);
        btnLJumpBack.setOnClickListener(sendCommand(getString(R.string.ljumpback)));
        
        btnLJumpForward = (ImageButton) findViewById(R.id.buttonLargeJumpForward);
        btnLJumpForward.setOnClickListener(sendCommand(getString(R.string.ljumpforward)));
        
        btnNext = (ImageButton) findViewById(R.id.buttonNext);
        btnNext.setOnClickListener(sendCommand(getString(R.string.next)));

        btnFullscreen = (ImageButton) findViewById(R.id.buttonFullscreen);
        btnFullscreen.setOnClickListener(sendCommand(getString(R.string.fullscreen)));
        
        btnScreenshot = (ImageButton) findViewById(R.id.buttonScreenshot);
        btnScreenshot.setOnClickListener(sendCommand(getString(R.string.screenshot)));

        btnSubtitles = (ImageButton) findViewById(R.id.buttonSubtitles);
        btnSubtitles.setOnClickListener(sendCommand(getString(R.string.subtitles)));
        
        btnFramestep = (ImageButton) findViewById(R.id.buttonFramestep);
        btnFramestep.setOnClickListener(sendCommand(getString(R.string.stepforward)));
        
        
        btnRetry = (Button) findViewById(R.id.buttonRetry);
        btnRetry.setOnClickListener(retryListener());

        valStatus = (TextView) findViewById(R.id.valueStatus);
        valFile = (TextView) findViewById(R.id.valueFile);
        valTime = (TextView) findViewById(R.id.valueTime);
        
        barTime = (ProgressBar) findViewById(R.id.barTime);
        barNetwork = (ProgressBar) findViewById(R.id.barNetwork);
        
        main = (LinearLayout) findViewById(R.id.main);
        areaExtra = (LinearLayout) findViewById(R.id.areaExtra);
        
        
        resultsTimer = new Handler();
    }
    
    @Override
    public void onPause() {
    	isPaused = true;
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	prefs = getSharedPreferences(getString(R.string.app_settings_file), Context.MODE_PRIVATE);
        server = prefs.getString("server", "unset");
        port = prefs.getString("port", "unset");
        
        if (server == "unset" || port == "unset") {
        	startActivity(settingsIntent);        	
        	return;
        }
        
        keepScreenOn = prefs.getBoolean("keepScreenOn", false);
        main.setKeepScreenOn(keepScreenOn);
        
        lockPortrait = prefs.getBoolean("lockPortrait", false);
        if (lockPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else {
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        	Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int orientation = display.getOrientation();
            if (orientation == 1 || orientation == 3) {
            	areaExtra.setVisibility(View.GONE);
            } else if (orientation == 0) {
            	areaExtra.setVisibility(View.VISIBLE);
            }
        }        
    	
    	retry();
    }
    
    @Override
    public void onConfigurationChanged(Configuration config) {
    	if (lockPortrait) return;
    	
    	super.onConfigurationChanged(config);
    	
		if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	    	areaExtra.setVisibility(View.GONE);
		} else if (config.orientation == Configuration.ORIENTATION_PORTRAIT){
	    	areaExtra.setVisibility(View.VISIBLE);
		}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.settings) {
        	startActivity(settingsIntent);
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void setError(String error) {
    	failedRequests += 1;
    	if (failedRequests >= maxFailedRequests) {
    		valStatus.setVisibility(View.GONE);
    		btnRetry.setVisibility(View.VISIBLE);
    		isPaused = true;
    	} else {
        	valStatus.setText(getString(R.string.status_value_status));
    	}
    	
    	valFile.setText(getString(R.string.status_value_file));
    	valTime.setText(getString(R.string.status_value_time));
    	barTime.setProgress(0);
    }
    
    public OnClickListener sendCommand(final String command) {
    	return new View.OnClickListener() {
    		public void onClick(View view) {
    			sendCommandTask = new SendCommandTask();
    			sendCommandTask.execute(command);
    		}
    	};
    }
    
    public OnClickListener retryListener() {
    	return new View.OnClickListener() {
    		public void onClick(View view) {
    			retry();
    		}
    	};
    }
    
    public void retry() {
    	failedRequests = 0;
    	valStatus.setVisibility(View.VISIBLE);
		btnRetry.setVisibility(View.GONE);
		isPaused = false;
		valStatus.setText(getString(R.string.status_value_status));
		resultsTimer.post(runFetchResults);
    }
    
    private Runnable runFetchResults = new Runnable() {
    	public void run() {
    		startFetchVarsTask();
        	
    		if (!isPaused) resultsTimer.postDelayed(this, 1000);
    	}
    };
    
    private void startFetchVarsTask() {
    	if (getVarsTask != null) {
			getVarsTask.cancel(false);
		}
		getVarsTask = new GetVarsTask();
		getVarsTask.execute();
    }
    
    class SendCommandTask extends AsyncTask<String, Void, String> {

    	@Override
    	protected String doInBackground(String... commands) {
    		Communication comm = new Communication(server, port);
    		String html = "";
    		
    		for (int i = 0; i < commands.length; i++) {
    			 html += comm.sendCommand(commands[i]);
    		}
    		
    		return html;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		visibleRequests += 1;
    		barNetwork.setVisibility(View.VISIBLE);
    	}
    	
    	@Override
    	protected void onPostExecute(String html) {
    		visibleRequests -= 1;
    		if (visibleRequests == 0) barNetwork.setVisibility(View.INVISIBLE);

			lastRequestSuccessful = false;
    		
    		Pattern regex = Pattern.compile("error: (.*)");
    		Matcher match = regex.matcher(html);
    		if (match.find()) {
    			setError(match.group(1));
    		} else {
    			if (isPaused) retry();
    			else startFetchVarsTask();
    		}
    	}
    	
    	@Override
    	protected void onCancelled() {
    		visibleRequests -= 1;
    		if (visibleRequests == 0) barNetwork.setVisibility(View.INVISIBLE);
    	}
    	
    }
    
    class GetVarsTask extends AsyncTask<Void, Void, HashMap<String, String>> {
    	
    	Boolean thisRequestVisible = false;

    	@Override
    	protected HashMap<String, String> doInBackground(Void... params) {
    		Communication comm = new Communication(server, port);
    		HashMap<String, String> values = comm.getVariables();
    		return values;
    	}
    	
    	@Override
    	protected void onPreExecute() {
    		if (!lastRequestSuccessful) {
    			thisRequestVisible = true;
    			visibleRequests += 1;
        		barNetwork.setVisibility(View.VISIBLE);
    		}
    	}
    	
    	@Override
    	protected void onPostExecute(HashMap<String, String> values) {
    		if (thisRequestVisible) {
	    		visibleRequests -= 1;
	    		if (visibleRequests == 0) barNetwork.setVisibility(View.INVISIBLE);
    		}
    		
    		if (values.containsKey("error")) {
    			lastRequestSuccessful = false;
    			setError(values.get("error"));
    			return;
    		}
    		
    		lastRequestSuccessful = true;
    		
    		if (values.containsKey("statestring") && values.get("statestring").equals("n/a")) {
    			valStatus.setText("N/A");
    			valFile.setText("No File Loaded");
    		} else {
    			if (values.containsKey("statestring")) valStatus.setText(values.get("statestring"));
        		if (values.containsKey("filename")) valFile.setText(values.get("filename"));
    		}
        	if (values.containsKey("positionstring") && values.containsKey("durationstring")) {
        		valTime.setText(values.get("positionstring") + " / " + values.get("durationstring"));
        	}
        	
        	if (values.containsKey("position") && values.containsKey("duration")) {
	        	float pct = (float)Integer.parseInt(values.get("position")) / (float)Integer.parseInt(values.get("duration"));
	        	barTime.setProgress(Math.round(pct * barTime.getMax()));
        	}
    	}
    	
    	@Override
    	protected void onCancelled() {
    		if (thisRequestVisible) {
	    		visibleRequests -= 1;
	    		if (visibleRequests == 0) barNetwork.setVisibility(View.INVISIBLE);
    		}
    	}
    	
    }
}

