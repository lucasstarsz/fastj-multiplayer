module fastj.network {
    requires transitive fastj.library;

    exports tech.fastj.network.client;
    exports tech.fastj.network.server;
    exports tech.fastj.network.serial;
    exports tech.fastj.network.serial.util;
    exports tech.fastj.network.serial.read;
    exports tech.fastj.network.serial.write;
}