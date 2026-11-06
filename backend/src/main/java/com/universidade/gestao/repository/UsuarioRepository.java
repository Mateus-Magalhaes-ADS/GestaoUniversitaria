package com.universidade.gestao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findByPerfil(Role perfil);

    boolean existsByEmailIgnoreCase(String email);

    default Optional<Usuario> findByEmail(String email) {
        return findByEmailIgnoreCase(email == null ? "" : email.trim());
    }

    default boolean existsByEmail(String email) {
        return existsByEmailIgnoreCase(email == null ? "" : email.trim());
    }
}
