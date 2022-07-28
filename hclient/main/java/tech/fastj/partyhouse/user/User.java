package tech.fastj.partyhouse.user;

import tech.fastj.network.rpc.Client;

import java.net.InetAddress;

import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.PointsState;

public class User {

    private static final User Instance = new User();

    private Client client;
    private ClientInfo clientInfo;
    private final PointsState pointsState;
    private final UserSettings settings;
    private InetAddress customIp;

    private User() {
        settings = new UserSettings();
        pointsState = new PointsState();
    }

    public void setCustomIp(InetAddress customIp) {
        this.customIp = customIp;
    }

    public InetAddress getCustomIp() {
        return customIp;
    }

    public PointsState getPointsState() {
        return pointsState;
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

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public static User getInstance() {
        return Instance;
    }
}
