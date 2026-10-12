package com.dsikorp.iamedassistan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
public class MedicalAuditAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userMessage = request.prompt().getUserMessage().getText();
        String conversationId = (String) request.context()
                .getOrDefault(ChatMemory.CONVERSATION_ID, "unknown");

        log.info("AUDIT | conversationId={} | pregunta={}",
                conversationId, userMessage);

        long start = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);
        long elapsed = System.currentTimeMillis() - start;

        String content = response.chatResponse().getResult().getOutput().getText();
        String preview = content != null && content.length() > 100
                ? content.substring(0, 100) + "..."
                : content;
        log.info("AUDIT | conversationId={} | tiempo={}ms | respuesta={}",
                conversationId, elapsed, preview);

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {

        String userMessage = request.prompt().getUserMessage().getText();
        String conversationId = (String) request.context()
                .getOrDefault(ChatMemory.CONVERSATION_ID, "unknown");

        log.info("AUDIT STREAM | conversationId={} | pregunta={}",
                conversationId, userMessage);

        return chain.nextStream(request);
    }

    @Override
    public String getName() {
        return "MedicalAuditAdvisor";
    }

    @Override
    public int getOrder() {
        //return 0;  // muestra los documentos en el log
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
