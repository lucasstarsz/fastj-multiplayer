package tech.fastj.partyhousecore;

public class PointsState {

    private int points;
    private ClientInfo clientInfo;

    public PointsState() {
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void modifyPoints(int points) {
        this.points += points;
    }

    public void resetPoints() {
        points = 0;
    }

    public ClientPoints createClientPoints() {
        return new ClientPoints(clientInfo, points);
    }

    @Override
    public String toString() {
        return "PointsState{" +
            "points=" + points +
            ", clientInfo=" + clientInfo +
            '}';
    }
}
