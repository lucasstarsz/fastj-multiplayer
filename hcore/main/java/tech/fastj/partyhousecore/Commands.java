package tech.fastj.partyhousecore;

import tech.fastj.network.rpc.commands.Command;

import java.util.UUID;

public class Commands {
    public static final Command.Id ClientJoined = new Command.Id("Client Joined", UUID.fromString("45567674-7baf-4c2c-b4a2-2f10633ec146"));
    public static final Command.Id ClientLeft = new Command.Id("Client Left", UUID.fromString("452d7d49-3b2d-4721-8f42-7dcad7bd9647"));
    public static final Command.Id UpdateClientInfo = new Command.Id("Update Client Name", UUID.fromString("3439fdae-dbac-49d1-9bbc-30ffed0ffab2"));
    public static final Command.Id UpdateClientGameState = new Command.Id("Update Client Game State", UUID.fromString("a98001a6-61f9-4224-89f8-d0d7c09f3f0b"));
}
