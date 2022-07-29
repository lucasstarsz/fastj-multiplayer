package tech.fastj.partyhouse.util;

import tech.fastj.logging.Log;
import tech.fastj.math.Pointf;

import tech.fastj.systems.control.GameHandler;

import java.util.Map;
import java.util.UUID;

import tech.fastj.partyhouse.objects.Player;
import tech.fastj.partyhousecore.ClientInfo;
import tech.fastj.partyhousecore.ClientPosition;
import tech.fastj.partyhousecore.ClientVelocity;
import tech.fastj.partyhousecore.PositionState;

public class PlayerUtil {

    public static Player createPlayer(PositionState positionState) {
        Log.info("creating new player instance for {}", positionState.getClientInfo().clientName());
        return new Player(positionState.getClientInfo().clientName());
    }

    public static PositionState createPositionState(ClientInfo clientInfo, Pointf center) {
        Log.info("creating new position state for {}", clientInfo.clientName());

        PositionState positionState = new PositionState();

        positionState.setClientInfo(clientInfo);
        positionState.setClientPosition(new ClientPosition(center.x, center.y));
        positionState.setClientVelocity(new ClientVelocity());

        return positionState;
    }

    public static PositionState createOtherPositionState(ClientInfo clientInfo, Pointf center, Map<UUID, PositionState> otherPlayerPositionStates) {
        PositionState positionState = createPositionState(clientInfo, center);
        otherPlayerPositionStates.put(clientInfo.clientId(), positionState);

        return positionState;
    }

    public static Player createOtherPlayer(PositionState positionState, Map<UUID, Player> otherPlayers, GameHandler gameHandler) {
        Player otherPlayer = createPlayer(positionState);
        Player replaced = otherPlayers.put(positionState.getClientInfo().clientId(), otherPlayer);

        if (replaced != null) {
            replaced.destroy(gameHandler);
        }

        gameHandler.drawableManager().addGameObject(otherPlayer);

        return otherPlayer;
    }
}
