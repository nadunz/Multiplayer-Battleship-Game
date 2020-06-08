
import java.awt.Color;

/**
 * Abstract player class (client and server will inherit this class)
 */
public abstract class Player {

    // name of the player
    private String name;
    // rest number of hit need to win the game
    private int numOfRemainingHits;
    // this player color and other player color
    private Color myColor, opponentColor;

    public boolean win = false;

    // construct a player
    public Player(String name, Color myColor, Color opponentColor) {
        this.name = name;
        this.myColor = myColor;
        this.opponentColor = opponentColor;
        // initially sum of all ship sizes
        this.numOfRemainingHits = 5 + 4 + 3 + 2 + 1;
    }

    // Get the player name
    public String getPlayerName() {
        return name;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    // Get my color
    public Color getMyColor() {
        return myColor;
    }

    // Returns oppenent color
    public Color getOpponentColor() {
        return opponentColor;
    }

    // Decrement number of remaining hits after player got a hit from the attack 
    public void decreaseRestHits() {
        numOfRemainingHits--;
    }

    // Returns the number of remainnig hits
    public int getNumOfRemainingHits() {
        return numOfRemainingHits;
    }

    // Reset the number of rest hits after each new round
    public void reset() {
        this.numOfRemainingHits = 5 + 4 + 3 + 2 + 1;
        win = false;
    }

    // send given message to other player
    public abstract void sendMessage(String message);

    // read the recieved message from other player 
    public abstract String readMessage();
}
