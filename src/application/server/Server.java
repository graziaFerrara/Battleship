package application.server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import application.Communicator;
import application.message.Message;
import application.table.Player;
import application.table.Table;

public class Server extends Communicator implements Runnable {

	private static DatagramSocket sock;

	private static Map<String, Player> users = new HashMap<>(); // holds the mapping between the username and the player
																// with that username
	private static Map<Player, Table> tables = new HashMap<>(); // holds the mapping between the player and the table it
																// belongs to
	
	private static String toHandle = null; // phase of the interaction with the client to be handled by the server

	private Player waitingUser = null; // user in the waiting room

	private Player player1 = null; // player1 in the table the server is currently managing
	private Player player2 = null; // player2 in the table the server is currently managing
	private Table table = null; // table which the server is currently managing
	private String toDo = null; // action to be performed while moving
	private int row = -1;
	private char col = ' '; // row and col to be used by the thread when managing the move to be done

	/**
	 * Creates the server.
	 * 
	 * @param port
	 * @throws IOException
	 */
	public Server(int port) throws IOException {
		sock = new DatagramSocket(port);
	}

	/**
	 * Creates an istance of the server which manages the table with the user 1 and
	 * the user 2.
	 * 
	 * @param player1
	 * @param player2
	 * @param table
	 */
	public Server(Player player1, Player player2, Table table) {
		this.player1 = player1;
		this.player2 = player2;
		this.table = table;
	}

	/**
	 * Creates an istance of the server which manages the table with the user 1 and
	 * the user 2 and has a specific action to perform.
	 * 
	 * @param player1
	 * @param player2
	 * @param table
	 * @param toDo
	 */
	public Server(Player player1, Player player2, Table table, String toDo, int row, char col) {
		this.player1 = player1;
		this.player2 = player2;
		this.table = table;
		this.toDo = toDo;
		this.row = row;
		this.col = col;
	}

	/**
	 * Method to be called by the server in a infinite loop, which only receives
	 * messages.
	 * 
	 * @throws IOException
	 */
	public void acceptMessage() throws IOException {

		Message message = receiveMessage(sock);
		String[] splittedMessage = message.getText().trim().split(" ");

		if (splittedMessage[0].equals("username")) {

			toHandle = "username";
			handleConnection(splittedMessage, message);

		} else if (splittedMessage[0].equals("placement")) {

			toHandle = "placement";
			handlePlacement(splittedMessage);

		} else if (splittedMessage[0].equals("play")) {

			toHandle = "play";
			handleMove(splittedMessage);

		} else if (splittedMessage[0].equals("disconnect")) {

			handleDisconnection(splittedMessage);

		}

	}

	/**
	 * If the username is already in use, the connection is refused and a message
	 * the client is sent to the client to warn him. Otherwise, if the username is
	 * valid, if there's already a user in the waiting room, a game table is
	 * created, otherwise the user is put in the waiting room.
	 * 
	 * @param splittedMessage
	 * @param usernameMessage
	 * @throws IOException
	 */
	private void handleConnection(String[] splittedMessage, Message usernameMessage) throws IOException {

		String username = splittedMessage[1];

		InetAddress IPAddress = usernameMessage.getAddress();
		int port = usernameMessage.getPort();

		if (!users.containsKey(username)) {

			// valid username
			Player player = new Player(username, IPAddress, port);
			users.put(username, player);

			sendMessage(sock, "ok", IPAddress, port);
			System.out.println(" - User " + username + " correctly logged in.");

			if (waitingUser == null) {

				// no user in the waiting room
				waitingUser = player;
				System.out.println(" - User " + username + " in waiting room.");

			} else {

				// already a user in the waiting room
				Table newTable = new Table(waitingUser, player);
				tables.put(waitingUser, newTable);
				tables.put(player, newTable);
				System.out.println(" - Game table created for " + username + " and " + waitingUser.getUsername());

				// create a new thread to manage the table
				Server s = new Server(player, waitingUser, newTable);
				waitingUser = null;
				Thread t = new Thread(s);
				t.start();
			}

		} else {
			// not valid username
			sendMessage(sock, "connection refused", IPAddress, port);
			System.out.println(" - ERROR: " + username + " is not valid. Connection refused.");
		}
	}

