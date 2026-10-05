package com.universidade.gestao.repository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.universidade.gestao.model.RespostaProva;
import com.universidade.gestao.service.EncryptedJsonDatabaseService;

@Repository
public class RespostaProvaRepository {

    private final EncryptedJsonDatabaseService database;

    public RespostaProvaRepository(EncryptedJsonDatabaseService database) {
        this.database = database;
    }

    public RespostaProva save(RespostaProva resposta) {
        return database.update(state -> {
            if (resposta.getId() == null) {
                resposta.setId(state.nextRespostaProvaId++);
            } else {
                state.respostasProva.removeIf(existing -> Objects.equals(existing.getId(), resposta.getId()));
            }
            state.respostasProva.add(resposta);
            return resposta;
        });
    }

    public Optional<RespostaProva> findById(Long id) {
        return database.read(state -> state.respostasProva.stream()
                .filter(resposta -> Objects.equals(resposta.getId(), id))
                .findFirst());
    }

    public List<RespostaProva> findByProvaId(Long provaId) {
        return database.read(state -> state.respostasProva.stream()
                .filter(resposta -> Objects.equals(resposta.getProvaId(), provaId))
                .toList());
    }

    public Optional<RespostaProva> findByProvaIdAndAlunoEmail(Long provaId, String email) {
        String normalizedEmail = normalizeEmail(email);
        return database.read(state -> state.respostasProva.stream()
                .filter(resposta -> Objects.equals(resposta.getProvaId(), provaId)
                        && normalizeEmail(resposta.getAlunoEmail()).equals(normalizedEmail))
                .findFirst());
    }

    public List<RespostaProva> findByAlunoEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return database.read(state -> state.respostasProva.stream()
                .filter(resposta -> normalizeEmail(resposta.getAlunoEmail()).equals(normalizedEmail))
                .toList());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
