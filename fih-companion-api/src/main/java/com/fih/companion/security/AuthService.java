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
    private final SecurityProperties securityProperties;

    public AuthService(UtilisateurRepository utilisateurRepository, JwtService jwtService,
                       SecurityProperties securityProperties) {
        this.utilisateurRepository = utilisateurRepository;
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    public LoginResponse login(LoginRequest request) {

        LoginResponse invitationsLogin = tryInvitationsAccount(request);
        if (invitationsLogin != null) {
            return invitationsLogin;
        }

        Utilisateur user = utilisateurRepository.findByUsername(request.username())
                .orElseThrow(AuthService::unauthorized);

        if (!isAdmin(user) || !passwordMatches(request.password(), user.getPassword())) {
            throw unauthorized();
        }

        String displayName = buildDisplayName(user);
        String token = jwtService.generate(user.getUsername(), user.getRole(), displayName);
        return new LoginResponse(token, user.getRole(), displayName);
    }


    private LoginResponse tryInvitationsAccount(LoginRequest request) {
        SecurityProperties.InvitationsAccount acc = securityProperties.getInvitationsAccount();
        if (acc == null || !acc.isConfigured() || request == null) {
            return null;
        }
        String submittedUser = request.username() == null ? "" : request.username().trim();
        boolean userMatches = submittedUser.equalsIgnoreCase(acc.getUsername().trim());
        if (!userMatches) {
            return null;
        }
        if (!passwordMatches(request.password(), acc.getPassword())) {
            throw unauthorized();
        }
        String displayName = acc.getDisplayName() == null || acc.getDisplayName().isBlank()
                ? acc.getUsername() : acc.getDisplayName();
        String token = jwtService.generate(acc.getUsername(), Roles.INVITATIONS_CLAIM, displayName);
        return new LoginResponse(token, Roles.INVITATIONS_CLAIM, displayName);
    }

    private boolean isAdmin(Utilisateur user) {
        return Boolean.TRUE.equals(user.getAdmin())
               || "Administrateur".equalsIgnoreCase(user.getRole());
        //return "Administrateur".equalsIgnoreCase(user.getRole());

    }


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
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides.");
    }
}
