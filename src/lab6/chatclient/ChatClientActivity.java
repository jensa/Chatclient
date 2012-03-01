package lab6.chatclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ChatClientActivity extends Activity {
	
	private ConnectionHandler conn;
	private EditText inputField;
	private boolean loggedIn;
	private List<String> connectedUsers;
	private String username;

	private Handler post;
	
	private static final String USER_LIST_REQUEST = "$gimme!_USERS";
	private static final String SET_USERNAME_REQUEST = "$lemmeset!_USER";
	private static final String LOGOUT_REQUEST = "$lemmeget!_THEFUCK_OUTTAHERE";
	private static final String MESSAGE_KEY = "m";
	private static final String DEFAULT_IP="192.168.99.102";
	private static final String SIMPLE_IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";
	private static final String SETNAME_COMMAND = "setname";
	private static final String DEFAULT_USERNAME = "Anonymous";
	private static final int MAX_CHAT_LENGTH = 200;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		username = DEFAULT_USERNAME;
		post = new Handler(){
			@Override
			public void handleMessage(Message m){
				printToChat(m.getData().getString(MESSAGE_KEY));
			}
		};
		loggedIn = false;
		connectedUsers = new ArrayList<String>();
		setupElements();
	}
	private void setupElements(){

		Button logoutButton = (Button) findViewById(R.id.logout);
		logoutButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				Button thisButton = (Button) findViewById(R.id.logout);
				if(loggedIn){
					thisButton.setText("Log in");
					doLogout();
				} else{
					thisButton.setText("Log out");
					String ip = inputField.getText().toString();
					if(ip.equals("")){
						doLogin();
					} else{
						doLogin(ip);
					}
					conn.sendMsg(USER_LIST_REQUEST);
				}
			}

		});

		Button sendButton = (Button) findViewById(R.id.send);
		sendButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				if(loggedIn){
					String msg = inputField.getText().toString();
					if(msg.startsWith("/")){
						String[] command = msg.substring(1).split(" ");
						if(command[0] != null && command.length>1 && command[0].equals(SETNAME_COMMAND)){
							conn.sendMsg(SET_USERNAME_REQUEST);
							conn.sendMsg(command[1]);
							printToChat("Your username is now: "+command[1]);
							username = command[1];
						}else{
							p("Invalid command");
						}
					}else{
					conn.sendMsg(username+">"+msg);
					}
				} else{
					p("Not logged in!");
				}
				inputField.setText("");
			}

		});
		Button showUsersButton = (Button) findViewById(R.id.showUs);
		showUsersButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				conn.sendMsg(USER_LIST_REQUEST);
				StringBuilder s = new StringBuilder(connectedUsers.size()*30);
				s.append("-----------------\nCurrently connected users:\n");
				for(String user : connectedUsers){
					s.append(user+"\n");
				}
				s.append("-----------------");
				printToChat(s.toString());
				updateStatus(connectedUsers.size());
			}

		});
		inputField = (EditText) findViewById(R.id.inputField);

	}
	
	private void doLogin(){
		doLogin(DEFAULT_IP);
	}
	private void doLogin(String ip){
		try {
			conn = new ConnectionHandler(ip,post,connectedUsers);
			new Thread(conn).start();
		} catch (IOException e) {
			err(e);
		}
		loggedIn = true;
		p("Logged in!");
		
	}
	
	private void doLogout(){
		conn.sendMsg(LOGOUT_REQUEST);
		try {
			conn.closeConn();
		} catch (IOException e) {
			err(e);
		}
		p("Logged out!");
		loggedIn = false;
		conn = null;
	}
	
	private void printToChat(String line){
		TextView chatWindow = (TextView) findViewById(R.id.textWindow);
		String chatText = chatWindow.getText().toString();
		if(chatText.length()>MAX_CHAT_LENGTH){
			chatText = chatText.substring(MAX_CHAT_LENGTH/2);
		}
		chatText = chatText.concat(line+"\n");
		chatWindow.setText(chatText);
	}
	private void updateStatus(int numUsers){
		TextView status = (TextView) findViewById(R.id.statusbar);
		status.setText("Users: "+numUsers);
	}
	public void err(Throwable e){
		Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
	}
	public void p(String m){
		Toast.makeText(this, m, Toast.LENGTH_LONG);
	}
}