package tech.fastj.partyhousecore;

import tech.fastj.network.serial.Message;

public record ClientPoints(ClientInfo clientInfo, int points) implements Message, Comparable<ClientPoints> {
    @Override
    public int compareTo(ClientPoints o) {
        return Integer.compare(o.points, points);
    }
}
