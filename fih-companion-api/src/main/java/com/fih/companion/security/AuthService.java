package com.fih.companion.security;

import com.fih.companion.domain.Utilisateur;
import com.fih.companion.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    public AuthService(UtilisateurRepository utilisateurRepository, JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Utilisateur user = utilisateurRepository.findByUsername(request.username())
                .orElseThrow(AuthService::unauthorized);

        if (!isAdmin(user) || !passwordMatches(request.password(), user.getPassword())) {
            throw unauthorized();
        }

        String displayName = buildDisplayName(user);
        String token = jwtService.generate(user.getUsername(), user.getRole(), displayName);
        return new LoginResponse(token, user.getRole(), displayName);
    }

    private boolean isAdmin(Utilisateur user) {
        return Boolean.TRUE.equals(user.getAdmin())
                || "Administrateur".equalsIgnoreCase(user.getRole());
    }

    // The ONE place passwords are compared.
    // TODO security: legacy plaintext passwords — the legacy DB stores passwords
    // in plain text and this system is strictly read-only, so we compare as-is.
    // Do NOT hash, salt, or write anything here.
    private boolean passwordMatches(String submitted, String stored) {
        return stored != null && stored.equals(submitted);
    }

    private String buildDisplayName(Utilisateur user) {
        String first = user.getFirstname() == null ? "" : user.getFirstname().trim();
        String last = user.getLastname() == null ? "" : user.getLastname().trim();
        String name = (first + " " + last).trim();
        return name.isEmpty() ? user.getUsername() : name;
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }
}
