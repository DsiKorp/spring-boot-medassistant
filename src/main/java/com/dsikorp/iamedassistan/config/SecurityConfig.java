package com.dsikorp.iamedassistan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http){

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement( session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                        )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/**").hasAnyRole("PATIENT", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer( auth2 ->
                        auth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        )
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter(){
        var converter = new JwtAuthenticationConverter();

//        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
//            var realmAccess = (Map<String, Object>) jwt.getClaim("realm_access");
//
//            if(realmAccess==null) return List.of();
//
//            var roles = (List<String>) realmAccess.get("roles");
//
//            if (roles==null) return List.of();
//
//            return roles.stream()
//                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//                    .collect(Collectors.toList());
//        });
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) return List.of();

            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof List<?> roles)) return List.of();

            return roles.stream()
                    .filter(role -> role instanceof String)
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });

        return converter;
    }
}
