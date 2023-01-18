package org.araymond.joal.core.ttorrent.client.announcer.response;

import com.turn.ttorrent.common.protocol.TrackerMessage.AnnounceRequestMessage.RequestEvent;
import lombok.extern.slf4j.Slf4j;
import org.araymond.joal.core.ttorrent.client.announcer.Announcer;
import org.araymond.joal.core.ttorrent.client.announcer.exceptions.TooMuchAnnouncesFailedInARawException;
import org.araymond.joal.core.ttorrent.client.announcer.request.SuccessAnnounceResponse;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AnnounceResponseHandlerChain implements AnnounceResponseCallback {
    private final List<AnnounceResponseHandlerChainElement> chainElements;

    public AnnounceResponseHandlerChain() {
        chainElements = new ArrayList<>(4);
    }

    public void appendHandler(final AnnounceResponseHandlerChainElement element) {
        this.chainElements.add(element);
    }

    @Override
    public void onAnnounceWillAnnounce(final RequestEvent event, final Announcer announcer) {
        for (final AnnounceResponseHandlerChainElement element : chainElements) {
            element.onAnnouncerWillAnnounce(announcer, event);
        }
    }

    @Override
    public void onAnnounceSuccess(final RequestEvent event, final Announcer announcer, final SuccessAnnounceResponse result) {
        for (final AnnounceResponseHandlerChainElement element : chainElements) {
            switch (event) {
                case STARTED: {
                    element.onAnnounceStartSuccess(announcer, result);
                    break;
                }
                case NONE: {
                    element.onAnnounceRegularSuccess(announcer, result);
                    break;
                }
                case STOPPED: {
                    element.onAnnounceStopSuccess(announcer, result);
                    break;
                }
                default: {
                    log.warn("Event {} cannot be handled by {}", event.getEventName(), getClass().getSimpleName());
                    break;
                }
            }
        }
    }

    @Override
    public void onAnnounceFailure(final RequestEvent event, final Announcer announcer, final Throwable throwable) {
        for (final AnnounceResponseHandlerChainElement element : chainElements) {
            switch (event) {
                case STARTED: {
                    element.onAnnounceStartFails(announcer, throwable);
                    break;
                }
                case NONE: {
                    element.onAnnounceRegularFails(announcer, throwable);
                    break;
                }
                case STOPPED: {
                    element.onAnnounceStopFails(announcer, throwable);
                    break;
                }
                default: {
                    log.warn("Event {} cannot be handled by {}", event.getEventName(), getClass().getSimpleName());
                    break;
                }
            }
        }
    }

    @Override
    public void onTooManyAnnounceFailedInARaw(final RequestEvent event, final Announcer announcer, final TooMuchAnnouncesFailedInARawException e) {
        for (final AnnounceResponseHandlerChainElement chainElement : chainElements) {
            chainElement.onTooManyAnnounceFailedInARaw(announcer, e);
        }
    }
}
