module fastj.network {
    requires transitive org.slf4j;

    exports tech.fastj.network.config;

    exports tech.fastj.network.rpc;
    exports tech.fastj.network.rpc.classes;
    exports tech.fastj.network.rpc.local;
    exports tech.fastj.network.rpc.local.command;
    exports tech.fastj.network.rpc.message;
    exports tech.fastj.network.rpc.message.prebuilt;
    exports tech.fastj.network.rpc.server;
    exports tech.fastj.network.rpc.server.command;

    exports tech.fastj.network.serial;
    exports tech.fastj.network.serial.util;
    exports tech.fastj.network.serial.read;
    exports tech.fastj.network.serial.write;
}