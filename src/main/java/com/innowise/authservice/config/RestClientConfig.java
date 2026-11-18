package com.innowise.authservice.config;

import com.innowise.authservice.role.Role;
import com.innowise.authservice.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class RestClientConfig {
    @Value("${application.client.user-service.url}")
    private String userServiceUrl;
    @Bean
    public RestClient userServiceRestClient(){
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl(userServiceUrl)
                .requestFactory(factory)
                .build();
    }
    @Bean
    public Supplier<String> tokenProvider(JwtService jwtService){
        return ()->jwtService.generateAccessToken(0L, Role.ADMIN);
    }
}