	/**
	 * Places the ship into the game board and starts a thread to communicate with
	 * the client.
	 * 
	 * @param splittedMessage
	 */
	private void handlePlacement(String[] splittedMessage) {

		Player sender = users.get(splittedMessage[1]);

		Table senderTable = tables.get(sender);
		Player other = senderTable.getOther(sender);

		// place the ship in the placement matrix
		senderTable.getGameBoard(sender).placeShip(splittedMessage[2], Integer.parseInt(splittedMessage[3]),
				splittedMessage[4].charAt(0), Integer.parseInt(splittedMessage[5]), splittedMessage[6]);

		// create a new thread to manage the placement
		Server s = new Server(sender, other, senderTable);
		Thread t = new Thread(s);
		t.start();

	}

	/**
	 * Processes the move made by the user and creates a thread to give him
	 * back a feedbcak about it.
	 * 
	 * @param splittedMessage
	 */
	private void handleMove(String[] splittedMessage) {

		Player sender = users.get(splittedMessage[1]);
		Table senderTable = tables.get(sender);
		Player other = senderTable.getOther(sender);

		char col = splittedMessage[2].charAt(0);
		int row = Integer.parseInt(splittedMessage[3]);
		String toDo = senderTable.getGameBoard(other).makeMove(col, row);

		// create a new thread to manage the move
		Server s = new Server(sender, other, senderTable, toDo, row, col);
		Thread t = new Thread(s);
		t.start();

	}

	/**
	 * This method handles the disconnection of the user.
	 * @param splittedMessage
	 */
	private void handleDisconnection(String[] splittedMessage) {

		Player sender = users.get(splittedMessage[1]);
		users.remove(sender.getUsername());
		tables.remove(sender);

		System.out.println("Still active users:");
		for (String p : users.keySet())
			System.out.println(p);

	}

	/**
	 * The thread only sends messages to the user, which are different depending on the
	 * context.
	 */
	@Override
	public void run() {

		if (toHandle.equals("username")) {

			try {
				sendMessage(sock, "place", player1.getIPaddress(), player1.getPort());
				sendMessage(sock, "place", player2.getIPaddress(), player2.getPort());
			} catch (IOException e) {
				// error sending message
			}

		} else if (toHandle.equals("placement")) {

			try {

				if (!table.getGameBoard(player1).isPlacementCompleted()
						|| !table.getGameBoard(player2).isPlacementCompleted())
					// player 1 is the sender
					sendMessage(sock, "ok", player1.getIPaddress(), player1.getPort());
				else {
					sendMessage(sock, "ok", player1.getIPaddress(), player1.getPort());
					sendMessage(sock, "completed " + table.getGameBoard(player2).getTotHits(), player1.getIPaddress(),
							player1.getPort());
					sendMessage(sock, "completed " + table.getGameBoard(player1).getTotHits(), player2.getIPaddress(),
							player2.getPort());
					sendMessage(sock, "yourturn ", player1.getIPaddress(), player1.getPort());
					sendMessage(sock, "wait ", player2.getIPaddress(), player2.getPort());
					System.out.println("completed");
				}

			} catch (IOException e) {
				// error sending message
			}

		} else if (toHandle.equals("play")) {

			try {

				if (toDo.equals("hit")) {
					// hit message to the sender
					sendMessage(sock, "hit", player1.getIPaddress(), player1.getPort());

					if (table.getGameBoard(player2).isLoser())
						sendMessage(sock, "loser", player2.getIPaddress(), player2.getPort());
					else
						sendMessage(sock, "move " + col + ' ' + row, player2.getIPaddress(), player2.getPort());

				} else if (toDo.equals("miss")) {
					// miss message to the sender
					sendMessage(sock, "miss", player1.getIPaddress(), player1.getPort());
					sendMessage(sock, "yourturn", player2.getIPaddress(), player2.getPort());

				} else {
					// invalid message to the sender
					sendMessage(sock, "invalid", player1.getIPaddress(), player1.getPort());
				}
			} catch (IOException e) {
				// error sending message
			}
			
		} 

	}

	public static void main(String args[]) {
		try {
			Server s = new Server(33333);
			while (true)
				s.acceptMessage();
		} catch (IOException e) {
			System.out.println("Cannot create the server socket");
		}
	}

}
