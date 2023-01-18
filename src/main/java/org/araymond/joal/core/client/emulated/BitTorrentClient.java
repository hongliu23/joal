package org.araymond.joal.core.client.emulated;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.turn.ttorrent.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.araymond.joal.core.bandwith.TorrentSeedStats;
import org.araymond.joal.core.client.emulated.generator.UrlEncoder;
import org.araymond.joal.core.client.emulated.generator.key.KeyGenerator;
import org.araymond.joal.core.client.emulated.generator.numwant.NumwantProvider;
import org.araymond.joal.core.client.emulated.generator.peerid.PeerIdGenerator;
import org.araymond.joal.core.exception.UnrecognizedClientPlaceholder;
import org.araymond.joal.core.torrent.torrent.InfoHash;
import org.araymond.joal.core.ttorrent.client.ConnectionHandler;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.araymond.joal.core.client.emulated.BitTorrentClientConfig.HttpHeader;

/**
 * Created by raymo on 26/01/2017.
 */
@EqualsAndHashCode(exclude = "urlEncoder")
public class BitTorrentClient {
    private final PeerIdGenerator peerIdGenerator;
    private final KeyGenerator keyGenerator;
    private final UrlEncoder urlEncoder;
    @Getter private final String query;
    private final List<Map.Entry<String, String>> headers;
    private final NumwantProvider numwantProvider;

    BitTorrentClient(final PeerIdGenerator peerIdGenerator, final KeyGenerator keyGenerator, final UrlEncoder urlEncoder, final String query, final Collection<HttpHeader> headers, final NumwantProvider numwantProvider) {
        Preconditions.checkNotNull(peerIdGenerator, "peerIdGenerator cannot be null or empty");
        Preconditions.checkNotNull(urlEncoder, "urlEncoder cannot be null");
        Preconditions.checkArgument(!StringUtils.isBlank(query), "query cannot be null or empty");
        Preconditions.checkNotNull(headers, "headers cannot be null");
        Preconditions.checkNotNull(numwantProvider, "numwantProvider cannot be null");
        this.peerIdGenerator = peerIdGenerator;
        this.urlEncoder = urlEncoder;
        this.query = query;
        this.headers = headers.stream().map(h -> new AbstractMap.SimpleImmutableEntry<>(h.getName(), h.getValue())).collect(Collectors.toList());
        this.keyGenerator = keyGenerator;
        this.numwantProvider = numwantProvider;
    }

    public String getPeerId(final InfoHash infoHash, final RequestEvent event) {
        return peerIdGenerator.getPeerId(infoHash, event);
    }

    @VisibleForTesting
    Optional<String> getKey(final InfoHash infoHash, final RequestEvent event) {
        return ofNullable(keyGenerator)
                .map(keyGen -> keyGen.getKey(infoHash, event));
    }

    public List<Map.Entry<String, String>> getHeaders() {
        return ImmutableList.copyOf(headers);
    }

    @VisibleForTesting
    Integer getNumwant(final RequestEvent event) {
        return this.numwantProvider.get(event);
    }

    public String createRequestQuery(final RequestEvent event, final InfoHash torrentInfoHash, final TorrentSeedStats stats, final ConnectionHandler connectionHandler) {
        String emulatedClientQuery = this.getQuery()
                .replaceAll("\\{infohash}", urlEncoder.encode(torrentInfoHash.value()))
                .replaceAll("\\{uploaded}", String.valueOf(stats.getUploaded()))
                .replaceAll("\\{downloaded}", String.valueOf(stats.getDownloaded()))
                .replaceAll("\\{left}", String.valueOf(stats.getLeft()))
                .replaceAll("\\{port}", String.valueOf(connectionHandler.getPort()))
                .replaceAll("\\{numwant}", String.valueOf(this.getNumwant(event)));

        final String peerId = this.peerIdGenerator.isShouldUrlEncode()
                ? urlEncoder.encode(this.getPeerId(torrentInfoHash, event))
                : this.getPeerId(torrentInfoHash, event);
        emulatedClientQuery = emulatedClientQuery.replaceAll("\\{peerid}", peerId);

        // set ip or ipv6 then remove placeholders that were left empty
        if (connectionHandler.getIpAddress() instanceof Inet4Address) {
            emulatedClientQuery = emulatedClientQuery.replaceAll("\\{ip}", connectionHandler.getIpAddress().getHostAddress());
        } else if(connectionHandler.getIpAddress() instanceof Inet6Address) {
            emulatedClientQuery = emulatedClientQuery.replaceAll("\\{ipv6}", urlEncoder.encode(connectionHandler.getIpAddress().getHostAddress()));
        }
        emulatedClientQuery = emulatedClientQuery.replaceAll("[&]*[a-zA-Z0-9]+=\\{ipv6}", "");
        emulatedClientQuery = emulatedClientQuery.replaceAll("[&]*[a-zA-Z0-9]+=\\{ip}", "");

        if (event == null || event == RequestEvent.NONE) {
            // if event was NONE, remove the event from the query string
            emulatedClientQuery = emulatedClientQuery.replaceAll("([&]*[a-zA-Z0-9]+=\\{event})", "");
        } else {
            emulatedClientQuery = emulatedClientQuery.replaceAll("\\{event}", event.getEventName());
        }

        if (emulatedClientQuery.contains("{key}")) {
            final String key = this.getKey(torrentInfoHash, event).orElseThrow(() -> new IllegalStateException("Client request query contains 'key' but BitTorrentClient does not have a key."));
            emulatedClientQuery = emulatedClientQuery.replaceAll("\\{key}", urlEncoder.encode(key));
        }

        final Matcher matcher = Pattern.compile("(.*?\\{.*?})").matcher(emulatedClientQuery);
        if (matcher.find()) {
            final String unrecognizedPlaceHolder = matcher.group();
            throw new UnrecognizedClientPlaceholder("Placeholder " + unrecognizedPlaceHolder + " were not recognized while building announce URL.");
        }

        // we might have removed event, ipv6 or ipv6, some '&' might have remains, lets remove them.
        if (emulatedClientQuery.endsWith("&")) {
            emulatedClientQuery = emulatedClientQuery.substring(0, emulatedClientQuery.length() - 1);
        }
        if (emulatedClientQuery.startsWith("&")) {
            emulatedClientQuery = emulatedClientQuery.substring(1, emulatedClientQuery.length());
        }
        emulatedClientQuery = emulatedClientQuery.replaceAll("&{2,}", "&");
        return emulatedClientQuery;
    }

    public List<Map.Entry<String, String>> createRequestHeaders() {
        final List<Map.Entry<String, String>> headers = new ArrayList<>(this.headers.size() + 1);

        for (final Map.Entry<String, String> header : this.headers) {
            final String name = header.getKey();
            final String value = header.getValue()
                    .replaceAll("\\{java}", System.getProperty("java.version"))
                    .replaceAll("\\{os}", System.getProperty("os.name"))
                    .replaceAll("\\{locale}", Locale.getDefault().toLanguageTag());

            final Matcher matcher = Pattern.compile("(\\{.*?})").matcher(value);
            if (matcher.find()) {
                final String unrecognizedPlaceHolder = matcher.group();
                throw new UnrecognizedClientPlaceholder("Placeholder " + unrecognizedPlaceHolder + " were not recognized while building client Headers.");
            }

            headers.add(new AbstractMap.SimpleImmutableEntry<>(name, value));
        }
        return headers;
    }
}
