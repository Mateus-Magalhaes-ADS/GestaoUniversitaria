package br.com.uniconecta.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.uniconecta.aluno.Aluno;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** Gera e valida os tokens JWT usados para manter o aluno logado. */
@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${uniconecta.jwt.secret}") String segredo,
                      @Value("${uniconecta.jwt.expiracao-minutos}") long expiracaoMinutos) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String gerarToken(Aluno aluno) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(aluno.getId()))
                .claim("nome", aluno.getNome())
                .claim("email", aluno.getEmail())
                .issuer("uniconecta-api")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    /** Retorna o id do aluno se o token for válido. */
    public Optional<Long> validarToken(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer("uniconecta-api")
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.of(Long.valueOf(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
