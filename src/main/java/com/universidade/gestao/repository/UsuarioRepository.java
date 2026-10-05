package com.universidade.gestao.repository;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.service.EncryptedJsonDatabaseService;

@Repository
public class UsuarioRepository {

    private final EncryptedJsonDatabaseService database;

    public UsuarioRepository(EncryptedJsonDatabaseService database) {
        this.database = database;
    }

    public Optional<Usuario> findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return database.read(state -> state.usuarios.stream()
                .filter(usuario -> normalizeEmail(usuario.getEmail()).equals(normalizedEmail))
                .findFirst());
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    public Usuario save(Usuario usuario) {
        return database.update(state -> {
            String normalizedEmail = normalizeEmail(usuario.getEmail());
            boolean emailAlreadyUsed = state.usuarios.stream()
                    .anyMatch(existing -> !Objects.equals(existing.getId(), usuario.getId())
                            && normalizeEmail(existing.getEmail()).equals(normalizedEmail));
            if (emailAlreadyUsed) {
                throw new IllegalArgumentException("Já existe uma conta cadastrada com este e-mail.");
            }

            if (usuario.getId() == null) {
                usuario.setId(state.nextUsuarioId++);
            } else {
                state.usuarios.removeIf(existing -> Objects.equals(existing.getId(), usuario.getId()));
            }
            state.usuarios.add(usuario);
            return usuario;
        });
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
