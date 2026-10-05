package com.universidade.gestao.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidade.gestao.model.Prova;
import com.universidade.gestao.model.Questao;
import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.StatusProva;
import com.universidade.gestao.model.TipoQuestao;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.ProvaRepository;
import com.universidade.gestao.repository.RespostaProvaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ProvaWorkflowIntegrationTest {

    private static final String DATABASE_FILE = "target/integration-tests/provas-" + UUID.randomUUID() + ".json.enc";

    @DynamicPropertySource
    static void configureEncryptedDatabase(DynamicPropertyRegistry registry) {
        registry.add("app.data.file", () -> DATABASE_FILE);
        registry.add("APP_DATA_ENCRYPTION_KEY", () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProvaRepository provaRepository;

    @Autowired
    private RespostaProvaRepository respostaProvaRepository;

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
    void alunoRespondeProfessorCorrigeECoordenadorPublicaAProva() throws Exception {
        String professorEmail = "professor-" + UUID.randomUUID() + "@example.com";
        String professorSenha = "senha-professor-teste";
        usuarioRepository.save(new Usuario("Prof. Ana", professorEmail, passwordEncoder.encode(professorSenha), Role.PROFESSOR));

        String coordenadorEmail = "coordenador-" + UUID.randomUUID() + "@example.com";
        String coordenadorSenha = "senha-coordenador-teste";
        usuarioRepository.save(new Usuario("Coord. Bia", coordenadorEmail, passwordEncoder.encode(coordenadorSenha), Role.COORDENADOR));

        String alunoEmail = "aluno-" + UUID.randomUUID() + "@example.com";
        String alunoSenha = "senha-aluno-teste";
        mockMvc.perform(post("/registrar").with(csrf())
                        .param("nome", "Aluno Carlos")
                        .param("email", alunoEmail)
                        .param("senha", alunoSenha))
                .andExpect(status().is3xxRedirection());

        MockHttpSession professorSession = autenticar(professorEmail, professorSenha, "/professor/dashboard");
        MockHttpSession alunoSession = autenticar(alunoEmail, alunoSenha, "/aluno/dashboard");
        MockHttpSession coordenadorSession = autenticar(coordenadorEmail, coordenadorSenha, "/coordenador/dashboard");

        // 1. Professor cria a prova com seis questões objetivas e uma dissertativa
        mockMvc.perform(post("/prova/nova").with(csrf()).session(professorSession)
                        .param("titulo", "Prova de Estruturas de Dados")
                        .param("disciplina", "Algoritmos")
                        .param("enunciado", "Questão objetiva 1", "Questão objetiva 2", "Questão objetiva 3",
                                "Questão objetiva 4", "Questão objetiva 5", "Questão objetiva 6", "Explique recursão.")
                        .param("tipo", "OBJETIVA", "OBJETIVA", "OBJETIVA", "OBJETIVA", "OBJETIVA", "OBJETIVA", "DISSERTATIVA")
                        .param("valor", "1", "1", "1", "1", "1", "1", "4")
                        .param("alternativas", "A1\nB1", "A2\nB2", "A3\nB3", "A4\nB4", "A5\nB5", "A6\nB6", "")
                        .param("alternativaCorreta", "0", "0", "0", "0", "0", "0", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prova/professor"));

        Prova provaCriada = provaRepository.findByProfessorEmail(professorEmail).get(0);
        assertEquals(StatusProva.RASCUNHO, provaCriada.getStatus());
        assertEquals(7, provaCriada.getQuestoes().size());
        Long provaId = provaCriada.getId();
        List<Questao> questoesObjetivas = provaCriada.getQuestoes().stream()
                .filter(questao -> questao.getTipo() == TipoQuestao.OBJETIVA)
                .toList();
        Questao questaoDissertativa = provaCriada.getQuestoes().get(6);
        assertEquals(6, questoesObjetivas.size());
        assertEquals(TipoQuestao.DISSERTATIVA, questaoDissertativa.getTipo());

        // 2. Professor abre a prova para respostas
        mockMvc.perform(post("/prova/{id}/abrir", provaId).with(csrf()).session(professorSession))
                .andExpect(status().is3xxRedirection());
        assertEquals(StatusProva.ABERTA, provaRepository.findById(provaId).orElseThrow().getStatus());

        // 3. Aluno vê a prova disponível e responde
        mockMvc.perform(get("/prova/disponiveis").session(alunoSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Prova de Estruturas de Dados")));

        mockMvc.perform(get("/prova/{id}/responder", provaId).session(alunoSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Questão 1")))
                .andExpect(content().string(containsString("Questão 7")))
                .andExpect(content().string(containsString("Explique recursão.")));

                var envioRespostas = post("/prova/{id}/responder", provaId).with(csrf()).session(alunoSession);
                for (Questao questao : questoesObjetivas) {
                        envioRespostas.param("alternativa_" + questao.getId(), "0");
                }
                mockMvc.perform(envioRespostas
                                                .param("texto_" + questaoDissertativa.getId(), "Recursão é quando uma função chama a si mesma."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/prova/disponiveis"));

        // 4. Aluno não pode responder a mesma prova de novo
        mockMvc.perform(get("/prova/{id}/responder", provaId).session(alunoSession))
                .andExpect(status().isForbidden());

        // 5. Aluno não pode acessar áreas do professor
        mockMvc.perform(get("/prova/professor").session(alunoSession))
                .andExpect(status().isForbidden());

        // 6. Professor encerra e corrige a prova
        mockMvc.perform(post("/prova/{id}/encerrar", provaId).with(csrf()).session(professorSession))
                .andExpect(status().is3xxRedirection());

        List<Long> respostaIds = respostaProvaRepository.findByProvaId(provaId).stream().map(r -> r.getId()).toList();
        assertEquals(1, respostaIds.size());
        Long respostaId = respostaIds.get(0);

        var formularioCorrecao = post("/prova/{id}/corrigir/{respostaId}", provaId, respostaId)
                .with(csrf()).session(professorSession);
        for (Questao questao : questoesObjetivas) {
            formularioCorrecao.param("nota_" + questao.getId(), "1");
            formularioCorrecao.param("comentario_" + questao.getId(), "Resposta correta.");
        }
        mockMvc.perform(formularioCorrecao
                        .param("nota_" + questaoDissertativa.getId(), "3")
                        .param("comentario_" + questaoDissertativa.getId(), "Boa definição, faltou um exemplo."))
                .andExpect(status().is3xxRedirection());

        var respostaCorrigida = respostaProvaRepository.findById(respostaId).orElseThrow();
        assertTrue(respostaCorrigida.isCorrigida());
        assertEquals(9.0, respostaCorrigida.getNotaFinal());
        assertEquals(StatusProva.CORRIGIDA, provaRepository.findById(provaId).orElseThrow().getStatus());

        // 7. Aluno ainda não vê a nota antes da publicação
        mockMvc.perform(get("/prova/resultados").session(alunoSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Prova de Estruturas de Dados"))));

        // 8. Coordenador não pode publicar antes de existir correção completa de outra prova, mas pode publicar esta
        mockMvc.perform(get("/prova/coordenacao").session(coordenadorSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Prova de Estruturas de Dados")));

        mockMvc.perform(post("/prova/{id}/publicar", provaId).with(csrf()).session(coordenadorSession))
                .andExpect(status().is3xxRedirection());

        assertEquals(StatusProva.PUBLICADA, provaRepository.findById(provaId).orElseThrow().getStatus());
        assertTrue(respostaProvaRepository.findById(respostaId).orElseThrow().isPublicada());

        // 9. Agora o aluno vê a nota publicada
        mockMvc.perform(get("/prova/resultados").session(alunoSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Prova de Estruturas de Dados")))
                .andExpect(content().string(containsString("9.0")));
    }

    private MockHttpSession autenticar(String email, String senha, String urlEsperada) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/login").with(csrf())
                        .param("username", email)
                        .param("password", senha))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(urlEsperada))
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        assertNotNull(sessao);
        return sessao;
    }
}
