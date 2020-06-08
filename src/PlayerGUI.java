
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class PlayerGUI extends JFrame {

    // total ship count
    private int shipSize = 5;

    // number of rows and cols in gird board
    private static final int ROW = 10, COL = 10;

    // all jpanel for the GUI
    private JPanel leftGridPanel, rightGridPanel, bottomNotificationPanel;

    // label for the bottom notice
    private JLabel notification;

    // my buttons on the grid
    private JButton[][] myButtons;

    // opponent buttons on the grid
    private JButton[][] opponentButtons;

    // auto play
    private JButton autoPlayButton;

    // track the turn and the ship placement
    public boolean myTurn, placedShips;

    // all the status in grids
    private int[][] status;

    // oppenents attacked grids
    private boolean[][] attackedGrids;

    // player for this Intarface
    private Player player;

    // random class instance
    private Random random;

    // for ship place events
    private ShipPlacementHandler shipPlacementHandler;

    // for attack event
    private AttackHandler attackHandler;

    public PlayerGUI(Player player) throws HeadlessException {
        super(player.getPlayerName());
        this.player = player;
        this.random = new Random();
        this.placedShips = false;

        // initialize the GUI
        init();

        // create the GUI with all dimentions and locations
        createGUI();
    }

    // Initilaize the GUI
    public void init() {

        // create UI components
        leftGridPanel = new JPanel();
        rightGridPanel = new JPanel();
        bottomNotificationPanel = new JPanel();

        // notification label
        notification = new JLabel();

        // all buttons
        myButtons = new JButton[ROW][COL];
        opponentButtons = new JButton[ROW][COL];
        autoPlayButton = new JButton("Auto Play");

        // for gird status
        status = new int[ROW][COL];
        attackedGrids = new boolean[ROW][COL];

        // create the event listners
        shipPlacementHandler = new ShipPlacementHandler();
        attackHandler = new AttackHandler(this);

        // initialize the status to 0
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                status[i][j] = 0;
                attackedGrids[i][j] = false;
            }
        }
    }

    // To do style the UI
    public void createGUI() {

        // left panel
        leftGridPanel = new JPanel();
        leftGridPanel.setLayout(new GridLayout(ROW, COL));

        // set my background color
        leftGridPanel.setBackground(player.getMyColor());

        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                myButtons[i][j] = new JButton();
                leftGridPanel.add(myButtons[i][j]);

                // set the button size
                myButtons[i][j].setPreferredSize(new Dimension(30, 10));

                // add action listner for each my buttons
                myButtons[i][j].addActionListener(shipPlacementHandler);

                // disable button
                myButtons[i][j].setEnabled(false);

                // remove the focusable painted border
                myButtons[i][j].setFocusPainted(false);

                // set opaque to show the background color
                myButtons[i][j].setOpaque(true);

                // set button default color to gray
                myButtons[i][j].setBackground(Color.GRAY);
            }
        }

        // set the button size
        autoPlayButton.setPreferredSize(new Dimension(100, 25));
        autoPlayButton.setEnabled(false);

        autoPlayButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String txt = autoPlayButton.getText();
                Server server = (Server) player;
                if (txt.equals("Auto Play")) {
                    autoPlayButton.setText("Manual");
                    server.autoPlay = true;
                    if (!placedShips) {
                        autoPlaceShips();
                    }
                } else {
                    autoPlayButton.setText("Auto Play");
                    server.autoPlay = false;
                }
            }
            
        });

        // opponent panel
        rightGridPanel = new JPanel();
        rightGridPanel.setLayout(new GridLayout(10, 10));

        // set background color for the opponent grid view
        rightGridPanel.setBackground(player.getOpponentColor());

        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                opponentButtons[i][j] = new JButton();
                rightGridPanel.add(opponentButtons[i][j]);

                // disable the button 
                opponentButtons[i][j].setEnabled(false);

                // set opaque to show the background color
                opponentButtons[i][j].setOpaque(true);

                // remove the focusable painted border
                opponentButtons[i][j].setFocusPainted(false);

                // set button default color to gray
                opponentButtons[i][j].setBackground(Color.GRAY);

                // set the button size
                opponentButtons[i][j].setPreferredSize(new Dimension(30, 10));

                // add action listner for each my buttons
                opponentButtons[i][j].addActionListener(attackHandler);
            }
        }

        // bottom info panel
        bottomNotificationPanel = new JPanel();

        // add the label to the info panel
        bottomNotificationPanel.add(notification);

        // if the player is server(player 2)
        if (player instanceof Server) {

            // opponent grid panel is set to the west of the frame
            this.add(leftGridPanel, BorderLayout.WEST);
            this.add(rightGridPanel, BorderLayout.EAST);
            this.setLocation(new Point(20, 70));
            bottomNotificationPanel.add(autoPlayButton);

        } else {
            // if the player is client(player 1)
            // opponent grid panel is set to the east of the frame
            this.add(leftGridPanel, BorderLayout.EAST);
            this.add(rightGridPanel, BorderLayout.WEST);
            this.setLocation(new Point(710, 70));
        }

        // add the botton notification panel to the frame
        this.add(bottomNotificationPanel, BorderLayout.SOUTH);

        // set the frame size
        this.setSize(650, 550);

        // off resize
        this.setResizable(false);

        // set default close operation (exit on click close button)
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // To reset the GUI for a new round
    public void reset() {

        // set the ship size
        shipSize = 5;

        // mark as no ship placed
        placedShips = false;

        setNotification("Place all ships to play again!");

        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {

                // reset status
                status[i][j] = 0;
                attackedGrids[i][j] = false;

                // set buttons color to grid
                opponentButtons[i][j].setBackground(Color.gray);
                autoPlayButton.setText("Auto Play");
                myButtons[i][j].setBackground(Color.gray);
            }
        }
        // disable the attack mode
        enableAttackMode(false);

        // enable the ship place mode
        enableShipPlaceMode(true);
    }

    // Take whether the current turn is mine or not
    public boolean isMyTurn() {
        return myTurn;
    }

    // update the turn
    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    // Enable or disable the attack mode
    public void enableAttackMode(boolean value) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                opponentButtons[i][j].setEnabled(value);
                autoPlayButton.setEnabled(value);
            }
        }
    }

    // Enable or disable the Ship Place Mode
    public void enableShipPlaceMode(boolean value) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                myButtons[i][j].setEnabled(value);
                autoPlayButton.setEnabled(value);
            }
        }
    }

    // This is the method to access the notification label
    public void setNotification(String msg) {
        this.notification.setText(msg);
    }

    // To update hit in opponent view 
    public void hit(int i, int j) {

        attackedGrids[i][j] = true;
        // if hit
        opponentButtons[i][j].setBackground(Color.RED);
        // and disable that button
        opponentButtons[i][j].setEnabled(false);
    }

    // To update miss in opponent view 
    public void miss(int i, int j) {

        attackedGrids[i][j] = true;
        // if miss
        opponentButtons[i][j].setBackground(Color.WHITE);
        // and disable that button
        opponentButtons[i][j].setEnabled(false);
    }

    // Attack to the openent position (i, j)
    // i = row index, j = column index
    public boolean attack(int i, int j) {

        setMyTurn(true);
        setNotification("Your turn");

        if (status[i][j] == 0) {
            // if miss return false
            return false;
        } else {
            // if hit
            myButtons[i][j].setOpaque(true);
            myButtons[i][j].setBackground(Color.RED);
            status[i][j] = 0;
            // if hit return true
            return true;
        }

    }

    public void autoPlaceShips() {

        while (!placedShips) {
            int i = random.nextInt(ROW);
            int j = random.nextInt(COL);

            placeShip(i, j);
        }
    }

    public int[] guessAttack() {
        while (true) {
            int i = random.nextInt(ROW);
            int j = random.nextInt(COL);

            if (!attackedGrids[i][j]) {
                return new int[]{i, j};
            }
        }
    }

    /**
     * To place the current sized ship from the clicked grid
     *
     * @param i - row index
     * @param j - col index
     */
    public void placeShip(int i, int j) {

        boolean up = false, down = false, right = false, left = false;
        // check up
        if (i - shipSize < 0) {
            up = false;
        } else {
            int total = 0;
            for (int k = 0; k < shipSize; k++) {
                total += status[i - k][j];
            }

            if (total == 0) {
                up = true;
            }
        }

        // check down
        if (i + shipSize > 9) {
            down = false;
        } else {
            int total = 0;
            for (int k = 0; k < shipSize; k++) {
                total += status[i + k][j];
            }

            if (total == 0) {
                down = true;
            }
        }

        // check right
        if (j + shipSize > 9) {
            right = false;
        } else {
            int total = 0;
            for (int k = 0; k < shipSize; k++) {
                total += status[i][j + k];
            }

            if (total == 0) {
                right = true;
            }
        }

        // check left
        if (j - shipSize < 0) {
            left = false;
        } else {
            int total = 0;
            for (int k = 0; k < shipSize; k++) {
                total += status[i][j - k];
            }

            if (total == 0) {
                left = true;
            }
        }

        if (!up && !down && !right && !left) {
            // cannot place current ship for this location
            System.out.printf("No appropriate "
                    + "locations found to place %d size ship\n", shipSize);
        } else {

            // choose random available direction to place current ship
            int direction = 0;
            while (true) {
                direction = random.nextInt(4);
                if (direction == 0 && down) {
                    break;
                } else if (direction == 1 && right) {
                    break;
                } else if (direction == 2 && up) {
                    break;
                } else if (direction == 3 && left) {
                    break;
                }
            }

            switch (direction) {
                case 0: // down
                    for (int k = 0; k < shipSize; k++) {
                        status[i + k][j] = shipSize;
                        myButtons[i + k][j].setBackground(player.getMyColor());
                    }
                    break;
                case 1: // right
                    for (int k = 0; k < shipSize; k++) {
                        status[i][j + k] = shipSize;
                        myButtons[i][j + k].setBackground(player.getMyColor());
                    }
                    break;
                case 2: // up
                    for (int k = 0; k < shipSize; k++) {
                        status[i - k][j] = shipSize;
                        myButtons[i - k][j].setBackground(player.getMyColor());
                    }
                    break;
                case 3: // left
                    for (int k = 0; k < shipSize; k++) {
                        status[i][j - k] = shipSize;
                        myButtons[i][j - k].setBackground(player.getMyColor());
                    }
                    break;
                default:
                    break;
            }

            // one ship was placed, decrese the ship size
            shipSize--;

            if (shipSize <= 0) {
                // all ships are placed
                placedShips = true;
                player.sendMessage("placed");

                // off the ship mode
                enableShipPlaceMode(false);
            }
        }

    }

    // Return whether all shiped were placed or not
    public boolean hasPlacedShips() {
        return placedShips;
    }

    // This is the action listner for ship placement
    private class ShipPlacementHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

            if (player instanceof Server) {
                Server s = (Server) player;

                if (s.autoPlay) {
                    return;
                }
            }

            Object source = e.getSource();
            for (int i = 0; i < ROW; i++) {
                for (int j = 0; j < COL; j++) {
                    if (source == myButtons[i][j]) {
                        placeShip(i, j);
                        return;
                    }
                }
            }
        }
    }

    // This is the action listner for an attack
    private class AttackHandler implements ActionListener {

        // player GUI
        PlayerGUI gui;

        public AttackHandler(PlayerGUI gui) {
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            if (player instanceof Server) {
                Server s = (Server) player;

                if (s.autoPlay) {
                    return;
                }
            }

            Object source = e.getSource();
            for (int i = 0; i < ROW; i++) {
                for (int j = 0; j < COL; j++) {
                    if (source == opponentButtons[i][j]) {
                        // if my turn
                        if (isMyTurn()) {
                            // send the corresponding attacked position
                            player.sendMessage(i + "," + j);
                        } else {
                            // if the turn is not your notify it
                            JOptionPane.showMessageDialog(gui, "Other player's turn");
                        }
                    }
                }
            }
        }
    }

}
