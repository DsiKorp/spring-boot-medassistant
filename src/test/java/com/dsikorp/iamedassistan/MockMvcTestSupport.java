package com.dsikorp.iamedassistan;

import com.dsikorp.iamedassistan.exception.GlobalExceptionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Utilidad compartida por los tests de los controllers.
 *
 * <p>Los controllers leen el {@code userId} del claim {@code userId} del JWT, así que el test
 * necesita que exista un principal {@link Jwt} resuelto por el
 * {@link AuthenticationPrincipalArgumentResolver} (el mismo que usa Spring Security en
 * producción) y, además, un {@link Authentication} para el parámetro homónimo.
 *
 * <p>Se usa {@code standaloneSetup} en lugar de un slice {@code @WebMvcTest} para no arrastrar
 * el auto-config del resource server (que exige el issuer de Keycloak) ni el contexto completo
 * de la aplicación: estos tests son unitarios y deben correr sin dependencias externas.
 */
public final class MockMvcTestSupport {

    private MockMvcTestSupport() {
    }

    /**
     * Construye un {@link Jwt} mínimo con el claim {@code userId}.
     */
    public static Jwt jwtFor(Long userId) {
        return Jwt.withTokenValue("token-de-prueba")
                .header("alg", "none")
                .claim("userId", userId)
                .build();
    }

    /**
     * Request post-processor que autentica la petición como un paciente con el userId dado.
     * Popula tanto el {@code SecurityContextHolder} (lo consume el
     * {@code AuthenticationPrincipalArgumentResolver}) como el principal de la request
     * (lo consume el resolver de tipo {@code Authentication}).
     */
    public static RequestPostProcessor asPatient(Long userId) {
        return request -> {
            Jwt jwt = jwtFor(userId);
            Authentication authentication = new JwtAuthenticationToken(
                    jwt, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            request.setUserPrincipal(authentication);
            return request;
        };
    }

    /**
     * Limpia el contexto de seguridad entre tests (es un {@code ThreadLocal}).
     */
    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Builder de MockMvc standalone con el resolver de principal y el handler global de errores.
     */
    public static StandaloneMockMvcBuilder standalone(Object... controllers) {
        return standaloneSetup(controllers)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler());
    }
}
