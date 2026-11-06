package com.universidade.gestao.controller.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.UsuarioRepository;

@RestController
@RequestMapping("/api")
public class AuthApiController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String professorCode;
    private final String coordinatorCode;

    public AuthApiController(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            @Value("${APP_PROFESSOR_REGISTRATION_CODE:}") String professorCode,
            @Value("${APP_COORDINATOR_REGISTRATION_CODE:}") String coordinatorCode) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.professorCode = professorCode;
        this.coordinatorCode = coordinatorCode;
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody RegistrationRequest request) {
        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, e-mail e senha são obrigatórios.");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Role role;
        try {
            role = request.profile() == null || request.profile().isBlank()
                    ? Role.ALUNO
                    : Role.valueOf(request.profile().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil inválido.");
        }
        if (role == Role.ATENDENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O perfil de atendente não pode ser criado publicamente.");
        }
        if (role != Role.ALUNO && !validInvite(role, request.inviteCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Código de convite inválido.");
        }
        if (usuarios.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }

        Usuario usuario = usuarios.save(new Usuario(
                request.name().trim(), email, passwordEncoder.encode(request.password()), role));
        return Map.of("id", usuario.getId(), "name", usuario.getNome(),
                "email", usuario.getEmail(), "profile", usuario.getPerfil().name());
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("email", jwt.getSubject(),
                "roles", jwt.getClaimAsStringList("roles") == null
                        ? java.util.List.of() : jwt.getClaimAsStringList("roles"));
    }

    private boolean validInvite(Role role, String suppliedCode) {
        String configured = role == Role.PROFESSOR ? professorCode : coordinatorCode;
        if (configured == null || configured.isBlank() || suppliedCode == null || suppliedCode.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                configured.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8),
                suppliedCode.trim().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    public record RegistrationRequest(String name, String email, String password, String profile, String inviteCode) {}
}
