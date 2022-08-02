package tech.fastj.partyhousecore;

import tech.fastj.network.rpc.CommandAlias;
import tech.fastj.network.rpc.classes.Classes;
import tech.fastj.network.rpc.classes.Classes0;
import tech.fastj.network.rpc.classes.Classes1;
import tech.fastj.network.rpc.classes.Classes2;
import tech.fastj.network.rpc.classes.Classes3;
import tech.fastj.network.rpc.classes.Classes4;
import tech.fastj.network.rpc.classes.Classes5;
import tech.fastj.network.rpc.classes.Classes6;

public enum Commands implements CommandAlias {
    ClientJoinLobby(ClientInfo.class),
    ClientLeaveLobby(ClientInfo.class),
    UpdateClientInfo(ClientInfo.class),
    Ready(ClientInfo.class),
    UnReady(ClientInfo.class),

    UpdateClientGameState(ClientInfo.class, ClientPosition.class, ClientVelocity.class),
    ModifyPoints(),
    SetPoints(),
    GameFinished(ClientInfo.class),
    GameResults(ClientPoints[].class),
    SwitchScene(String.class),

    SnowballThrow(SnowballInfo.class),
    SnowballHit(ClientInfo.class, SnowballInfo.class);

    private final Classes commandClasses;

    @Override
    public Classes getCommandClasses() {
        return commandClasses;
    }

    Commands(Class<?>... classes) {
        this.commandClasses = switch (classes.length) {
            case 0 -> new Classes0();
            case 1 -> new Classes1<>(classes[0]);
            case 2 -> new Classes2<>(classes[0], classes[1]);
            case 3 -> new Classes3<>(classes[0], classes[1], classes[2]);
            case 4 -> new Classes4<>(classes[0], classes[1], classes[2], classes[3]);
            case 5 -> new Classes5<>(classes[0], classes[1], classes[2], classes[3], classes[4]);
            case 6 -> new Classes6<>(classes[0], classes[1], classes[2], classes[3], classes[4], classes[5]);
            default -> throw new IllegalStateException("Unexpected value: " + classes.length);
        };
    }
}
