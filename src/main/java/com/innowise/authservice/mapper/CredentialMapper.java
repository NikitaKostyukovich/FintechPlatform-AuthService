package com.innowise.authservice.mapper;

import com.innowise.authservice.dto.request.RegisterRequestDto;
import com.innowise.authservice.entity.Credential;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CredentialMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "login", source = "dto.login")
    @Mapping(target = "role", constant = "USER")
    Credential toEntity(RegisterRequestDto dto, Long userId, String passwordHash);
}
