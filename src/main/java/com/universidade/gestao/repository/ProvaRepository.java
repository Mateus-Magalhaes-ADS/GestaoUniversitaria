package com.universidade.gestao.repository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.universidade.gestao.model.Prova;
import com.universidade.gestao.model.Questao;
import com.universidade.gestao.model.StatusProva;
import com.universidade.gestao.service.EncryptedJsonDatabaseService;

@Repository
public class ProvaRepository {

    private final EncryptedJsonDatabaseService database;

    public ProvaRepository(EncryptedJsonDatabaseService database) {
        this.database = database;
    }

    public Prova save(Prova prova) {
        return database.update(state -> {
            if (prova.getId() == null) {
                prova.setId(state.nextProvaId++);
            } else {
                state.provas.removeIf(existing -> Objects.equals(existing.getId(), prova.getId()));
            }
            for (Questao questao : prova.getQuestoes()) {
                if (questao.getId() == null) {
                    questao.setId(state.nextQuestaoId++);
                }
            }
            state.provas.add(prova);
            return prova;
        });
    }

    public Optional<Prova> findById(Long id) {
        return database.read(state -> state.provas.stream()
                .filter(prova -> Objects.equals(prova.getId(), id))
                .findFirst());
    }

    public List<Prova> findAll() {
        return database.read(state -> List.copyOf(state.provas));
    }

    public List<Prova> findByProfessorEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return database.read(state -> state.provas.stream()
                .filter(prova -> normalizeEmail(prova.getProfessorEmail()).equals(normalizedEmail))
                .toList());
    }

    public List<Prova> findByStatus(StatusProva status) {
        return database.read(state -> state.provas.stream()
                .filter(prova -> prova.getStatus() == status)
                .toList());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
