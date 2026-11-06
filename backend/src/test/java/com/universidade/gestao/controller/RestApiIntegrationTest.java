package com.universidade.gestao.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.universidade.gestao.model.Matricula;
import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.MatriculaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class RestApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private MatriculaRepository matriculas;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void publicRegistrationCreatesAccountWithoutReturningPassword() throws Exception {
        String email = "rest-" + java.util.UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"API Student","email":"%s","password":"a-secure-password"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.profile").value("ALUNO"))
                .andExpect(content().string(not(containsString("a-secure-password"))));

        Usuario saved = usuarios.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("a-secure-password", saved.getSenha()));
    }

    @Test
    void studentApiListsOnlyTheirOwnEnrollment() throws Exception {
        String email = "owner-" + java.util.UUID.randomUUID() + "@example.com";
        usuarios.save(new Usuario("Owner Student", email, passwordEncoder.encode("password"), Role.ALUNO));
        usuarios.save(new Usuario("Other Student", "other-" + java.util.UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("password"), Role.ALUNO));
        Matricula owned = enrollment("Owner Student", email, "12345678901", 1L);
        matriculas.save(owned);
        String otherEmail = "other-" + java.util.UUID.randomUUID() + "@example.com";
        matriculas.save(enrollment("Other Student", otherEmail, "10987654321", 2L));

        mockMvc.perform(get("/api/v1/matriculas")
                        .with(jwt().jwt(token -> token.subject(email))
                                .authorities(new SimpleGrantedAuthority("ROLE_ALUNO"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(email)))
                .andExpect(content().string(not(containsString("other-"))));
    }

    private Matricula enrollment(String name, String email, String cpf, Long courseId) {
        Matricula matricula = new Matricula(name, email, cpf, courseId);
        matricula.setEndereco("Rua de teste, 10");
        matricula.setCep("01000-000");
        return matricula;
    }

    @Test
    void apiRequiresBearerAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oauth2OpenIdConfigurationIsPubliclyDiscoverable() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.code_challenge_methods_supported[0]").value("S256"));
    }
}
