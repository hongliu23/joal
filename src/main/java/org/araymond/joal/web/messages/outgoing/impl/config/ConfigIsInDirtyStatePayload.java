package org.araymond.joal.web.messages.outgoing.impl.config;

import lombok.Getter;
import org.araymond.joal.core.config.AppConfiguration;
import org.araymond.joal.core.events.config.ConfigurationIsInDirtyStateEvent;
import org.araymond.joal.web.messages.outgoing.MessagePayload;

/**
 * Created by raymo on 09/07/2017.
 */
@Getter
public class ConfigIsInDirtyStatePayload implements MessagePayload {
    private final AppConfiguration config;

    public ConfigIsInDirtyStatePayload(final ConfigurationIsInDirtyStateEvent event) {
        this.config = event.getConfiguration();
    }
}
