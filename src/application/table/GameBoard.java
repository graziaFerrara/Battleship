package application.table;

import java.util.HashMap;
import java.util.Map;

public class GameBoard {
	
	public final static int NUMROWS = 10, NUMCOLS = 11;
	
	private Map <String, Integer> ships;
	private char[][] matrix = new char[NUMROWS][NUMCOLS];
	
	private boolean placementCompleted = false;
	private int hits;
	private int totHits;
	
	/**
	 * Populates the matrix.
	 */
	public GameBoard() {
		
		ships = new HashMap <> ();
		ships.put("AircraftCarrier", 5);
		ships.put("Battleship", 4);
		ships.put("Destroyer", 3);
		ships.put("Submarine", 3);
		ships.put("PatrolBoat", 2);
		
		for(int i=0; i<NUMROWS; i++)
			for(int j=0; j<NUMCOLS; j++)
				matrix[i][j] = '-';
		
		hits = 0;
		totHits = 0;
		
	}

	/**
	 * Allows to place a ship into the game board.
	 * @param name
	 * @param startRow
	 * @param startCol
	 * @param length
	 * @param orientation
	 * @return
	 */
	public boolean placeShip(String name, int startRow, char startCol, int length, String orientation) {
		
		if (ships.containsKey(name)) {
			
			ships.remove(name);
			
			insertShip(startRow, startCol, length, orientation);
			
			if (ships.size()<=0) placementCompleted = true;
			
			return true;
			
		}else {
			
			System.out.println("Not valid ship");
			
			return false;
			
		}
	}
	
	/**
	 * Utility mthod to insert a ship.
	 * @param startRow
	 * @param startCol
	 * @param length
	 * @param orientation
	 */
	private void insertShip(int startRow, char startCol, int length, String orientation) {
		
		int rowIndex = convertRowToIndex(startRow), colIndex = convertCharToIndex(startCol);
		
		if(orientation.equals("horizontal")) {
			
			for(int i=colIndex; i<colIndex+length; i++)
				if (matrix[rowIndex][i] != 'S') {
					matrix[rowIndex][i] = 'S';
					totHits++;
				}
			
		} else if (orientation.equals("vertical")) {
			
			for(int i=rowIndex; i<rowIndex+length; i++)
				if (matrix[i][colIndex] != 'S') {
					matrix[i][colIndex] = 'S';
					totHits++;
				}
			
		}
		
	}

	/**
	 * Allows to make a move and returns the outcome as a string.
	 * @param col
	 * @param row
	 * @return
	 */
	public String makeMove(char col, int row) {
		
		int rowIndex = convertRowToIndex(row), colIndex = convertCharToIndex(col);
		
		if (matrix[rowIndex][colIndex] == 'S') {
			matrix[rowIndex][colIndex] = 'X';
			hits++;
			return "hit";
		} else if (matrix[rowIndex][colIndex] == '-') {
			matrix[rowIndex][colIndex] = '0';
			return "miss";
		}
		
		return "invalid";
	}
	
	private void printMatrix() {
		
		System.out.println("\nCurrent matrix:");
		
		for(int i=0; i<NUMROWS; i++) {
			for(int j=0; j<NUMCOLS; j++)
				System.out.print(matrix[i][j]);
			System.out.println("\n");
		}
	}
	
	private int convertRowToIndex(int row) {
		return NUMROWS - row - 1;
	}
	
	private int convertCharToIndex(char col) {
		return col - 'a';
	}
	
	public int getTotHits() {
		return totHits;
	}
	
	public boolean isPlacementCompleted() {
		return placementCompleted;
	}
	
	public boolean isLoser() {
		return (hits == totHits);
	}

}
