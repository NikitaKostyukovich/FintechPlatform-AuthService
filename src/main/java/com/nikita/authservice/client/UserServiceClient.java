package com.nikita.authservice.client;

import com.nikita.authservice.dto.RegisterRequestDto;
import com.nikita.authservice.dto.AuthResponseDto;
import com.nikita.authservice.exception.UserServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserServiceClient {

    private final RestClient restClient;

   @Autowired
    public UserServiceClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public AuthResponseDto createUser(RegisterRequestDto requestDto) {
        try {
            return restClient.post()
                    .uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestDto)
                    .retrieve()
                    .body(AuthResponseDto.class);
        } catch (RestClientException e) {
            throw new UserServiceException("User service request failed: " + e.getMessage());
        }
    }

    public void deleteUser(Long userId) {
       try {
           restClient.delete()
                   .uri("/api/users/{id}", userId)
                   .retrieve()
                   .toBodilessEntity();
       }catch(RestClientException e){
           throw new UserServiceException("User service request failed: " + e.getMessage());
       }
    }
}