package com.universidade.gestao.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidade.gestao.model.Matricula;
import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.Usuario;

class EncryptedJsonDatabaseServiceTest {

    private static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String WRONG_ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            "abcdef0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8));

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesSensitiveDataAndDocumentsEncryptedAndReadsThemBack() throws Exception {
        Path databaseFile = temporaryDirectory.resolve("universidade.json.enc");
        EncryptedJsonDatabaseService database = createDatabase(databaseFile, ENCRYPTION_KEY);
        byte[] identityDocument = new byte[] { 12, 34, 56, 78 };

        database.update(state -> {
            Usuario usuario = new Usuario("Ana Souza", "ana@example.com", "bcrypt-hash", Role.ALUNO);
            usuario.setId(1L);
            state.usuarios.add(usuario);

            Matricula matricula = new Matricula("Ana Souza", "ana@example.com", "12345678900", 1L);
            matricula.setId(1L);
            matricula.setEndereco("Rua de teste, 123");
            matricula.setCep("01000-000");
            matricula.setDocumentoIdentidadeNome("identidade.pdf");
            matricula.setDocumentoIdentidadeContentType("application/pdf");
            matricula.setDocumentoIdentidadeConteudo(identityDocument);
            state.matriculas.add(matricula);
            return null;
        });

        String encryptedFile = Files.readString(databaseFile);
        assertFalse(encryptedFile.contains("Ana Souza"));
        assertFalse(encryptedFile.contains("12345678900"));
        assertFalse(encryptedFile.contains("Rua de teste"));

        EncryptedJsonDatabaseService.DatabaseState recovered = database.read(state -> state);
        assertEquals("Ana Souza", recovered.usuarios.get(0).getNome());
        assertEquals("12345678900", recovered.matriculas.get(0).getCpf());
        assertEquals("Rua de teste, 123", recovered.matriculas.get(0).getEndereco());
        assertArrayEquals(identityDocument, recovered.matriculas.get(0).getDocumentoIdentidadeConteudo());
    }

    @Test
    void rejectsAnIncorrectEncryptionKey() {
        Path databaseFile = temporaryDirectory.resolve("universidade.json.enc");
        createDatabase(databaseFile, ENCRYPTION_KEY).update(state -> null);
        EncryptedJsonDatabaseService databaseWithWrongKey = createDatabase(databaseFile, WRONG_ENCRYPTION_KEY);

        assertThrows(IllegalStateException.class, () -> databaseWithWrongKey.read(state -> state));
    }

    private EncryptedJsonDatabaseService createDatabase(Path file, String key) {
        return new EncryptedJsonDatabaseService(new ObjectMapper().findAndRegisterModules(), file.toString(), key);
    }
}
