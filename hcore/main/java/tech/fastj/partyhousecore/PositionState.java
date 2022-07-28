package tech.fastj.partyhousecore;

import tech.fastj.math.Pointf;
import tech.fastj.graphics.game.GameObject;

import tech.fastj.network.rpc.NetworkSender;
import tech.fastj.network.rpc.message.CommandTarget;
import tech.fastj.network.rpc.message.NetworkType;

import java.io.IOException;

public class PositionState {

    private ClientInfo clientInfo;
    private ClientPosition clientPosition;
    private ClientVelocity clientVelocity;
    private boolean isPlayerDead;

    private boolean needsUpdate;

    public PositionState() {
    }

    public boolean isPlayerDead() {
        return isPlayerDead;
    }

    public void setPlayerDead(boolean playerDead) {
        isPlayerDead = playerDead;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        needsUpdate = true;
    }

    public ClientPosition getClientPosition() {
        return clientPosition;
    }

    public void setClientPosition(ClientPosition clientPosition) {
        this.clientPosition = clientPosition;
        needsUpdate = true;
    }

    public ClientVelocity getClientVelocity() {
        return clientVelocity;
    }

    public void setClientVelocity(ClientVelocity clientVelocity) {
        this.clientVelocity = clientVelocity;
        needsUpdate = true;
    }

    public void updatePlayerPosition(GameObject player) {
        player.setTranslation(new Pointf(clientPosition.x(), clientPosition.y()));
        player.rotate(-player.getRotation());
        player.rotate(clientVelocity.angle());
    }

    public boolean needsUpdate() {
        return needsUpdate;
    }

    public void sendUpdate(NetworkSender sender, CommandTarget target) throws IOException {
        needsUpdate = false;
        if (!isPlayerDead) {
            sender.sendCommand(NetworkType.UDP, target, Commands.UpdateClientGameState, clientInfo, clientPosition, clientVelocity);
        }
    }

    public void updateVelocity(float inputAngle, float v) {
        setClientVelocity(new ClientVelocity(clientVelocity.angle() + inputAngle, clientVelocity.speed() + v));
    }

    public void updatePosition(float x, float y) {
        setClientPosition(new ClientPosition(clientPosition.x() + x, clientPosition.y() + y));
    }
}
