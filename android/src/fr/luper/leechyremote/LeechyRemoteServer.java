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
	private Socket socket;
	PrintWriter out;
	
	LeechyRemoteServer(String init_address, int init_port) {
		address = init_address;
		port = init_port;
	}
	
	/**
	 * Send an action to the server.
	 * 
	 * @param name
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void sendAction(String name) throws UnknownHostException, IOException {
		if (socket == null) {
			connect();
		}
		Log.d(TAG, "Sending action to server: " + name);
		
		out.println("SMPLAYER ACTION " + name);	
		if (out.checkError()) {
			// Try to reconnect
			connect();
			out.println("SMPLAYER ACTION " + name);			
		}
	}
	
	/**
	 * Establish the connection to the server.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void connect() throws UnknownHostException, IOException {		
		socket = new Socket(address, port);
		socket.setKeepAlive(true);
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	/**
	 * Get a textual representation of the server address.
	 */
	public String getAddrString() {
		return address + ":" + port;
	}
	
	/*************************************************************************
	 * Accessors
	 */

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
