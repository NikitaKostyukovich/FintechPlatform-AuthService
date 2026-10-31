package com.innowise.authservice.controller;

import com.innowise.authservice.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JwksController {

    private static final Logger log = LoggerFactory.getLogger(JwksController.class);

    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> getJwks(){
        log.info("Received GET request to /.well-known/jwks.json to fetch public keys");
        return ResponseEntity.ok(jwtService.getJwks());
    }
}
