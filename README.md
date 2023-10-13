# Battleship
This project is a JavaFX implementation of the <b>Battleship game</b> that uses <b>UDP</b> for communication between a server and multiple clients.
<ul>
  <li>Each user is identified by a unique username, and the server refuses connections if the username is already logged in.</li>
  <li>The game server creates a game table for every two clients who connect before starting a game.</li>
  <li>After joining a table, each player is able to specify the locations of their ships as points on the game board.</li>
  <li>There are five ships of different dimension: <i>Aircraft Carrier(5)</i>, <i>Battleship(4)</i>, <i>Destroyer(3)</i>, <i>Submarine(3)</i>, and <i>Patrol Boat(2)</i>.</li>
  <li>Once both players have placed their ships, the game starts. </li>
  <li>Each player inputs a coordinate to attack the opponent's ships.</li>
  <li>The server sends a message to each player indicating whether they hit or missed the enemy's ship.</li>
  <li>The game boards are updated after each turn.</li>
  <li> The game continues until one player destroys all of the opponent's ships.</li>
</ul>
<br/>
<hr/>
<br/>
To run the project in the Eclipse IDE make sure to:
<ul>
  <li>install the package <i>e(fx)clipse</i> from the marketplace <i>(Help > Eclipse Marketplace)</i>, or if you have one of the more recent Java versions, it's better to download its nightly version directly from their repository to avoid the bug verifying when creating a FXML document</li>
  <li>download the JavaFX SDK (I used the 21) <a href="https://gluonhq.com/products/javafx/" target="_blank" >here</a></li>
  <li>in <i>Preferences > JavaFX</i> set the path to the lib folder of the SDK</li>
  <li>in <i>Preferences > User Libraries</i> create a new library (it's sufficient that it contains the jars from the lib folder of the SDK)</li>
  <li>right click on the project and in <i>Build Path > Configure BuildPath > Libraries > Modulepath</i> add the just created library</li>
  <li>during the execution, in <i>Run Configurations > Arguments > VM arguments</i>, insert <i>--module-path="/Library/Frameworks/javafx-sdk-21/lib" --add-modules=javafx.controls,javafx.fxml</i> putting your path to the SDK,</li>
  <li>to avoid problems when running, uncheck the option <i>"Use the -XstartOnFirstThread argument when launching with SWT"</i> (for both the server and the client)</li>
</ul>

