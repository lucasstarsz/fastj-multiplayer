package tech.fastj.partyhousecore;

import java.util.UUID;

public record SnowballHitInfo(UUID clientId, UUID snowballId) {
    public SnowballHitInfo(SnowballInfo snowballInfo) {
        this(snowballInfo.clientInfo().clientId(), snowballInfo.snowballId());
    }
}
