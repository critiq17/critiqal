package org.acme.api.dtos;

public record UpdateProfileRequest(String name, String bio, String avatarUrl) {}