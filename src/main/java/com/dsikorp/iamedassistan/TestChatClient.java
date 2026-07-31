package com.dsikorp.iamedassistan;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;

//@Component
@RequiredArgsConstructor
public class TestChatClient implements CommandLineRunner {

    private final ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception {
        String response = chatClient
                .prompt("¿Qué es un agujero negro?")
                .call()
                .content();

        System.out.println(response);

    }
}










