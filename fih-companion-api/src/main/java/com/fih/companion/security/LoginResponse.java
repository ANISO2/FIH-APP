package com.fih.companion.security;

public record LoginResponse(String token, String role, String displayName) {
}
