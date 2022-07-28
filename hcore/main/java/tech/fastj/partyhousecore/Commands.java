package tech.fastj.partyhousecore;

import tech.fastj.network.rpc.commands.Command;

import java.util.UUID;

public class Commands {
    public static final Command.Id ClientJoinLobby = new Command.Id("Client Joined", UUID.fromString("45567674-7baf-4c2c-b4a2-2f10633ec146"));
    public static final Command.Id ClientLeaveLobby = new Command.Id("Client Left", UUID.fromString("452d7d49-3b2d-4721-8f42-7dcad7bd9647"));
    public static final Command.Id UpdateClientInfo = new Command.Id("Update Client Info", UUID.fromString("3439fdae-dbac-49d1-9bbc-30ffed0ffab2"));

    public static final Command.Id UpdateClientGameState = new Command.Id("Update Client Game State", UUID.fromString("a98001a6-61f9-4224-89f8-d0d7c09f3f0b"));
    public static final Command.Id Ready = new Command.Id("Ready to Play Game", UUID.fromString("07d842e6-e4ce-4231-a9f3-39e704906789"));
    public static final Command.Id UnReady = new Command.Id("Not Ready to Play Game", UUID.fromString("9e983d66-c47d-4753-b247-a1b8092d7b0f"));

    public static final Command.Id ModifyPoints = new Command.Id("Modify Points", UUID.fromString("9fa17a1b-00db-468d-8f34-c41b94f066e2"));
    public static final Command.Id SetPoints = new Command.Id("Set Points", UUID.fromString("bb70704a-981f-4d81-b226-c9f03a887ee0"));
    public static final Command.Id GameFinished = new Command.Id("Game Finished", UUID.fromString("43eb4f2e-151d-426a-b0d8-6df009b7c59e"));
    public static final Command.Id GameResults = new Command.Id("Game Results", UUID.fromString("fb80bd83-ee43-4d00-a8a8-6ada96dbf737"));
    public static final Command.Id SwitchScene = new Command.Id("Switch Session", UUID.fromString("a440754d-9de9-4af1-9187-7177f8df8eff"));

    public static final Command.Id SnowballThrow = new Command.Id("Throw Snowball", UUID.fromString("cef7cfaa-89cd-4b77-b0ec-f2fab68c2d76"));
    public static final Command.Id SnowballHit = new Command.Id("Hit By Snowball", UUID.fromString("4864463a-50c4-4e5f-8eef-3c61ffc02513"));
}
