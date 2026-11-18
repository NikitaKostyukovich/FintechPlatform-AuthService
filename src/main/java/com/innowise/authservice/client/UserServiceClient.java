package com.innowise.authservice.client;

import com.innowise.authservice.dto.request.RegisterRequestDto;
import com.innowise.authservice.dto.request.UserCreateRequestDto;
import com.innowise.authservice.dto.response.AuthResponseDto;
import com.innowise.authservice.exception.UserAlreadyExistsException;
import com.innowise.authservice.exception.UserServiceBadRequestException;
import com.innowise.authservice.exception.UserServiceException;
import com.innowise.authservice.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

@Component
public class UserServiceClient {

    private final RestClient restClient;
    private final Supplier<String> tokenProvider;
   @Autowired
    public UserServiceClient(RestClient restClient, Supplier<String> tokenProvider) {
       this.tokenProvider = tokenProvider;
       this.restClient = restClient;
    }

    public AuthResponseDto createUser(UserCreateRequestDto requestDto) {
        try {
            return restClient.post()
                    .uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.get())
                    .body(requestDto)
                    .retrieve()
                    .body(AuthResponseDto.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 409) {
                throw new UserAlreadyExistsException("Email is already registered in User Service");
            }
            if (e.getStatusCode().value() == 400) {
                throw new UserServiceBadRequestException("Invalid data: " + e.getResponseBodyAsString());
            }
            throw new UserServiceException("User service returned error status: " + e.getStatusCode());

        }catch (RestClientException e) {
            throw new UserServiceException("User service request failed: " + e.getMessage());
        }
    }

    public void deleteUser(Long userId) {
       try {
           restClient.delete()
                   .uri("/api/users/{id}", userId)
                   .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.get())
                   .retrieve()
                   .toBodilessEntity();
       }catch(RestClientException e){
           throw new UserServiceException("User service request failed: " + e.getMessage());
       }
    }
}