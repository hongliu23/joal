package org.araymond.joal.core.events.global.state;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.araymond.joal.core.client.emulated.BitTorrentClient;

@Getter
public class GlobalSeedStartedEvent {
    private final BitTorrentClient bitTorrentClient;

    public GlobalSeedStartedEvent(final BitTorrentClient bitTorrentClient) {
        Preconditions.checkNotNull(bitTorrentClient, "BitTorrentClient cannot be null");
        this.bitTorrentClient = bitTorrentClient;
    }
}
