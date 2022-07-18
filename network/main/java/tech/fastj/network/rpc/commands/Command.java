package tech.fastj.network.rpc.commands;

import java.util.UUID;

public interface Command {

    static Id named(String name) {
        return new Id(name);
    }

    record Id(String name, UUID uuid) {
        public Id(String name) {
            this(name, UUID.randomUUID());
        }

        public String formattedToString() {
            return "\"" + name + "\":" + uuid;
        }
    }
}
