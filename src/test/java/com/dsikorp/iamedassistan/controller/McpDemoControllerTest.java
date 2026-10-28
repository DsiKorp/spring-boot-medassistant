package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.ChatClientTestSupport;
import com.dsikorp.iamedassistan.MockMvcTestSupport;
import com.dsikorp.iamedassistan.config.ClientResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class McpDemoControllerTest {

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private ToolCallbackProvider mcpTools;

    @Mock
    private ChatModel chatModel;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.standalone(new McpDemoController(clientResolver, mcpTools)).build();
    }

    @AfterEach
    void tearDown() {
        MockMvcTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("el endpoint de demo devuelve la respuesta del modelo")
    void demoReturnsModelResponse() throws Exception {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Respuesta con tools MCP");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            mockMvc.perform(post("/api/v1/mcp/demo")
                            .with(MockMvcTestSupport.asPatient(7L))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"prompt\":\"busca algo\",\"model\":\"gemini\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Respuesta con tools MCP"));

            verify(mock.request()).tools(mcpTools);
        }
    }
}
