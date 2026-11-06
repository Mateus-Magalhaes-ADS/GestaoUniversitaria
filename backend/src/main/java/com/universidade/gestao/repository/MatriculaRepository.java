package com.universidade.gestao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.universidade.gestao.model.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    boolean existsByCpf(String cpf);

    Optional<Matricula> findByCpf(String cpf);

    List<Matricula> findAllByEmailIgnoreCase(String email);

    List<Matricula> findByCursoId(Long cursoId);

    default List<Matricula> findAllByEmail(String email) {
        return findAllByEmailIgnoreCase(email == null ? "" : email.trim());
    }
}
