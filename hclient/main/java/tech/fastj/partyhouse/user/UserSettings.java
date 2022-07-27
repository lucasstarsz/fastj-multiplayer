package tech.fastj.partyhouse.user;

//import tech.fastj.stackattack.scenes.game.GameStartDifficulty;

public class UserSettings {

    //    private GameStartDifficulty gameStartDifficulty;
    private int highestDifficultyReached;

    public UserSettings() {
//        this.gameStartDifficulty = GameStartDifficulty.Normal;
        this.highestDifficultyReached = 0;
    }

    public UserSettings(int highestDifficultyReached) {
//        this.gameStartDifficulty = GameStartDifficulty.Normal;
        this.highestDifficultyReached = highestDifficultyReached;
    }

//    public UserSettings(GameStartDifficulty gameStartDifficulty) {
//        this.gameStartDifficulty = gameStartDifficulty;
//        this.highestDifficultyReached = 0;
//    }

//    public UserSettings(int highestDifficultyReached, GameStartDifficulty gameStartDifficulty) {
//        this.gameStartDifficulty = gameStartDifficulty;
//        this.highestDifficultyReached = highestDifficultyReached;
//    }

//    public void setGameStartDifficulty(GameStartDifficulty gameStartDifficulty) {
//        this.gameStartDifficulty = gameStartDifficulty;
//    }
//
//    public GameStartDifficulty getGameStartDifficulty() {
//        return gameStartDifficulty;
//    }

    public int getHighestDifficultyReached() {
        return highestDifficultyReached;
    }

    public void incrementDifficulty() {
        highestDifficultyReached++;
    }
}
