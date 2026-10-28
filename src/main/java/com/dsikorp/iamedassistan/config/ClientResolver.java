package com.dsikorp.iamedassistan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;

public interface ClientResolver {
    ChatClient resolve(String model);

    ChatModel resolveModel(String model);

    VectorStore resolveVectorStore(String model);



}