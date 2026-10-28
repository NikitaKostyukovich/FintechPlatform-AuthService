package com.nikita.authservice.mapper;

import com.nikita.authservice.dto.AuthResponseDto;
import com.nikita.authservice.dto.RegisterRequestDto;
import com.nikita.authservice.entity.Credential;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CredentialMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "login", source = "dto.login")
    @Mapping(target = "role", constant = "USER")
    Credential toEntity(RegisterRequestDto dto, Long userId, String passwordHash);
}
