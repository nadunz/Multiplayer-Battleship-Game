
import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.net.*;
import java.io.*;

public class Client extends Player {

    // socket to write outputs and read inputs
    private Socket socket;

    // Data input Stream of the connected socket
    private DataInputStream input;

    // Data Output Stream of the connected socket
    private DataOutputStream output;

    // GUI for the client
    private PlayerGUI playerGUI;

    public Client(String name, Color myColor, Color opponentColor) {
        super(name, myColor, opponentColor);

        // crete new player GUI for the client
        this.playerGUI = new PlayerGUI(this);

        // set as client's turn
        this.playerGUI.setMyTurn(true);
    }

    // start to create the connection
    public void start(String host, int port) throws IOException {

        try {
            // create socket for connecting to the server
            socket = new Socket(host, port);

            // create input and output steams for the socket
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            // send conneted(request) message to the server
            sendMessage("connected");

            // start the client player thread to response to the receiving messages
            new ClientPlayerThread().start();

        } catch (ConnectException ex) {
            JOptionPane.showMessageDialog(playerGUI, ex.getMessage());
            System.exit(0);
        } catch (Exception ex) {
            System.out.println("Host or port is unreachable.");
            System.exit(0);
        }
    }

    /**
     * This method send any given message to the server
     *
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
     * Return the receiving message from server
     *
     * @return the read messge
     */
    @Override
    public String readMessage() {

        String readUTF = null;
        try {
            readUTF = input.readUTF().trim();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(playerGUI, "Server disconnected!");
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

    // Thread for play the game by responding messages to the server
    private class ClientPlayerThread extends Thread {

        @Override
        public void run() {

            while (true) {

                // read the message from the socket
                String msg = readMessage();

                // if the message is null exit the client
                if (msg == null) {
                    System.exit(0);
                }

                if (msg.equals("start")) {

                    // time to start the game
                    // show the client GUI
                    playerGUI.setVisible(true);
                    playerGUI.setNotification("Player 2 connected, place all the ships.");

                    // enable the ship placement mode
                    playerGUI.enableShipPlaceMode(true);

                } else if (msg.equals("placed")) {

                    reset();

                    // if the server placed the all ships
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
                        playerGUI.setNotification("Player 2 has placed all ships.. "
                                + "please place your ships to play.");
                    }
                } else if (msg.equals("play")) {

                    reset();

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
                    playerGUI.reset(); // reset the GUI
                    playerGUI.setMyTurn(true);

                    // reset the player
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

                        // check whether the client player has victory
                        if (getNumOfRemainingHits() == 0) {
                            sendMessage("win:" + getPlayerName());

                            // confirm message to play again or exit
                            playerGUI.setNotification("You won the game!");
                            int opt = JOptionPane.showConfirmDialog(playerGUI, "You won!. Do you need to play again ?", "Win",
                                    JOptionPane.YES_NO_OPTION);

                            if (opt == 0) {
                                playerGUI.reset();
                                sendMessage("reset");
                                reset();
                                playerGUI.setMyTurn(true);
                                setWin(true);
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

                    } else {

                        // read the oppenent attack
                        String[] data = msg.split(",");

                        // row and column indexes
                        int i = Integer.parseInt(data[0].trim()); // attack row
                        int j = Integer.parseInt(data[1].trim()); // attack col
                        boolean attack = playerGUI.attack(i, j);

                        if (!win) {
                            // send the result of the attack
                            sendMessage((attack ? "hit:" : "miss:") + (i + "," + j));
                        }

                    }

                }
            }

        }
    }

    public static void main(String args[]) {

        int port = 4500; // default port
        String host = "localhost"; // default ip address

        if (args.length >= 2) {
            // if there are command line arguments
            // read them
            host = args[0].trim();
            try {
                port = Integer.parseInt(args[1].trim());
            } catch (NumberFormatException e) {
                // if the port is invalid
                System.out.println("Invalid port! default port is used.");
            }
        }
        // create a client instance
        Client client = new Client("Player 1", Color.ORANGE, Color.GREEN);

        // start the client by parsing the host and port
        try {
            client.start(host, port);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
