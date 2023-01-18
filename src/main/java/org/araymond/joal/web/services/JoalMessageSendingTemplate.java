package org.araymond.joal.web.services;

import lombok.RequiredArgsConstructor;
import org.araymond.joal.web.annotations.ConditionalOnWebUi;
import org.araymond.joal.web.messages.outgoing.MessagePayload;
import org.araymond.joal.web.messages.outgoing.StompMessage;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * Created by raymo on 29/06/2017.
 */
@ConditionalOnWebUi
@RequiredArgsConstructor
@Service
public class JoalMessageSendingTemplate {

    private final SimpMessageSendingOperations messageSendingOperations;

    public void convertAndSend(final String destination, final MessagePayload payload) throws MessagingException {
        final StompMessage stompMessage = StompMessage.wrap(payload);
        messageSendingOperations.convertAndSend(destination, stompMessage);
    }

}
