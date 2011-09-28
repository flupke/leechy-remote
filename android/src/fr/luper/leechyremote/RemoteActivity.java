package fr.luper.leechyremote;

import java.io.IOException;
import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote);
        try {
        	startServerProbe();
        } catch (IOException err) {
        	notifyUser("Error while scanning network: " + err.getMessage());
        }
    }
    
    @Override
    public void onDestroy() {
    	stopServerProbe();
    	super.onDestroy();
    }
    
    /**
     * Probe for a Leechy Remote server.
     * @throws IOException 
     */
    private void startServerProbe() throws IOException {
    	notifyUser("Looking for a server...");
    	stopServerProbe();
        acquireMulticastLock();      
    	jmdns = JmDNS.create();
    	jmdns.addServiceListener(SERVICE_NAME, this);
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
    			TextView status_label = (TextView) findViewById(R.id.status_label);
    	    	Context context = getApplicationContext();
    	    	Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
    	    	toast.show();
    	    	status_label.setText(text);
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
        notifyUser("Found server at " + address + ":" + port);
        stopServerProbe();
    }    		
    
    @Override
    public void serviceAdded(ServiceEvent event) {
        jmdns.requestServiceInfo(event.getType(), event.getName(), true);
    }
    
	@Override
    public void serviceRemoved(ServiceEvent event) {
        notifyUser("Server lost");
    }    
}