package com.dsikorp.iamedassistan.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros de {@link MedicalAuditAdvisor}: el advisor debe delegar en la cadena
 * y devolver exactamente la respuesta producida por ella.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalAuditAdvisor")
class MedicalAuditAdvisorTest {

    @Mock
    private CallAdvisorChain callChain;
    @Mock
    private StreamAdvisorChain streamChain;

    private final MedicalAuditAdvisor advisor = new MedicalAuditAdvisor();

    private static ChatClientRequest requestWithConversationId() {
        return new ChatClientRequest(
                new Prompt("sintoma de prueba"),
                Map.of(ChatMemory.CONVERSATION_ID, "user-1"));
    }

    private static ChatClientRequest requestWithoutConversationId() {
        return new ChatClientRequest(
                new Prompt("sintoma de prueba"),
                Map.of());
    }

    private static ChatClientResponse modelResponse() {
        ChatResponse chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("respuesta del modelo"))))
                .build();
        return new ChatClientResponse(chatResponse, Map.of());
    }

    @Test
    @DisplayName("adviseCall devuelve la respuesta exacta de la cadena")
    void adviseCallReturnsChainResponse() {
        ChatClientRequest request = requestWithConversationId();
        ChatClientResponse response = modelResponse();
        when(callChain.nextCall(request)).thenReturn(response);

        ChatClientResponse result = advisor.adviseCall(request, callChain);

        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("adviseCall invoca la cadena exactamente una vez")
    void adviseCallInvokesChainOnce() {
        ChatClientRequest request = requestWithConversationId();
        when(callChain.nextCall(request)).thenReturn(modelResponse());

        advisor.adviseCall(request, callChain);

        verify(callChain, times(1)).nextCall(request);
    }

    @Test
    @DisplayName("adviseCall funciona cuando la peticion no trae conversation id")
    void adviseCallWithoutConversationIdStillReturnsChainResponse() {
        ChatClientRequest request = requestWithoutConversationId();
        ChatClientResponse response = modelResponse();
        when(callChain.nextCall(request)).thenReturn(response);

        ChatClientResponse result = advisor.adviseCall(request, callChain);

        assertThat(result).isSameAs(response);
        verify(callChain, times(1)).nextCall(request);
    }

    @Test
    @DisplayName("adviseStream devuelve el Flux exacto de la cadena")
    void adviseStreamReturnsChainFlux() {
        ChatClientRequest request = requestWithConversationId();
        Flux<ChatClientResponse> flux = Flux.just(modelResponse());
        when(streamChain.nextStream(request)).thenReturn(flux);

        Flux<ChatClientResponse> result = advisor.adviseStream(request, streamChain);

        assertThat(result).isSameAs(flux);
    }

    @Test
    @DisplayName("adviseStream invoca la cadena exactamente una vez")
    void adviseStreamInvokesChainOnce() {
        ChatClientRequest request = requestWithConversationId();
        when(streamChain.nextStream(request)).thenReturn(Flux.just(modelResponse()));

        advisor.adviseStream(request, streamChain);

        verify(streamChain, times(1)).nextStream(request);
    }

    @Test
    @DisplayName("adviseStream funciona cuando la peticion no trae conversation id")
    void adviseStreamWithoutConversationIdStillReturnsChainFlux() {
        ChatClientRequest request = requestWithoutConversationId();
        Flux<ChatClientResponse> flux = Flux.just(modelResponse());
        when(streamChain.nextStream(request)).thenReturn(flux);

        Flux<ChatClientResponse> result = advisor.adviseStream(request, streamChain);

        assertThat(result).isSameAs(flux);
        verify(streamChain, times(1)).nextStream(request);
    }

    @Test
    @DisplayName("getName devuelve el nombre del advisor")
    void getNameReturnsAdvisorName() {
        assertThat(advisor.getName()).isEqualTo("MedicalAuditAdvisor");
    }

    @Test
    @DisplayName("getOrder devuelve la precedencia mas alta")
    void getOrderReturnsHighestPrecedence() {
        assertThat(advisor.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
