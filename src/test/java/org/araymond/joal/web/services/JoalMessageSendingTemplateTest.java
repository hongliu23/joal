package org.araymond.joal.web.services;

import org.araymond.joal.web.messages.outgoing.StompMessage;
import org.araymond.joal.web.messages.outgoing.StompMessageTypes;
import org.araymond.joal.web.messages.outgoing.impl.global.state.GlobalSeedStoppedPayload;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class JoalMessageSendingTemplateTest {

    @Test
    public void shouldWrapMessageAndSend() {
        final SimpMessageSendingOperations sendingOperations = mock(SimpMessageSendingOperations.class);
        final JoalMessageSendingTemplate joalMessageSendingTemplate = new JoalMessageSendingTemplate(sendingOperations);

        final GlobalSeedStoppedPayload payload = new GlobalSeedStoppedPayload();
        joalMessageSendingTemplate.convertAndSend("/test", payload);

        final ArgumentCaptor<StompMessage> captor = ArgumentCaptor.forClass(StompMessage.class);

        verify(sendingOperations, times(1)).convertAndSend(eq("/test"), captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(StompMessageTypes.GLOBAL_SEED_STOPPED);
        assertThat(captor.getValue().getPayload()).isEqualTo(payload);
    }

}
