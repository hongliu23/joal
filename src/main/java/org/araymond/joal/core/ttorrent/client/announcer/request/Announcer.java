package org.araymond.joal.core.ttorrent.client.announcer.request;

import com.google.common.base.Objects;
import com.turn.ttorrent.client.announce.AnnounceException;
import com.turn.ttorrent.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;
import org.araymond.joal.core.ttorent.client.announce.exceptions.TooMuchAnnouncesFailedInARawException;
import org.araymond.joal.core.ttorrent.client.announcer.tracker.NewTrackerClient;
import org.araymond.joal.core.torrent.torrent.InfoHash;
import org.araymond.joal.core.torrent.torrent.MockedTorrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Announcer {
    private static final Logger logger = LoggerFactory.getLogger(Announcer.class);

    private int lastKnownInterval = 10;
    private final MockedTorrent torrent;
    private final NewTrackerClient trackerClient;
    private final AnnounceDataAccessor announceDataAccessor;

    public Announcer(final MockedTorrent torrent, final AnnounceDataAccessor announceDataAccessor) {
        this.torrent = torrent;
        this.trackerClient = new NewTrackerClient(torrent);
        this.announceDataAccessor = announceDataAccessor;
    }

    public SuccessAnnounceResponse announce(final RequestEvent event) throws AnnounceException, TooMuchAnnouncesFailedInARawException {
        final SuccessAnnounceResponse responseMessage = this.trackerClient.announce(
                this.announceDataAccessor.getHttpRequestQueryForTorrent(this.torrent.getTorrentInfoHash(), event),
                this.announceDataAccessor.getHttpHeadersForTorrent()
        );

        this.lastKnownInterval = responseMessage.getInterval();

        return responseMessage;
    }

    public MockedTorrent getTorrent() {
        return torrent;
    }

    public int getLastKnownInterval() {
        return lastKnownInterval;
    }

    public InfoHash getTorrentInfoHash() {
        return this.getTorrent().getTorrentInfoHash();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Announcer announcer = (Announcer) o;
        return Objects.equal(this.getTorrentInfoHash(), announcer.getTorrentInfoHash());
    }

    @Override
    public int hashCode() {
        return this.getTorrentInfoHash().hashCode();
    }
}
