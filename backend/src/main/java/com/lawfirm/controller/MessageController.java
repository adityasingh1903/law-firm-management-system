package com.lawfirm.controller;

import com.lawfirm.dto.MessageDto;
import com.lawfirm.dto.SendMessageRequest;
import com.lawfirm.service.ClientService;
import com.lawfirm.service.LawyerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Handles real-time messages over WebSocket (STOMP).
 *
 * Flow:
 *   Angular  →  /app/chat.client.send  OR  /app/chat.lawyer.send
 *   Server saves via existing service (same validation as REST)
 *   Server broadcasts  →  /topic/case.{caseId}
 *   Both parties receive instantly
 */
@Controller
public class MessageController {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private ClientService         clientService;
    @Autowired private LawyerService         lawyerService;

    /** Client sends a message → Angular destination: /app/chat.client.send */
    @MessageMapping("/chat.client.send")
    public void clientSend(@Payload SendMessageRequest request, Principal principal) {
        MessageDto saved = clientService.sendMessage(principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/case." + saved.getCaseId(), saved);
    }

    /** Lawyer sends a message → Angular destination: /app/chat.lawyer.send */
    @MessageMapping("/chat.lawyer.send")
    public void lawyerSend(@Payload SendMessageRequest request, Principal principal) {
        MessageDto saved = lawyerService.sendMessage(principal.getName(), request);
        messagingTemplate.convertAndSend("/topic/case." + saved.getCaseId(), saved);
    }
}