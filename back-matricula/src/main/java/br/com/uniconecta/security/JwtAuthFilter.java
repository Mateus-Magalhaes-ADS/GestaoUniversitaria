package br.com.uniconecta.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lê o cabeçalho "Authorization: Bearer <token>" e, se o token for válido,
 * registra o id do aluno como usuário autenticado da requisição.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.validarToken(header.substring(7)).ifPresent(alunoId -> {
                var auth = new UsernamePasswordAuthenticationToken(
                        alunoId, null, List.of(new SimpleGrantedAuthority("ROLE_ALUNO")));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
