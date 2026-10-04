package com.dsikorp.iamedassistan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    // Se usa RestClient en ves de WebClient por que el servicio es bloqueante,
    // WebClient es reactivo, todos los repositorios jpa son sincronos.
    @Bean
    public RestClient openFdaClient(){
        return RestClient.builder()
                .baseUrl("https://api.fda.gov")
                .build();
    }
}
