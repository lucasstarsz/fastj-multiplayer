package tech.fastj.network.rpc.message;

public enum SentMessageType {
    KeepAlive,
    Disconnect,
    PingRequest,
    PingResponse,
    LobbyUpdate,
    SessionUpdate,
    AvailableLobbiesUpdate,
    RPCCommand,
    Request
}
