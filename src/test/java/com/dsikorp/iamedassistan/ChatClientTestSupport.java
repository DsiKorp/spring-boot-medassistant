package com.dsikorp.iamedassistan;

import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Utilidad compartida por los tests unitarios de los agentes.
 *
 * <p>Los agentes arman su propio {@code ChatClient.create(...)} en tiempo de ejecución
 * (no usan los beans configurados en {@code AssistantConfig}), así que no hay ningún bean que
 * inyectar en el test: hay que interceptar el método estático con
 * {@code mockStatic(ChatClient.class)}.
 *
 * <p>Este helper construye un {@link ChatClient} mock cuyas llamadas encadenadas devuelven
 * siempre el <em>mismo</em> spec de request. Eso es clave: de este modo cualquier cadena
 * (system → user → advisors → tools → toolContext → call) llega al mismo punto final, sin
 * importar qué métodos intermedios invoque la clase bajo prueba.
 *
 * <p>Todos los stubs son {@code lenient()} porque no todos los agentes usan
 * advisors/tools/toolContext y el modo estricto de Mockito fallaría por stubbing sin uso.
 */
public final class ChatClientTestSupport {

    private ChatClientTestSupport() {
    }

    /**
     * @param client  el cliente mock que se devolverá desde {@code ChatClient.create(...)}
     * @param request el spec de request, para verificar el system/user prompt renderizado
     * @param call    el spec de respuesta, para stubear {@code content()} o {@code entity(Class)}
     */
    public record Mock(ChatClient client,
                       ChatClient.ChatClientRequestSpec request,
                       ChatClient.CallResponseSpec call) {
    }

    /**
     * Crea el mock de {@link ChatClient} con la cadena fluida encadenable.
     */
    @SuppressWarnings("unchecked")
    public static Mock mockChatClient() {
        ChatClient client = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);

        lenient().when(client.prompt()).thenReturn(request);
        lenient().when(request.system(anyString())).thenReturn(request);
        lenient().when(request.user(anyString())).thenReturn(request);
        lenient().when(request.advisors(any(Consumer.class))).thenReturn(request);
        lenient().when(request.tools(any(Object[].class))).thenReturn(request);
        lenient().when(request.toolContext(anyMap())).thenReturn(request);
        lenient().when(request.call()).thenReturn(call);

        return new Mock(client, request, call);
    }
}
