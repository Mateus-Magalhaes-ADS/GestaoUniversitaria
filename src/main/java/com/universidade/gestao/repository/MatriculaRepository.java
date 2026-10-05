package com.universidade.gestao.repository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.universidade.gestao.model.Matricula;
import com.universidade.gestao.service.EncryptedJsonDatabaseService;

@Repository
public class MatriculaRepository {

    private final EncryptedJsonDatabaseService database;

    public MatriculaRepository(EncryptedJsonDatabaseService database) {
        this.database = database;
    }

    public Matricula save(Matricula matricula) {
        return database.update(state -> {
            if (matricula.getId() == null) {
                matricula.setId(state.nextMatriculaId++);
            } else {
                state.matriculas.removeIf(existing -> Objects.equals(existing.getId(), matricula.getId()));
            }
            state.matriculas.add(matricula);
            return matricula;
        });
    }

    public boolean existsByCpf(String cpf) {
        String normalizedCpf = normalizeCpf(cpf);
        return database.read(state -> state.matriculas.stream()
                .anyMatch(matricula -> normalizeCpf(matricula.getCpf()).equals(normalizedCpf)));
    }

    public Optional<Matricula> findByCpf(String cpf) {
        String normalizedCpf = normalizeCpf(cpf);
        return database.read(state -> state.matriculas.stream()
                .filter(matricula -> normalizeCpf(matricula.getCpf()).equals(normalizedCpf))
                .findFirst());
    }

    public Optional<Matricula> findById(Long id) {
        return database.read(state -> state.matriculas.stream()
                .filter(matricula -> Objects.equals(matricula.getId(), id))
                .findFirst());
    }

    public List<Matricula> findAll() {
        return database.read(state -> List.copyOf(state.matriculas));
    }

    public List<Matricula> findAllByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return database.read(state -> state.matriculas.stream()
                .filter(matricula -> normalizeEmail(matricula.getEmail()).equals(normalizedEmail))
                .toList());
    }

    public List<Matricula> findByCursoId(Long cursoId) {
        return database.read(state -> state.matriculas.stream()
                .filter(matricula -> Objects.equals(matricula.getCursoId(), cursoId))
                .toList());
    }

    private String normalizeCpf(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("[^0-9]", "");
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
