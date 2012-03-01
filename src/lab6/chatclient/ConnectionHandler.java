package lab6.chatclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ConnectionHandler implements Runnable{

	private Socket sock;
	private ConcurrentLinkedQueue<String> incoming;
	private ConcurrentLinkedQueue<String> outgoing;
	private ObjectOutputStream o;
	private SocketListener inputListener;
	private boolean running;
	private Handler post;

	private List<String> connectedUsers;
	
	private static final String USER_LIST_REQUEST = "$gimme!_USERS";
	private static final String SET_USERNAME_REQUEST = "$lemmeset!_USER";
	private static final String LOGOUT_REQUEST = "$lemmeget!_THEFUCK_OUTTAHERE";
	private static final String USER_LIST_FLAG = "$itscoming!_USERS";
	private static final String MESSAGE_KEY = "m";
	
	private static final int PORT = 1234;


	public ConnectionHandler (String ip,Handler h, List<String> conUsers) throws IOException{
			running = true;
			post = h;
			connectedUsers = conUsers;
			incoming = new ConcurrentLinkedQueue<String>();
			outgoing = new ConcurrentLinkedQueue<String>();
			InetAddress host = InetAddress.getByName(ip);
			sock = new Socket(host,PORT);
			o = new ObjectOutputStream(sock.getOutputStream());
			ObjectInputStream input = new ObjectInputStream(sock.getInputStream());
			inputListener = new SocketListener(input);
			new Thread(inputListener).start();

		
	}
	public void closeConn() throws IOException{
		inputListener.stopListening();
		running = false;
			sock.close();
	}
	@Override
	public void run(){
		while(running){
			while(!incoming.isEmpty()){
				String msg = incoming.poll();
				if(msg.equals(USER_LIST_FLAG)){
					getUserList();
				}else{
					Message m = new Message();
					Bundle b = new Bundle();
					b.putString(MESSAGE_KEY, msg);
					m.setData(b);
					post.sendMessage(m);
					
				}
			}
			while(!outgoing.isEmpty()){
				try {
					o.writeObject(outgoing.poll());
				} catch (IOException e) {
					//Do nothing
				}
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				//Do nothing
			}
		}

	}
	
	private void getUserList(){
		int numClients = 0;
		
		try{
			numClients = Integer.parseInt(incoming.poll());
		}catch(NumberFormatException e){
			
		}
		connectedUsers.clear();
		for(int i=0;i<numClients;i++){
			String userName = incoming.poll();
			connectedUsers.add(userName);
		}
	}
	private class SocketListener implements Runnable{
		ObjectInputStream in;
		boolean running;

		public SocketListener(ObjectInputStream i){
			in = i;
			running = true;
		}
		@Override
		public void run() {
			while(running){
				try {
					String msg = (String) in.readObject();
					if(msg != null)
					incoming.add(msg);
				} catch (IOException e) {
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		public void stopListening(){
			running = false;
		}

	}

	public void sendMsg(String m){
		outgoing.add(m);
	}

}
