package application.client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

import application.Communicator;
import application.message.Message;
import javafx.beans.property.BooleanProperty;

public class Client extends Communicator implements Runnable{
	
	private static DatagramSocket sock = null;
	private static InetAddress IPAddress = null;
	private static int port = -1;
	
	private static String username = null;

	private static BooleanProperty waiting;
	private static BooleanProperty placing; 
	private static BooleanProperty playing;
	
	private static int hits = 0;
	private static int totHits = 0;
	
	private boolean finished = false;
	
	/**
	 * Constructor used by the thread.
	 */
	public Client() {
		
	}
	
	/**
	 * Constructor used by the main.
	 * @param waiting
	 * @param placing
	 * @param playing
	 */
	public Client(BooleanProperty waiting, BooleanProperty placing, BooleanProperty playing) {
		this();
		Client.waiting = waiting;
		Client.placing = placing;
		Client.playing = playing;
	}

	/**
	 * Sends a connection request to the server and waits for a response. If the connection
	 * goes fine, a thread is created to manage the communication.
	 * @param IPAddress
	 * @param port
	 * @param username
	 * @return
	 * @throws IOException
	 */
	public boolean connect(String IPAddress, int port, String username) throws IOException {
		
		Client.sock = new DatagramSocket();
		Client.IPAddress = InetAddress.getByName(IPAddress);
		Client.port = port;
		Client.username = username;
		
		// send connection request
		sendMessage(sock, "username " + Client.username, Client.IPAddress, Client.port);
		
		// receive response
		Message response = receiveMessage(sock);
		
		if (response.getText().toLowerCase().trim().equals("ok")) {
			
			// connection accepted
			System.out.println("connected");
			
			Client c = new Client();
			Thread t = new Thread(c);
			t.start();
			return true;
			
		}else{
			
			// connection refused
			System.out.println("connectection refused");
			return false;
			
		}
		
	}
	
	/**
	 * Disconnects the user from the server.
	 */
	public void disconnect() {
		try {
			sendMessage(sock, "disconnect " + username, IPAddress, port);
		} catch (IOException e) {
			// error during send
		}
		sock.close();
		reinit();
	}
	
	/**
	 * Sends to the server the name of the ship to be placed, its length and its coordinates and orientation
	 * and waits for a feedback.
	 * 
	 * @param name
	 * @param startRow
	 * @param startCol
	 * @param length
	 * @param orientation
	 * @return
	 */
	public boolean placeShip(String name, int startRow, char startCol, int length, String orientation) {
		
		String message = "placement " + username + ' ' + name + ' ' + startRow + ' ' + startCol + ' ' + length + ' ' + orientation;
		
		try {
			sendMessage(sock, message, IPAddress, port);
			Message receivedMessage = receiveMessage(sock);
			if (receivedMessage.getText().trim().equals("ok")) return true;
		} catch (IOException e) {
			System.out.println("Error sending the message");
			return false;
		}
		
		return false;
		
	}
	
	/**
	 * Sends to the server the move to be done and waits for a feedback, 
	 * then the feedback is returned as a string. This is usefult to 
	 * understand the action to be performed in the GUI.
	 * 
	 * @param col
	 * @param row
	 * @return
	 */
	public String sendMove(char col, int row) {
	
		String message = "play " + username + ' ' + col + ' ' + row;
		
		try {
			sendMessage(sock, message, IPAddress, port);
			Message receivedMessage = receiveMessage(sock);
			
			if (receivedMessage.getText().trim().equals("miss")) {
				System.out.println("miss");
				return "miss";
			} else if (receivedMessage.getText().trim().equals("hit")) {
				hits++;
				System.out.println("hit");
				return "hit";
			} else if (receivedMessage.getText().trim().equals("invalid")) {
				return "invalid";
			} 
			
		} catch (IOException e) {
			System.out.println("Error sending the message");
			return "error";
		}
		
		return "error";
		
	}

	/**
	 * The thread, depending on the gaming phase, performs a different action.
	 */
	@Override
	public void run() {
		
		while (waiting.getValue()) {
			try {
				Message message = receiveMessage(sock);
				if(message.getText().toLowerCase().trim().equals("place")) {
					waiting.setValue(false);
					placing.setValue(true);
					System.out.println("Start placing...");
				}
			} catch (IOException e) {
				// error while receiving placing message
			}
		}
		
		while (placing.getValue()) { }
		
		try {
			Message message = receiveMessage(sock);
			String [] splittedMessage = message.getText().toLowerCase().trim().split(" ");
			if(splittedMessage[0].equals("completed")) {
				totHits = Integer.parseInt(splittedMessage[1]);
				playing.setValue(true);
				System.out.println("Start playing...");
			}
		} catch (IOException e) {
			// error while receiving playing message
		}
		
		boolean jump = false; 
		
		while (playing.getValue())
			
			if (!finished) {
				if (checkHits()) playing.setValue(false);
			} else {
				System.out.println("Game Over!");
				jump = true;
				break;
			}
			
		if (!finished && !jump) {
			finished = true;
		}
		
	}
	
	/**
	 * This method is called by the Task started from the GUI to
	 * receive a new message.
	 * @return
	 */
	public String receiveMove() {
		try {
			Message message = receiveMessage(sock);
			return message.getText();
		} catch (IOException e) {
			// error while receiving playing message
		}
		return "";
	}
	
	/**
	 * Checks if the number of hits is equal to the total one, 
	 * if it is, return true, else false.
	 * @return
	 */
	private boolean checkHits() {
		return (hits == totHits);
	}

	private void reinit() {
		username = null;
		hits = 0;
		totHits = 0;
		finished = false;
	}
		
}
