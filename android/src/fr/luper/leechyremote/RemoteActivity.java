package fr.luper.leechyremote;

import java.io.IOException;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

public class RemoteActivity extends Activity implements ServiceListener {
	private static final String TAG = "LeechyRemote";
	private static final String SERVICE_NAME = "_leechyremote._tcp.local."; 
	
	private MulticastLock multicast_lock;	
	private JmDNS jmdns;
	private LeechyRemoteServer current_server;
	
    @Override
    public void onCreate(Bundle state) {        
    	String previous_server_address;
    	int previous_server_port;
    	boolean do_probe = true;   
    	
    	// Initialize GUI
        super.onCreate(state);        
        setContentView(R.layout.remote);
        installEventHandlers();	
        
        // Try to reconnect to the previous server, or probe for it
    	if (state != null) {
    		previous_server_address = state.getString("previous_server_address");
		   	previous_server_port = state.getInt("previous_server_port");	        
	        if (previous_server_address != null) {
	        	current_server = new LeechyRemoteServer(previous_server_address, previous_server_port);
	        	try {
	        		current_server.connect();
	        		Log.d(TAG, "Reconnected to " + current_server.getAddrString());
	        		do_probe = false;
	        	} catch (Exception err) { }
	        }
    	}    	
        if (do_probe) {
        	startServerProbe();         
        }    	        
    }
    
    @Override
    public void onDestroy() {
    	stopServerProbe();
    	super.onDestroy();
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	if (state != null && current_server != null) {
    		state.putString("previous_server_address", current_server.getAddress());
    		state.putInt("previous_server_port", current_server.getPort());
    	}
    }    
    
    private void installEventHandlers() {
    	// Volume controls
    	findViewById(R.id.volume_up_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("volume_up");
    		}
    	});
    	findViewById(R.id.volume_down_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("volume_down");
    		}
    	});    	
    	findViewById(R.id.mute_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("mute");
    		}
    	});    	
    	// Seek bar
    	findViewById(R.id.expand_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("toggle_fullscreen");
    		}
    	});    	

    	findViewById(R.id.rewind_1_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("rewind_1");
    		}
    	});
    	findViewById(R.id.rewind_2_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("rewind_2");
    		}
    	});
    	findViewById(R.id.rewind_3_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("rewind_3");
    		}
    	});   
    	findViewById(R.id.forward_1_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("forward_1");
    		}
    	});
    	findViewById(R.id.forward_2_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("forward_2");
    		}
    	});
    	findViewById(R.id.forward_3_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("forward_3");
    		}
    	}); 
    	// Playback control
    	findViewById(R.id.prev_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("play_previous");
    		}
    	});    	
    	findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("play_next");
    		}
    	});    	
    	findViewById(R.id.play_pause_button).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			sendAction("play_pause");
    		}
    	});
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.remote, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.probe_action:
            current_server = null;
            startServerProbe();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }    
    
    /**
     * Send an action to the currently bound server.
     */
    private void sendAction(String action) {
		if (current_server != null) {
			try {
				current_server.sendAction(action);
			} catch (Exception err) {
				notifyUser("Error sending action to server: " + err.getMessage());
			}
		}
	}

	/**
     * Probe for a Leechy Remote server.
     * @throws IOException 
     */
    private void startServerProbe() {
    	notifyUser("Looking for a server...");
    	stopServerProbe();
        acquireMulticastLock();    
        try {
        	jmdns = JmDNS.create();
        	jmdns.addServiceListener(SERVICE_NAME, this);
        } catch (IOException err) {
            notifyUser("Error while scanning network: " + err.getMessage());
        }    	
    }
    
    private void stopServerProbe() {
    	if (jmdns != null) {
    		jmdns.removeServiceListener(SERVICE_NAME, this);
    		try {
    			jmdns.close();
    			jmdns = null;
    		} catch (IOException err) {
    			notifyUser("Error closing mDNS interface: " + err.getMessage());
    		}
        	releaseMulticastLock();
    	}
    }    
    
    /**
     * Acquire the multicast lock required by Android to scan for mDNS services.
     */
    private void acquireMulticastLock() {
    	WifiManager wifi = (WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
    	multicast_lock = wifi.createMulticastLock("LeechyRemoteLock");
    	multicast_lock.setReferenceCounted(true);
    	multicast_lock.acquire();    	
    }  
    
    private void releaseMulticastLock() {
    	if (multicast_lock != null) {
    		multicast_lock.release();
    		multicast_lock = null;
    	}
    }
    
    /**
     * Shortcut to show a notification.
     */
    private synchronized void notifyUser(final String text) {
    	runOnUiThread(new Runnable() {
    		public void run() {
    	    	Context context = getApplicationContext();
    	    	Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
    	    	toast.show();
    		}
    	});
    	Log.d(TAG, text);
    }
    
    /*************************************************************************
     * ServiceListener implementation.
     */
    
	@Override
    public void serviceResolved(ServiceEvent event) {
		ServiceInfo info = event.getInfo();
		String address = info.getHostAddresses()[0];
		int port = info.getPort();
		current_server = new LeechyRemoteServer(address, port);
        notifyUser("Found server at " + current_server.getAddrString());
        stopServerProbe();
    }    		
    
    @Override
    public void serviceAdded(ServiceEvent event) {
        jmdns.requestServiceInfo(event.getType(), event.getName(), true);
    }
    
	@Override
    public void serviceRemoved(ServiceEvent event) {
        notifyUser("Server lost");
        current_server = null;
    }    
}