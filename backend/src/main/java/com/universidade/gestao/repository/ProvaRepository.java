package com.universidade.gestao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.universidade.gestao.model.Prova;
import com.universidade.gestao.model.StatusProva;

public interface ProvaRepository extends JpaRepository<Prova, Long> {

    List<Prova> findByProfessorEmailIgnoreCase(String email);

    List<Prova> findByStatus(StatusProva status);

    default List<Prova> findByProfessorEmail(String email) {
        return findByProfessorEmailIgnoreCase(email == null ? "" : email.trim());
    }
}
