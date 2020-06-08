
import java.awt.Color;
import javax.swing.JOptionPane;
import java.net.*;
import java.io.*;

public class Server extends Player {

    // network server socket for the server
    private ServerSocket serversocket;

    // socket to write outputs and read inputs
    private Socket socket;

    // Data Output Stream of the connected socket
    private DataOutputStream output;

    // Data Input Stream of the connected socket
    private DataInputStream input;

    // GUI for the server
    private PlayerGUI playerGUI;
    
    // auto play mode false
    public boolean autoPlay = false;

    public Server(String name, Color myColor, Color opponentColor) {
        super(name, myColor, opponentColor);

        // crete new player GUI for the server
        this.playerGUI = new PlayerGUI(this);

        // show the player GUI
        this.playerGUI.setVisible(true);

        // it is not the server turn
        this.playerGUI.setMyTurn(false);
    }

    // start to listening on the given port
    public void start(int port) throws IOException {

        System.out.printf("Server(%s) connection Starting on port: %d\n", getPlayerName(), port);
        System.out.println("Waiting for player 1...");

        playerGUI.setNotification("Waiting for player 1..");

        // create a server socket
        serversocket = new ServerSocket(port);

        // after any client connected establish a new socket between client and server
        socket = serversocket.accept();

        // get the input and output steams of the socket
        output = new DataOutputStream(socket.getOutputStream());
        input = new DataInputStream(socket.getInputStream());

        new ServerPlayerThread().start();

    }

     /**
     * This method send any given message to the connected client
     * @param message the given message
     */
    @Override
    public void sendMessage(String message) {
        try {
            output.writeUTF(message);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(playerGUI, ex.getMessage());
        }
    }

    /**
     * Return the receiving message from client
     * @return the read message
     */
    @Override
    public String readMessage() {

        String readUTF = null;
        try {
            readUTF = input.readUTF().trim();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(playerGUI, "Client player disconnected");
        }

        return readUTF;
    }

    // To close the socket and I/O sreams
    public void closeConnection() {
        try {
            socket.close();
            output.close();
            input.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    // Thread for play the game by responding messages to the client
    private class ServerPlayerThread extends Thread {

        @Override
        public void run() {

            while (true) {
                
                // read the message from the socket
                String msg = readMessage();
                
                // if the message is null exit the server
                if (msg == null) {
                    System.exit(0);
                }
                
                // after the client sent a connected request
                if (msg.equals("connected")) {
                    
                    sendMessage("start");
                    // time to start the game
                    
                    playerGUI.enableShipPlaceMode(true);
                    playerGUI.setNotification("Player 1 connected, place all the ships.");
                    System.out.println("Player 1 connected!");

                } else if (msg.equals("placed")) {

                    // if the client placed the all ships
                    if (playerGUI.hasPlacedShips()) {
                        
                        // send message to play the game
                        sendMessage("play");
                        
                        // start the attack mode
                        playerGUI.enableAttackMode(true);
                        
                        // set the current turn in the GUI
                        if (playerGUI.isMyTurn()) {
                            playerGUI.setNotification("Your turn");
                        } else {
                            playerGUI.setNotification("Other player's turn");
                        }
                    } else {
                        playerGUI.setNotification("Player 1 has placed all ships.. please place your ships to play.");
                    }
                } else if (msg.equals("play")) {
                    
                    // if received to play request
                    playerGUI.enableAttackMode(true);

                    if (playerGUI.isMyTurn()) {
                        playerGUI.setNotification("Your turn");
                    } else {
                        playerGUI.setNotification("Other player's turn");
                    }

                } else if (msg.equals("exit")) {
                    
                    // if received an exit request
                    closeConnection();
                    System.exit(0);
                } else if (msg.equals("reset")) {
                    
                    // if received a reset request
                    playerGUI.reset();
                    autoPlay = false;
                    playerGUI.setMyTurn(false);
                    reset();
                } else {

                    String[] split = msg.split(":");

                    if (split[0].equals("hit")) {
                        //if hit
                        String[] location = split[1].split(",");
                        int i = Integer.parseInt(location[0].trim());
                        int j = Integer.parseInt(location[1].trim());

                        playerGUI.hit(i, j);
                        playerGUI.setMyTurn(false);
                        playerGUI.setNotification("Other player's turn");

                        decreaseRestHits();

                        // check whether the server has victory
                        if (getNumOfRemainingHits() == 0) {

                            sendMessage("win:" + getPlayerName());
                            playerGUI.setNotification("You won the game!");

                            // confirm message to play again or exit
                            int opt = JOptionPane.showConfirmDialog(playerGUI, "You won!. Do you need to play again ?", "Win",
                                    JOptionPane.YES_NO_OPTION);

                            if (opt == 0) {
                                playerGUI.reset();
                                sendMessage("reset");
                                reset();
                                autoPlay = false;
                                playerGUI.setMyTurn(false);
                            } else {
                                sendMessage("exit");
                                closeConnection();
                                System.exit(0);
                            }
                        }

                    } else if (split[0].equals("miss")) {
                        // if miss
                        String[] location = split[1].split(",");
                        int i = Integer.parseInt(location[0].trim());
                        int j = Integer.parseInt(location[1].trim());

                        playerGUI.miss(i, j);
                        playerGUI.setMyTurn(false);
                        playerGUI.setNotification("Other player's turn");

                    } else if (split[0].equals("win")) {
                        // if won other player
                        playerGUI.setNotification(split[1] + " won the game!");
                        autoPlay = false;

                    } else {
                        
                        // read the oppenent attack
                        String[] data = msg.split(",");
                        
                        // row and column indexes
                        int i = Integer.parseInt(data[0].trim()); // attack row
                        int j = Integer.parseInt(data[1].trim()); // attack col
                        boolean attack = playerGUI.attack(i, j);

                        // send the result of the attack
                        sendMessage((attack ? "hit:" : "miss:") + (i + "," + j));
                        
                        if(autoPlay) {
                           
                            int[] guess = playerGUI.guessAttack();
                            int r = guess[0];
                            int c = guess[1];
                            
                            // send the corresponding attacked position
                            sendMessage(r + "," + c);
                        }

                    }
                }
            }

        }
    }

    public static void main(String[] args) {

        int port = 4500; // default port

        if (args.length >= 1) {
            // if there are command line arguments
            // read them
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                // if the port is invalid
                System.out.println("Invalid port! default port is used.");
            }
        }
        // create a server instance
        Server server = new Server("Player 2", Color.GREEN, Color.ORANGE);

        // start the server by parsing the port
        try {
            server.start(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
