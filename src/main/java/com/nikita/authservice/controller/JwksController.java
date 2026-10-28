package com.nikita.authservice.controller;

import com.nikita.authservice.service.JwtService;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {
    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks(){
        return ResponseEntity.ok(jwtService.getJwks());
    }
}
