package fr.luper.leechyremote;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * Interactions with Leechy Remote servers.
 */
public class LeechyRemoteServer {
	private static final String TAG = "LeechyRemote";
	
	private String address;
	private int port;
	
	LeechyRemoteServer(String init_address, int init_port) {
		address = init_address;
		port = init_port;
	}
	
	public void sendAction(String name) throws UnknownHostException, IOException {
		Socket socket = new Socket(address, port);
		PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		Log.d(TAG, "Sending action to server:" + name);
		out.println("SMPLAYER ACTION " + name);
	}
}
