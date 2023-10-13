package application.client;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import javafx.scene.control.ToggleGroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;

import javafx.scene.control.MenuItem;

import javafx.scene.control.Label;

import javafx.scene.control.RadioButton;

import javafx.scene.input.MouseEvent;

import javafx.scene.shape.Rectangle;

import javafx.scene.layout.BorderPane;

import javafx.scene.layout.GridPane;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class ClientDocumentController {
	@FXML
	private BorderPane loginPane;
	@FXML
	private TextField addressField;
	@FXML
	private TextField portField;
	@FXML
	private TextField usernameField;
	@FXML
	private Button loginButton;
	@FXML
	private BorderPane gamePane;
	@FXML
	private MenuItem disconnectItem;
	@FXML
	private Label waitingLabel;
	@FXML
	private Label selectShipLabel;
	@FXML
	private HBox placingBox;
	@FXML
	private Rectangle ship5;
	@FXML
	private RadioButton aircraftCarrier;
	@FXML
	private ToggleGroup ship;
	@FXML
	private Rectangle ship4;
	@FXML
	private RadioButton battleShip;
	@FXML
	private Rectangle ship3bis;
	@FXML
	private RadioButton destroyer;
	@FXML
	private Rectangle ship3;
	@FXML
	private RadioButton submarine;
	@FXML
	private Rectangle ship2;
	@FXML
	private RadioButton patrolBoat;
	@FXML
	private TextField rowField;
	@FXML
	private TextField columnField;
	@FXML
	private RadioButton horizontal;
	@FXML
	private ToggleGroup orientation;
	@FXML
	private RadioButton vertical;
	@FXML
	private Button placeShipButton;
	@FXML
	private GridPane gridUser;
	@FXML
	private Label userLabel;
	@FXML
	private GridPane gridAdversary;

	private Client client;
	private Alert alert = new Alert(AlertType.NONE);

	private BooleanProperty waiting = new SimpleBooleanProperty(), placing = new SimpleBooleanProperty(),
			playing = new SimpleBooleanProperty(), makeMove = new SimpleBooleanProperty();
	
	ChangeListener<Boolean> placingListener, playingListener, makeMoveListener;

	private Map<RadioButton, Integer> shipMap;
	private Map<RadioButton, Paint> colorMap;

	private int placed = 0;

	/**
	 * Init the GUI.
	 */
	@FXML
	public void initialize() {

		createShipMap();
		createColorMap();
		initProperties();

		client = new Client(waiting, placing, playing);

		addListeners();

	}

	@FXML
	public void connect(ActionEvent event) {

		String IPaddress = addressField.getText(), port = portField.getText(), username = usernameField.getText();

		if (checkLoginFields(IPaddress, port, username)) {
			try {
				if (client.connect(IPaddress, Integer.parseInt(port), username)) {
					// connection ok
					switchConnectionPlacing();
				} else {
					// not valid username
					showAlert("Connection failed, retry with another username!", AlertType.ERROR);
					usernameField.clear();
					System.out.println("Connection failed. Not valid username.");
				}
			} catch (IOException e) {
				// connection not ok
				showAlert("Connection failed, retry!", AlertType.ERROR);
				clearLoginFields();
				System.out.println("Connection error.");
			}
		} else {
			// not valid fields
			showAlert("Not valid fields, retry!", AlertType.ERROR);
			clearLoginFields();
			System.out.println("Invalid fields.");
		}
	}

	@FXML
	public void placeShip(ActionEvent event) {

		String row = rowField.getText(), col = columnField.getText();

		if (placing.getValue()) {

			RadioButton selectedShip = (RadioButton) ship.getSelectedToggle();
			RadioButton selectedOrientation = (RadioButton) orientation.getSelectedToggle();

			if (checkPlacementFields(selectedShip, row, col, selectedOrientation)) {

				if (client.placeShip(selectedShip.getText(), Integer.parseInt(row), col.charAt(0),
						shipMap.get(selectedShip), selectedOrientation.getText().toLowerCase())) {
					colorCells(selectedOrientation.getText().toLowerCase(), Integer.parseInt(row), col.charAt(0),
							shipMap.get(selectedShip), colorMap.get(selectedShip));
					selectedShip.getParent().setVisible(false);
					selectedShip.getParent().setManaged(false);
					ship.getToggles().remove(selectedShip);
					placed++;
				}

				if (placed >= 5) {
					switchPlacingWaiting();
				}

			} else {
				// not valid fields, retry
				showAlert("Not valid fields, retry!", AlertType.ERROR);
				System.out.println("Invalid fields.");
			}

		} else {

			if (checkMoveFields(row, col)) {

				// send moves
				String ans = client.sendMove(col.charAt(0), Integer.parseInt(row));

				int colIndex = getIndexFromCharCol(col.charAt(0)), rowIndex = getIndexFromIntRow(Integer.parseInt(row));

				Pane pane = new Pane();

				if (ans.equals("hit")) {
					// HIT
					pane.setStyle("-fx-background-color: red;");
					gridAdversary.add(pane, colIndex, rowIndex);

				} else if (ans.equals("miss")) {
					// MISS
					pane.setStyle("-fx-background-color: blue;");
					gridAdversary.add(pane, colIndex, rowIndex);
					switchPlayingWaiting();

				} else if (ans.equals("invalid")) {
					// INVALID MOVE
					showAlert("You cannot do this move, retry!", AlertType.ERROR);
				}

			}

		}

	}

	@FXML
	public void disconnect(ActionEvent event) {
		disconnectUser();
	}

	private void disconnectUser() {
		
		removeListeners();

		client.disconnect();
		
		switchDisconnectConnect();

		waiting = new SimpleBooleanProperty();
		placing = new SimpleBooleanProperty();
		playing = new SimpleBooleanProperty();
		initProperties();
		client = new Client(waiting, placing, playing);
		addListeners();
	}

	private void removeListeners() {
		placing.removeListener(placingListener);
		playing.removeListener(playingListener);
		makeMove.removeListener(makeMoveListener);
	}

	private boolean checkLoginFields(String IPaddress, String port, String username) {
		if (!IPaddress.isEmpty() && !port.isEmpty() && !username.isEmpty())
			try {
				Integer.parseInt(port);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		return false;
	}

	private boolean checkPlacementFields(RadioButton selectedShip, String row, String col,
			RadioButton selectedOrientation) {

		if (selectedShip != null && !row.isEmpty() && !col.isEmpty() && selectedOrientation != null) {

			if (row.length() > 1 || col.length() > 1)
				return false;

			try {

				int rowNumber = Integer.parseInt(row);
				if (rowNumber < 0 || rowNumber > 9)
					return false;

				char colLetter = col.charAt(0);
				if (colLetter < 'a' || colLetter > 'j')
					return false;

				int length = shipMap.get(selectedShip);
				String orientation = selectedOrientation.getText().toLowerCase();

				// TODO controlla questa condizione
				/*
				 * if (orientation.equals("horizontal")) if ((colLetter + length) > 'j') return
				 * false; else if (orientation.equals("vertical")) if ((rowNumber + length) > 9)
				 * return false;
				 */

				return true;

			} catch (NumberFormatException e) {
				return false;
			}

		}

		return false;
	}

	private boolean checkMoveFields(String row, String col) {

		if (row.length() < 1 || col.length() < 1)
			return false;

		try {

			int rowNumber = Integer.parseInt(row);
			if (rowNumber < 0 || rowNumber > 9)
				return false;

			char colLetter = col.charAt(0);
			if (colLetter < 'a' || colLetter > 'j')
				return false;

			return true;

		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void colorCells(String orientation, int row, char col, int length, Paint color) {

		int numRows = gridUser.getRowCount();
		int colIndex = col - 'a' + 1;

		if (orientation.equals("horizontal")) {

			for (int i = 0; i < length; i++) {
				Pane pane = new Pane();
				pane.setStyle("-fx-background-color: " + colorToHex(color) + ";");
				gridUser.add(pane, colIndex + i, numRows - row - 1);
			}

		} else if (orientation.equals("vertical")) {

			for (int i = 0; i < length; i++) {
				Pane pane = new Pane();
				pane.setStyle("-fx-background-color: " + colorToHex(color) + ";");
				gridUser.add(pane, colIndex, numRows - row - 1 + i);
			}

		}

	}

	private String colorToHex(Paint color) {
		return String.format("#%02X%02X%02X", (int) (((Color) color).getRed() * 255),
				(int) (((Color) color).getGreen() * 255), (int) (((Color) color).getBlue() * 255));
	}

	private void switchConnectionPlacing() {
		loginPane.setVisible(false);
		placingBox.setVisible(false);
		placingBox.setManaged(false);
		userLabel.setVisible(false);
		gamePane.setVisible(true);
		gridAdversary.setVisible(false);
		gridAdversary.setManaged(false);
	}

	private void showAlert(String text, AlertType alertType) {
		alert.setContentText(text);
		alert.setHeaderText(null);
		alert.setGraphic(null);
		alert.setAlertType(alertType);
		alert.showAndWait();
	}

	private void clearLoginFields() {
		addressField.clear();
		portField.clear();
		usernameField.clear();
	}

	private void switchConnectPlacing() {
		placingBox.setVisible(true);
		placingBox.setManaged(true);
		waitingLabel.setVisible(false);
	}

	private void switchPlacingWaiting() {
		userLabel.setVisible(true);
		placingBox.setVisible(false);
		placingBox.setManaged(false);
		gridAdversary.setVisible(true);
		gridAdversary.setManaged(true);
		waitingLabel.setText("Waiting for the adversary to finish placing...");
		waitingLabel.setVisible(true);
		placing.setValue(false);
	}

	private void switchPlacingPlaying() {
		waitingLabel.setText("Make your move!");
		gridAdversary.setDisable(false);
		placingBox.setVisible(true);
		placingBox.setManaged(true);
		horizontal.setManaged(false);
		vertical.setManaged(false);
		horizontal.setVisible(false);
		vertical.setVisible(false);
		selectShipLabel.setVisible(false);
		placeShipButton.setText("Send Move");
		rowField.clear();
		columnField.clear();
	}

	private void switchWaitingPlaying() {
		enableMoveFields();
		waitingLabel.setText("Make your move!");
	}

	private void switchPlayingWaiting() {
		waitingLabel.setText("Waiting for the adversary to make a move...");
		makeMove.setValue(false);
		disableMoveFields();
	}

	private void switchDisconnectConnect() {
		waitingLabel.setVisible(true);
		ObservableList <Node> childrenUser = gridUser.getChildren(), 
				childrenAdversary = gridAdversary.getChildren();
		List<Node> nodesToRemove1 = new ArrayList<>(), nodesToRemove2 = new ArrayList<>();
		
		for (Node n : childrenUser) {
			if (n instanceof Pane)
				nodesToRemove1.add(n);
		}
		
		for (Node n : childrenAdversary) {
			if (n instanceof Pane)
				nodesToRemove2.add(n);
		}
		
		gridUser.getChildren().removeAll(nodesToRemove1);
		gridAdversary.getChildren().removeAll(nodesToRemove2);
		
		gridAdversary.setVisible(false);
		gridUser.setVisible(true);
		waitingLabel.setText("Waiting for a player...");
		waitingLabel.setVisible(true);
		gamePane.setVisible(false);
		clearLoginFields();
		loginPane.setVisible(true);
		ship2.setVisible(true);
		ship2.setManaged(true);
		ship3.setVisible(true);
		ship3.setManaged(true);
		ship3bis.setVisible(true);
		ship3bis.setManaged(true);
		ship4.setVisible(true);
		ship4.setManaged(true);
		ship5.setVisible(true);
		ship5.setManaged(true);
	}

	private void disableMoveFields() {
		rowField.setDisable(true);
		columnField.setDisable(true);
		placeShipButton.setDisable(true);
	}

	private void enableMoveFields() {
		rowField.setDisable(false);
		columnField.setDisable(false);
		placeShipButton.setDisable(false);
	}

	private void initProperties() {
		waiting.setValue(true);
		placing.setValue(false);
		playing.setValue(false);
		makeMove.setValue(false);
	}

	private void createColorMap() {
		colorMap = new HashMap<>();
		colorMap.put(aircraftCarrier, ship5.getFill());
		colorMap.put(battleShip, ship4.getFill());
		colorMap.put(destroyer, ship3bis.getFill());
		colorMap.put(submarine, ship3.getFill());
		colorMap.put(patrolBoat, ship2.getFill());
	}

	private void createShipMap() {
		shipMap = new HashMap<>();
		shipMap.put(aircraftCarrier, 5);
		shipMap.put(battleShip, 4);
		shipMap.put(destroyer, 3);
		shipMap.put(submarine, 3);
		shipMap.put(patrolBoat, 2);
	}

	private void addListeners() {

		placingListener = (observable, oldValue, newValue) -> {
		    if (newValue)
		        switchConnectPlacing();
		};
		
		placing.addListener(placingListener);
		
		playingListener = (observable, oldValue, newValue) -> {

			if (newValue) {

				Platform.runLater(() -> {
					switchPlacingPlaying();
				});

				makeMove.setValue(true);

				Task<Void> task = new Task<>() {
					@Override
					public Void call() {
						String message = client.receiveMove();
						String[] splittedMessage = message.toLowerCase().trim().split(" ");
						if (splittedMessage[0].equals("wait")) {
							makeMove.setValue(false);
							Platform.runLater(() -> {
								waitingLabel.setText("Waiting for the adversary to make a move...");
								disableMoveFields();
							});
						}
						return null;
					}
				};
				new Thread(task).start();

			} else {

				Platform.runLater(() -> {
					showAlert("VICTORY!", AlertType.CONFIRMATION);
					disconnectUser();
				});
				System.out.println("Victory");
			}
		};
		
		playing.addListener(playingListener);

		makeMoveListener = (observable, oldValue, newValue) -> {

			if (!newValue) {
				// start a task to recieve the hit messages from the adversary
				Task<Void> task = new Task<>() {
					@Override
					public Void call() {

						while (true) {

							String message = client.receiveMove();
							String[] splittedMessage = message.toLowerCase().trim().split(" ");

							if (splittedMessage[0].equals("move")) {
								// write an x on the hitted cell
								int colIndex = getIndexFromCharCol(splittedMessage[1].charAt(0)),
										rowIndex = getIndexFromIntRow(Integer.parseInt(splittedMessage[2]));

								Platform.runLater(() -> {
									writeX(colIndex, rowIndex);
								});

							} else if (splittedMessage[0].equals("yourturn")) {
								// enable che possibility of making a move
								makeMove.setValue(true);

								Platform.runLater(() -> {
									switchWaitingPlaying();
								});

								break;

							} else if (splittedMessage[0].equals("loser")) {
								// GAME OVER

								Platform.runLater(() -> {
									showAlert("GAME OVER!", AlertType.ERROR);
									disconnectUser();
								});

								System.out.println("Game over.");
								break;
							}

						}

						return null;

					}

					private void writeX(int col, int row) {
						Label label = new Label("   X");
						label.setPrefWidth(35);
						label.setPrefHeight(35);
						gridUser.add(label, col, row);
					}
				};

				new Thread(task).start();
			}

		};
		
		makeMove.addListener(makeMoveListener);

	}

	private int getIndexFromCharCol(char col) {
		return col - 'a' + 1;
	}

	private int getIndexFromIntRow(int row) {
		return gridAdversary.getRowCount() - row - 1;
	}

}
