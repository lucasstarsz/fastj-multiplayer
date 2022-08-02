package tech.fastj.partyhouse.user;

import tech.fastj.network.rpc.local.LocalClient;

import java.net.InetAddress;

import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.Commands;
import tech.fastj.partyhousecore.PointsState;

public class User {

    private static final User Instance = new User();

    private LocalClient<Commands> client;
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

    public LocalClient<Commands> getClient() {
        return client;
    }

    public void setClient(LocalClient<Commands> client) {
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
