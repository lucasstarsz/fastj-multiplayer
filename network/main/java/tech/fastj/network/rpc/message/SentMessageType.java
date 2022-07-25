package tech.fastj.network.rpc.message;

public enum SentMessageType {
    KeepAlive,
    Disconnect,
    PingRequest,
    PingResponse,
    RPCCommand,
    Request
}
