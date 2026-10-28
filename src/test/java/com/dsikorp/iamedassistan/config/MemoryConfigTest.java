package com.dsikorp.iamedassistan.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros de {@link MemoryConfig}: la memoria debe construirse como
 * {@link MessageWindowChatMemory} y delegar la persistencia en el repositorio inyectado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MemoryConfig")
class MemoryConfigTest {

    @Mock
    private ChatMemoryRepository chatMemoryRepository;

    private MemoryConfig memoryConfig;

    @BeforeEach
    void setUp() {
        memoryConfig = new MemoryConfig();
    }

    @Test
    @DisplayName("El bean chatMemory se crea como MessageWindowChatMemory")
    void chatMemoryBeanIsMessageWindowChatMemory() {
        ChatMemory chatMemory = memoryConfig.chatMemory(chatMemoryRepository);

        assertThat(chatMemory).isNotNull();
        assertThat(chatMemory).isInstanceOf(MessageWindowChatMemory.class);
    }

    @Test
    @DisplayName("La memoria lee la conversacion almacenada en el repositorio")
    void chatMemoryReadsStoredConversationFromRepository() {
        Message storedMessage = new UserMessage("hola");
        when(chatMemoryRepository.findByConversationId("conversation-1")).thenReturn(List.of(storedMessage));

        ChatMemory chatMemory = memoryConfig.chatMemory(chatMemoryRepository);
        List<Message> messages = chatMemory.get("conversation-1");

        assertThat(messages).isNotNull();
        assertThat(messages).hasSize(1);
        assertThat(messages).contains(storedMessage);
        assertThat(messages.get(0).getText()).isEqualTo("hola");
        verify(chatMemoryRepository).findByConversationId("conversation-1");
    }

    @Test
    @DisplayName("La memoria persiste las conversations nuevas en el repositorio")
    void chatMemoryPersistsNewMessagesInRepository() {
        when(chatMemoryRepository.findByConversationId("conversation-2")).thenReturn(List.of());

        ChatMemory chatMemory = memoryConfig.chatMemory(chatMemoryRepository);
        chatMemory.add("conversation-2", List.of(new UserMessage("hola")));

        verify(chatMemoryRepository).saveAll(eq("conversation-2"), anyList());
    }
}
