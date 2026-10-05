package com.universidade.gestao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomSuccessHandler customSuccessHandler;

    public SecurityConfig(CustomSuccessHandler customSuccessHandler) {
        this.customSuccessHandler = customSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Adicione /matricula às rotas públicas liberadas com permitAll()
                .requestMatchers("/", "/login", "/registrar", "/matricula", "/css/**", "/js/**").permitAll()
                
                .requestMatchers("/aluno/dashboard").hasRole("ALUNO")
                .requestMatchers("/professor/dashboard").hasRole("PROFESSOR")
                .requestMatchers("/coordenador/dashboard").hasRole("COORDENADOR")
                .requestMatchers("/atendente/dashboard").hasRole("ATENDENTE")
                .requestMatchers("/matricula/*/documento/*").hasAnyRole("ALUNO", "ATENDENTE", "COORDENADOR")
            
                // Mantenha apenas o envio (/matricula/enviar) restrito, se aplicável, ou ajuste conforme a sua regra
                .requestMatchers("/matricula/enviar").hasRole("ALUNO")
                
                .requestMatchers("/prova/nova", "/prova/professor", "/prova/*/abrir", "/prova/*/encerrar",
                        "/prova/*/corrigir", "/prova/*/corrigir/*").hasRole("PROFESSOR")
                .requestMatchers("/prova/coordenacao", "/prova/coordenacao/*", "/prova/*/publicar").hasRole("COORDENADOR")
                .requestMatchers("/prova/disponiveis", "/prova/*/responder", "/prova/resultados").hasRole("ALUNO")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout.permitAll());

        return http.build();
    }
}
