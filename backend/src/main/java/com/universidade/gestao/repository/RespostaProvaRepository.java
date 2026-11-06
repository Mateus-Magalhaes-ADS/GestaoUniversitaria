package com.universidade.gestao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.universidade.gestao.model.RespostaProva;

public interface RespostaProvaRepository extends JpaRepository<RespostaProva, Long> {

    List<RespostaProva> findByProvaId(Long provaId);

    List<RespostaProva> findByAlunoEmailIgnoreCase(String email);

    Optional<RespostaProva> findByProvaIdAndAlunoEmailIgnoreCase(Long provaId, String email);

    default Optional<RespostaProva> findByProvaIdAndAlunoEmail(Long provaId, String email) {
        return findByProvaIdAndAlunoEmailIgnoreCase(provaId, email == null ? "" : email.trim());
    }

    default List<RespostaProva> findByAlunoEmail(String email) {
        return findByAlunoEmailIgnoreCase(email == null ? "" : email.trim());
    }
}
