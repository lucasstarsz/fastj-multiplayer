package tech.fastj.partyhouse.user;

import tech.fastj.network.rpc.Client;

public class User {

    private static final User Instance = new User();

    private int score;
    private int highScore;
    private int numberStacked;
    private int highestNumberStacked;
    private boolean hasHighScore;
    private boolean hasHighBlocksStacked;
    private Client client;
    private final UserSettings settings;

    private User() {
        score = 0;
        highScore = 0;
        numberStacked = 0;
        highestNumberStacked = 0;
        hasHighScore = false;
        hasHighBlocksStacked = false;
        settings = new UserSettings();
    }

    public int getScore() {
        return score;
    }

    public int getHighScore() {
        return highScore;
    }

    public int getNumberStacked() {
        return numberStacked;
    }

    public int getHighestNumberStacked() {
        return highestNumberStacked;
    }

    public boolean getHasHighScore() {
        return hasHighScore;
    }

    public boolean getHasHighBlocksStacked() {
        return hasHighBlocksStacked;
    }

    public UserSettings getSettings() {
        return settings;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void addToScore(int scoreIncrement) {
        score += scoreIncrement;
        if (score > highScore) {
            highScore = score;
            hasHighScore = true;
        }

        numberStacked++;
        if (highestNumberStacked < numberStacked) {
            highestNumberStacked = numberStacked;
            hasHighBlocksStacked = true;
        }
    }

    public void resetScore() {
        score = 0;
        numberStacked = 0;
        hasHighScore = false;
        hasHighBlocksStacked = false;
    }

    public static User getInstance() {
        return Instance;
    }
}
