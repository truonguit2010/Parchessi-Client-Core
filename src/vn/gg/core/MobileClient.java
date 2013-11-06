package vn.gg.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Vector;

import com.unity3d.player.UnityPlayer;

/**
 * 
 * @author truongps
 *
 */
public class MobileClient {
	
	final static String TAG = "Android Mobile Client";
	// protected IMessageListener messageHandler;
	/**
	 * Ten object de send message qua.
	 */
	private String messageHandler;
	private DataOutputStream dos;
	private DataInputStream dis;
	private Sender sender;
	private Reader reader;
	public String currentIp;
	public int currentPort;
	private Socket socket;
	
	/**
	 * 
	 * @param messageHandler
	 */
	public void setMessageHandler(String messageHandler) {
		this.messageHandler = messageHandler;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return (this.socket != null) && (this.socket.isConnected());
	}

	/**
	 * 
	 * @param ip
	 * @param port
	 */
	public void connect(final String ip, final int port) {
		new Thread(new Runnable() {
			public void run() {
				try {
					Log.log(TAG, "Connecting to ip: " + ip + " - port: " + port);
					doConnect(ip, port);
					Log.log(TAG, "Connected to ip: " + ip + " - port: " + port);
					// Connect OK
					if (messageHandler != null) {
						UnityPlayer.UnitySendMessage(messageHandler, "onConnected", "");
					}
				} catch (Exception localException) {
					// Connect Fail
					if (messageHandler != null) {
						UnityPlayer.UnitySendMessage(messageHandler, "onConnectFail", "");
					}
				}
			}
		}).start();
	}

	private void doConnect(String ip, int port) throws IOException {
		socket = new java.net.Socket();
		//applyHints(hints);  // better to call BEFORE socket is connected!
		
		// and connect...
		InetSocketAddress address = new InetSocketAddress(ip, port);
		socket.connect(address);
		
		this.dis = new DataInputStream(socket.getInputStream());
		this.dos = new DataOutputStream(socket.getOutputStream());
		
		this.sender = new Sender();
		this.reader = new Reader();
		new Thread(this.sender).start();
		new Thread(this.reader).start();
		this.currentIp = ip;
		this.currentPort = port;
	}

	public void sendMessage(String jsonMessage) {
		this.sender.addMessage(jsonMessage);
	}

	private synchronized void doSendMessage(String jsonMessage)
			throws IOException {
		byte[] arrayOfByte = jsonMessage.getBytes("UTF-8");
		dos.writeInt(arrayOfByte.length);
		dos.write(arrayOfByte);
		this.dos.flush();
	}

	public void close() {
		try {
			this.currentIp = null;
			this.currentPort = -1;
			if (this.sender != null)
				this.sender.stop();
			if (this.dos != null) {
				this.dos.close();
				this.dos = null;
			}
			if (this.dis != null) {
				this.dis.close();
				this.dis = null;
			}
			if (this.socket != null) {
				this.socket.close();
				this.socket = null;
			}
			this.sender = null;
			this.reader = null;
		} catch (Exception localException) {
		}
	}

	private class Reader implements Runnable {
		private Reader() {
		}

		public void run() {
			try {
				while (MobileClient.this.isConnected()) {
					final String localMessage = readMessage();
					if (localMessage == null)
						break;
					// On message
					if (messageHandler != null) {
						UnityPlayer.UnitySendMessage(messageHandler, "onMessage", localMessage);
					}
				}
			} catch (Exception localException) {
				// On disconnect
				if (messageHandler != null) {
					UnityPlayer.UnitySendMessage(messageHandler, "onDisconnect", "");
				}
			}
		}

		private String readMessage() throws Exception {
			int messageLeght = dis.readInt();
			if (messageLeght <= 0)
				return null;
			//messageLeght--;
			byte[] arrayOfByte = new byte[messageLeght];
			int k = 0;
			int m = 0;
			while ((k != -1) && (m < messageLeght)) {
				k = dis.read(arrayOfByte, m, messageLeght - m);
				if (k > 0)
					m += k;
			}
			if (messageLeght == 0)
				return null;
			
			return new String(arrayOfByte, "UTF-8");
		}
	}

	private class Sender implements Runnable {
		private final Vector<String> sendingMessage = new Vector<String>();

		private Sender() {
		}

		public void addMessage(String jsonMessage) {
			synchronized (this.sendingMessage) {
				this.sendingMessage.addElement(jsonMessage);
				this.sendingMessage.notifyAll();
			}
		}

		public void run() {
			try {
				while (MobileClient.this.isConnected())
					synchronized (this.sendingMessage) {
						while (this.sendingMessage.size() > 0)
							if (MobileClient.this.isConnected()) {
								String localMessage = this.sendingMessage
										.elementAt(0);
								this.sendingMessage.removeElementAt(0);
								MobileClient.this.doSendMessage(localMessage);
							}
						try {
							this.sendingMessage.wait();
						} catch (InterruptedException localInterruptedException) {
						}
					}
			} catch (IOException localIOException) {
			}
		}

		public void stop() {
			synchronized (this.sendingMessage) {
				this.sendingMessage.removeAllElements();
				this.sendingMessage.notifyAll();
			}
		}
	}
}
