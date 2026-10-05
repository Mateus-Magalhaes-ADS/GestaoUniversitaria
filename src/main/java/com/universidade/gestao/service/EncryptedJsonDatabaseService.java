package com.universidade.gestao.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidade.gestao.model.Matricula;
import com.universidade.gestao.model.Prova;
import com.universidade.gestao.model.RespostaProva;
import com.universidade.gestao.model.Usuario;

@Service
public class EncryptedJsonDatabaseService {

    private static final int IV_LENGTH = 12;
    private static final int AUTHENTICATION_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 32;
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    private final ObjectMapper objectMapper;
    private final Path databasePath;
    private final SecretKeySpec encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptedJsonDatabaseService(
            ObjectMapper objectMapper,
            @Value("${app.data.file:data/universidade.json.enc}") String filePath,
            @Value("${APP_DATA_ENCRYPTION_KEY:}") String encodedKey) {
        this.objectMapper = objectMapper;
        this.databasePath = Paths.get(filePath).toAbsolutePath().normalize();

        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException("Configure APP_DATA_ENCRYPTION_KEY com uma chave Base64 de 32 bytes.");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
            if (keyBytes.length != KEY_LENGTH) {
                throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY deve decodificar para exatamente 32 bytes.");
            }
            this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY precisa estar codificada em Base64.", exception);
        }
    }

    public synchronized <T> T read(Function<DatabaseState, T> query) {
        return query.apply(load());
    }

    public synchronized <T> T update(Function<DatabaseState, T> operation) {
        DatabaseState state = load();
        T result = operation.apply(state);
        save(state);
        return result;
    }

    private DatabaseState load() {
        if (!Files.exists(databasePath)) {
            return new DatabaseState();
        }

        try {
            EncryptedEnvelope envelope = objectMapper.readValue(databasePath.toFile(), EncryptedEnvelope.class);
            if (envelope.version != 1) {
                throw new IOException("Versão de banco não suportada.");
            }
            byte[] iv = Base64.getDecoder().decode(envelope.iv);
            byte[] ciphertext = Base64.getDecoder().decode(envelope.ciphertext);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(AUTHENTICATION_TAG_LENGTH, iv));
            DatabaseState state = objectMapper.readValue(cipher.doFinal(ciphertext), DatabaseState.class);
            if (state.usuarios == null) {
                state.usuarios = new ArrayList<>();
            }
            if (state.matriculas == null) {
                state.matriculas = new ArrayList<>();
            }
            if (state.provas == null) {
                state.provas = new ArrayList<>();
            }
            if (state.respostasProva == null) {
                state.respostasProva = new ArrayList<>();
            }
            return state;
        } catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Não foi possível ler o banco JSON criptografado. Verifique a chave e a integridade do arquivo.", exception);
        }
    }

    private void save(DatabaseState state) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(AUTHENTICATION_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(objectMapper.writeValueAsBytes(state));

            EncryptedEnvelope envelope = new EncryptedEnvelope();
            envelope.version = 1;
            envelope.iv = Base64.getEncoder().encodeToString(iv);
            envelope.ciphertext = Base64.getEncoder().encodeToString(ciphertext);

            Path parent = databasePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temporaryPath = databasePath.resolveSibling(databasePath.getFileName() + ".tmp");
            objectMapper.writeValue(temporaryPath.toFile(), envelope);
            try {
                Files.move(temporaryPath, databasePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryPath, databasePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível salvar o banco JSON criptografado.", exception);
        }
    }

    public static class DatabaseState {
        public long nextUsuarioId = 1;
        public long nextMatriculaId = 1;
        public long nextProvaId = 1;
        public long nextQuestaoId = 1;
        public long nextRespostaProvaId = 1;
        public List<Usuario> usuarios = new ArrayList<>();
        public List<Matricula> matriculas = new ArrayList<>();
        public List<Prova> provas = new ArrayList<>();
        public List<RespostaProva> respostasProva = new ArrayList<>();
    }

    private static class EncryptedEnvelope {
        public int version;
        public String iv;
        public String ciphertext;
    }
}