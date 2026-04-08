package org.acme.api.dtos;

public record AuthResponse(String token, UserDTO user) {}
