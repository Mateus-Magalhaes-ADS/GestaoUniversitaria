package com.universidade.gestao.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidade.gestao.model.Matricula;
import com.universidade.gestao.model.Role;
import com.universidade.gestao.repository.MatriculaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardAccessIntegrationTest {

    private static final String DATABASE_FILE = "target/integration-tests/universidade-" + UUID.randomUUID() + ".json.enc";

    @DynamicPropertySource
    static void configureEncryptedDatabase(DynamicPropertyRegistry registry) {
        registry.add("app.data.file", () -> DATABASE_FILE);
        registry.add("APP_DATA_ENCRYPTION_KEY", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        registry.add("APP_BOOTSTRAP_STAFF_EMAIL", () -> "coordenador@example.com");
        registry.add("APP_BOOTSTRAP_STAFF_PASSWORD", () -> "senha-coordenador-teste");
        registry.add("APP_BOOTSTRAP_STAFF_NAME", () -> "Coordenação Teste");
        registry.add("APP_BOOTSTRAP_STAFF_ROLE", () -> "COORDENADOR");
        registry.add("APP_PROFESSOR_REGISTRATION_CODE", () -> "convite-professor-teste");
        registry.add("APP_COORDINATOR_REGISTRATION_CODE", () -> "convite-coordenador-teste");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() throws IOException {
        Path path = Paths.get(DATABASE_FILE);
        if (Files.exists(path)) {
            Files.deleteIfExists(path);
        }
    }

    @Test
    @SuppressWarnings("null")
    void loginAfterFollowingEnrollmentLinkReturnsToEnrollmentPage() throws Exception {
        String email = "aluno-link-" + UUID.randomUUID() + "@example.com";
        String password = "senha-segura-de-teste";
        usuarioRepository.save(new com.universidade.gestao.model.Usuario(
                "Aluno Link", email, passwordEncoder.encode(password), Role.ALUNO));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/matricula\"")));

        MvcResult requestResult = mockMvc.perform(get("/matricula"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) requestResult.getRequest().getSession(false);
        assertNotNull(session);

        MvcResult loginResult = mockMvc.perform(post("/login").session(session).with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrlPattern("http://localhost/matricula*"))
                .andReturn();
        MockHttpSession authenticatedSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(authenticatedSession);

        mockMvc.perform(get("/matricula").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Solicitação de Matrícula")))
                .andExpect(content().string(containsString(email)));
    }

    @Test
    @SuppressWarnings("null")
    void studentCanSeeOnlyOwnEnrollmentAndCannotOpenStaffDashboard() throws Exception {
        String email = "aluno-" + UUID.randomUUID() + "@example.com";
        String password = "senha-segura-de-teste";

        mockMvc.perform(get("/registrar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Professor")))
                .andExpect(content().string(containsString("Coordenador")))
                .andExpect(content().string(containsString("codigoConvite")));

        String rejeitadoEmail = "sem-convite-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nome", "Sem Convite")
                        .param("email", rejeitadoEmail)
                        .param("senha", password)
                        .param("perfil", "COORDENADOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrar?erro=convite"));
        org.junit.jupiter.api.Assertions.assertTrue(usuarioRepository.findByEmail(rejeitadoEmail).isEmpty());

        String professorEmail = "professor-convite-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nome", "Professor Convidado")
                        .param("email", professorEmail)
                        .param("senha", password)
                        .param("perfil", "PROFESSOR")
                        .param("codigoConvite", "convite-coordenador-teste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrar?erro=convite"));
        org.junit.jupiter.api.Assertions.assertTrue(usuarioRepository.findByEmail(professorEmail).isEmpty());

        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nome", "Professor Convidado")
                        .param("email", professorEmail)
                        .param("senha", password)
                        .param("perfil", "PROFESSOR")
                        .param("codigoConvite", "convite-professor-teste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?sucesso"));
        assertEquals(Role.PROFESSOR, usuarioRepository.findByEmail(professorEmail).orElseThrow().getPerfil());

        String coordenadorEmail = "coordenador-convite-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nome", "Coordenador Convidado")
                        .param("email", coordenadorEmail)
                        .param("senha", password)
                        .param("perfil", "COORDENADOR")
                        .param("codigoConvite", "convite-coordenador-teste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?sucesso"));
        assertEquals(Role.COORDENADOR, usuarioRepository.findByEmail(coordenadorEmail).orElseThrow().getPerfil());

        // Cadastro sem perfil explícito continua criando uma conta de aluno.
        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nome", "Ana Souza")
                        .param("email", email)
                        .param("senha", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?sucesso"));

        var usuario = usuarioRepository.findByEmail(email).orElseThrow();
        assertEquals(Role.ALUNO, usuario.getPerfil());
        assertNotNull(usuario.getId());
        assertEquals(Role.COORDENADOR,
                usuarioRepository.findByEmail("coordenador@example.com").orElseThrow().getPerfil());

        // 2. Realiza o Login com a conta de aluno recém-criada
        MvcResult loginResult = mockMvc.perform(post("/login").with(csrf())
                        .param("username", email)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/aluno/dashboard"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        // 3. Submete a solicitação de matrícula com upload de documentos
        byte[] identityBytes = "documento confidencial".getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(multipart("/matricula/enviar")
                        .file(new MockMultipartFile("documentoIdentidade", "identidade.pdf", "application/pdf", identityBytes))
                        .file(new MockMultipartFile("historicoEscolar", "historico.pdf", "application/pdf", "historico".getBytes(StandardCharsets.UTF_8)))
                        .param("cpf", "12345678900")
                        .param("endereco", "Rua de teste, 123")
                        .param("cep", "01000-000")
                        .param("cursoId", "1")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matricula"));

        Matricula matricula = matriculaRepository.findAllByEmail(email).get(0);
        assertEquals("Ana Souza", matricula.getNomeCompleto());
        assertEquals("Rua de teste, 123", matricula.getEndereco());
        assertEquals("01000-000", matricula.getCep());

        // 4. Valida acesso às visões permitidas do aluno
        mockMvc.perform(get("/aluno/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("12345678900")))
                .andExpect(content().string(containsString("Rua de teste, 123")));

        mockMvc.perform(get("/matricula").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Souza")))
                .andExpect(content().string(containsString(email)));

        // 5. Valida bloqueio de segurança: Aluno tenta acessar dashboard do Coordenador -> 403 Forbidden
        mockMvc.perform(get("/coordenador/dashboard").session(session))
                .andExpect(status().isForbidden());

        // 6. Aluno baixa seu próprio documento anexado
        mockMvc.perform(get("/matricula/{id}/documento/identidade", matricula.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().bytes(identityBytes));

        // 7. Login com a conta de Coordenador e verificação da visualização dos dados do aluno
        MvcResult staffLoginResult = mockMvc.perform(post("/login").with(csrf())
                        .param("username", "coordenador@example.com")
                        .param("password", "senha-coordenador-teste"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/coordenador/dashboard"))
                .andReturn();
        MockHttpSession staffSession = (MockHttpSession) staffLoginResult.getRequest().getSession(false);
        assertNotNull(staffSession);

        mockMvc.perform(get("/coordenador/dashboard").session(staffSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("12345678900")));
    }
}
