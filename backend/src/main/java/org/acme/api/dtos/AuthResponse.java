package org.acme.api.dtos;

public record AuthResponse(String token, UserDTO user) {}
public record RegisterRequest(String username, String password) {}
public record LoginRequest(String username, String password) {}
