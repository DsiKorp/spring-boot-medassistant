package com.dsikorp.iamedassistan.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemoryConfig {

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository){
    //ChatMemory chatMemory(){
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository) // memoria persistente
                .maxMessages(10)
                .build();
    }
}
