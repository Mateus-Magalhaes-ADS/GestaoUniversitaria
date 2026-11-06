package com.universidade.gestao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.UsuarioRepository;

@Component
public class StaffAccountBootstrap implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;
    private final String roleName;

    public StaffAccountBootstrap(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${APP_BOOTSTRAP_STAFF_EMAIL:}") String email,
            @Value("${APP_BOOTSTRAP_STAFF_PASSWORD:}") String password,
            @Value("${APP_BOOTSTRAP_STAFF_NAME:Equipe}") String name,
            @Value("${APP_BOOTSTRAP_STAFF_ROLE:}") String roleName) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
        this.roleName = roleName;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Configure APP_BOOTSTRAP_STAFF_PASSWORD para criar a conta de equipe.");
        }

        Role role;
        try {
            role = Role.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IllegalStateException("APP_BOOTSTRAP_STAFF_ROLE deve ser PROFESSOR, ATENDENTE ou COORDENADOR.", exception);
        }
        if (role == Role.ALUNO) {
            throw new IllegalStateException("A conta de equipe não pode usar o perfil ALUNO.");
        }

        usuarioRepository.findByEmail(email).ifPresentOrElse(usuario -> {
            if (usuario.getPerfil() != role) {
                throw new IllegalStateException("O e-mail da conta de equipe já está registrado com outro perfil.");
            }
        }, () -> usuarioRepository.save(new Usuario(name, email,
                passwordEncoder.encode(password), role)));
    }
}
