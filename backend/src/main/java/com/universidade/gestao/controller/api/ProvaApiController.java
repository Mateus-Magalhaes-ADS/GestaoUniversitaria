package com.universidade.gestao.controller.api;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.universidade.gestao.model.Prova;
import com.universidade.gestao.model.Questao;
import com.universidade.gestao.model.RespostaProva;
import com.universidade.gestao.model.RespostaQuestao;
import com.universidade.gestao.model.Role;
import com.universidade.gestao.model.StatusProva;
import com.universidade.gestao.model.TipoQuestao;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.ProvaRepository;
import com.universidade.gestao.repository.RespostaProvaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/v1/provas")
public class ProvaApiController {

    private static final int OBJECTIVE_COUNT = 6;
    private final ProvaRepository provas;
    private final RespostaProvaRepository respostas;
    private final UsuarioRepository usuarios;

    public ProvaApiController(
            ProvaRepository provas,
            RespostaProvaRepository respostas,
            UsuarioRepository usuarios) {
        this.provas = provas;
        this.respostas = respostas;
        this.usuarios = usuarios;
    }

    @GetMapping
    public List<ExamResponse> list(@AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        List<Prova> list = switch (actor.getPerfil()) {
            case ALUNO -> provas.findByStatus(StatusProva.ABERTA);
            case PROFESSOR -> provas.findByProfessorEmail(actor.getEmail());
            case COORDENADOR -> provas.findAll();
            case ATENDENTE -> throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        };
        return list.stream().map(prova -> toResponse(prova, actor.getPerfil() != Role.ALUNO)).toList();
    }

    @PostMapping
    public ExamResponse create(@RequestBody CreateExamRequest request, @AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        if (actor.getPerfil() != Role.PROFESSOR && actor.getPerfil() != Role.COORDENADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (request.title() == null || request.title().isBlank()
                || request.subject() == null || request.subject().isBlank()
                || request.questions() == null || request.questions().size() != 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe título, disciplina e exatamente sete questões.");
        }
        Usuario professor = actor;
        if (actor.getPerfil() == Role.COORDENADOR) {
            professor = usuarios.findByEmail(request.professorEmail())
                    .filter(usuario -> usuario.getPerfil() == Role.PROFESSOR)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Professor inválido."));
        }

        Prova prova = new Prova();
        prova.setTitulo(request.title().trim());
        prova.setDisciplina(request.subject().trim());
        prova.setProfessorEmail(professor.getEmail());
        prova.setProfessorNome(professor.getNome());
        if (actor.getPerfil() == Role.COORDENADOR) {
            prova.setStatus(StatusProva.ABERTA);
        }
        List<Questao> questions = new ArrayList<>();
        for (int i = 0; i < request.questions().size(); i++) {
            QuestionRequest input = request.questions().get(i);
            TipoQuestao expected = i < OBJECTIVE_COUNT ? TipoQuestao.OBJETIVA : TipoQuestao.DISSERTATIVA;
            if (input.type() != expected || input.prompt() == null || input.prompt().isBlank()
                    || input.value() <= 0 || !Double.isFinite(input.value())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A prova exige seis questões objetivas seguidas por uma dissertativa, todas válidas.");
            }
            Questao question = new Questao();
            question.setId((long) i + 1);
            question.setEnunciado(input.prompt().trim());
            question.setTipo(input.type());
            question.setValor(input.value());
            question.setAlternativas(input.alternatives() == null ? List.of() : input.alternatives());
            question.setAlternativaCorreta(input.correctAlternative());
            if (expected == TipoQuestao.OBJETIVA
                    && (question.getAlternativas().size() < 2 || input.correctAlternative() == null
                    || input.correctAlternative() < 0 || input.correctAlternative() >= question.getAlternativas().size())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativas da questão objetiva inválidas.");
            }
            questions.add(question);
        }
        prova.setQuestoes(questions);
        return toResponse(provas.save(prova), true);
    }

    @PostMapping("/{id}/abrir")
    public ExamResponse open(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        Prova prova = findExam(id);
        ensureProfessorOwns(actor, prova);
        if (prova.getStatus() != StatusProva.RASCUNHO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente provas em rascunho podem ser abertas.");
        }
        prova.setStatus(StatusProva.ABERTA);
        return toResponse(provas.save(prova), true);
    }

    @PostMapping("/{id}/encerrar")
    public ExamResponse close(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        Prova prova = findExam(id);
        ensureProfessorOwns(actor, prova);
        if (prova.getStatus() != StatusProva.ABERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prova não está aberta.");
        }
        prova.setStatus(StatusProva.ENCERRADA);
        return toResponse(provas.save(prova), true);
    }

    @GetMapping("/{id}")
    public ExamResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        Prova prova = findExam(id);
        if (actor.getPerfil() == Role.PROFESSOR) {
            ensureProfessorOwns(actor, prova);
        } else if (actor.getPerfil() == Role.ALUNO && prova.getStatus() != StatusProva.ABERTA
                && prova.getStatus() != StatusProva.PUBLICADA) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } else if (actor.getPerfil() == Role.ATENDENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        boolean reveal = actor.getPerfil() != Role.ALUNO || prova.getStatus() == StatusProva.PUBLICADA;
        return toResponse(prova, reveal);
    }

    @GetMapping("/{id}/respostas")
    public List<RespostaProva> listAnswers(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        ensureProfessorOwns(actor, findExam(id));
        return respostas.findByProvaId(id);
    }

    @PostMapping("/{id}/respostas/{answerId}/correcao")
    public Map<String, Object> grade(
            @PathVariable Long id,
            @PathVariable Long answerId,
            @RequestBody GradeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        Prova prova = findExam(id);
        ensureProfessorOwns(actor, prova);
        if (prova.getStatus() != StatusProva.ENCERRADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Encerre a prova antes de corrigir.");
        }
        RespostaProva answer = respostas.findById(answerId)
                .filter(item -> item.getProvaId().equals(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        double objectiveScore = 0;
        double essayScore = 0;
        for (RespostaQuestao item : answer.getRespostas()) {
            Questao question = prova.getQuestoes().stream()
                    .filter(candidate -> candidate.getId().equals(item.getQuestaoId()))
                    .findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));
            if (question.getTipo() == TipoQuestao.OBJETIVA) {
                objectiveScore += item.getNota() == null ? 0 : item.getNota();
                continue;
            }
            GradeItem grade = request.grades() == null ? null : request.grades().stream()
                    .filter(candidate -> candidate.questionId().equals(item.getQuestaoId()))
                    .findFirst().orElse(null);
            if (grade == null || grade.score() < 0 || grade.score() > question.getValor()
                    || !Double.isFinite(grade.score())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma nota válida para a questão dissertativa.");
            }
            item.setNota(grade.score());
            item.setComentarioProfessor(grade.comment());
            essayScore += grade.score();
        }
        answer.setNotaFinal(objectiveScore + essayScore);
        answer.setCorrigida(true);
        answer.setDataCorrecao(LocalDateTime.now());
        respostas.save(answer);
        List<RespostaProva> allAnswers = respostas.findByProvaId(id);
        if (!allAnswers.isEmpty() && allAnswers.stream().allMatch(RespostaProva::isCorrigida)) {
            prova.setStatus(StatusProva.CORRIGIDA);
            provas.save(prova);
        }
        return Map.of("answerId", answer.getId(), "score", answer.getNotaFinal(), "graded", true);
    }

    @PostMapping("/{id}/publicar")
    public Map<String, Object> publish(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Usuario actor = currentUser(jwt);
        if (actor.getPerfil() != Role.COORDENADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Prova prova = findExam(id);
        if (prova.getStatus() != StatusProva.CORRIGIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prova precisa estar corrigida para ser publicada.");
        }
        prova.setStatus(StatusProva.PUBLICADA);
        prova.setDataPublicacao(LocalDateTime.now());
        provas.save(prova);
        respostas.findByProvaId(id).forEach(answer -> {
            answer.setPublicada(true);
            respostas.save(answer);
        });
        return Map.of("id", prova.getId(), "status", prova.getStatus().name());
    }

    @PostMapping("/{id}/responder")
    public Map<String, Object> answer(
            @PathVariable Long id,
            @RequestBody AnswerRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Usuario student = currentUser(jwt);
        if (student.getPerfil() != Role.ALUNO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Prova prova = findExam(id);
        if (prova.getStatus() != StatusProva.ABERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A prova não está aberta para respostas.");
        }
        if (respostas.findByProvaIdAndAlunoEmail(id, student.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta prova já foi respondida.");
        }
        if (request.answers() == null || request.answers().size() != prova.getQuestoes().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma resposta para cada questão.");
        }

        double score = 0;
        List<RespostaQuestao> submitted = new ArrayList<>();
        for (Questao question : prova.getQuestoes()) {
            SubmittedAnswer input = request.answers().stream()
                    .filter(answer -> question.getId().equals(answer.questionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resposta incompleta."));
            RespostaQuestao answer = new RespostaQuestao();
            answer.setQuestaoId(question.getId());
            if (question.getTipo() == TipoQuestao.OBJETIVA) {
                Integer selected = input.selectedAlternative();
                if (selected == null || selected < 0 || selected >= question.getAlternativas().size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternativa inválida.");
                }
                answer.setAlternativaEscolhida(selected);
                double earned = selected.equals(question.getAlternativaCorreta()) ? question.getValor() : 0;
                answer.setNota(earned);
                score += earned;
            } else {
                if (input.text() == null || input.text().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A resposta dissertativa é obrigatória.");
                }
                answer.setTextoResposta(input.text().trim());
                answer.setNota(0.0);
            }
            submitted.add(answer);
        }

        RespostaProva response = new RespostaProva();
        response.setProvaId(id);
        response.setAlunoEmail(student.getEmail());
        response.setAlunoNome(student.getNome());
        response.setRespostas(submitted);
        response.setNotaFinal(score);
        response.setCorrigida(false);
        response.setPublicada(false);
        RespostaProva saved = respostas.save(response);
        return Map.of("id", saved.getId(), "examId", id, "submittedAt", saved.getDataEnvio().toString());
    }

    @GetMapping("/resultados")
    public List<Map<String, Object>> results(@AuthenticationPrincipal Jwt jwt) {
        Usuario student = currentUser(jwt);
        if (student.getPerfil() != Role.ALUNO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return respostas.findByAlunoEmail(student.getEmail()).stream()
                .filter(RespostaProva::isPublicada)
                .map(answer -> {
                    Prova prova = findExam(answer.getProvaId());
                    return Map.<String, Object>of("examId", prova.getId(), "title", prova.getTitulo(),
                            "subject", prova.getDisciplina(), "score", answer.getNotaFinal() == null ? 0 : answer.getNotaFinal(),
                            "total", prova.getValorTotal(), "publishedAt",
                            prova.getDataPublicacao() == null ? "" : prova.getDataPublicacao().toString());
                }).toList();
    }

    private Usuario currentUser(Jwt jwt) {
        return usuarios.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private Prova findExam(Long id) {
        return provas.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void ensureProfessorOwns(Usuario actor, Prova prova) {
        if (actor.getPerfil() != Role.PROFESSOR || !actor.getEmail().equalsIgnoreCase(prova.getProfessorEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private ExamResponse toResponse(Prova prova, boolean revealCorrectAnswers) {
        List<QuestionResponse> questions = prova.getQuestoes().stream().map(question ->
                new QuestionResponse(question.getId(), question.getEnunciado(), question.getTipo(),
                        question.getAlternativas(), revealCorrectAnswers ? question.getAlternativaCorreta() : null,
                        question.getValor())).toList();
        return new ExamResponse(prova.getId(), prova.getTitulo(), prova.getDisciplina(),
                prova.getProfessorEmail(), prova.getProfessorNome(), prova.getStatus(),
                questions, prova.getDataCriacao(), prova.getDataPublicacao(), prova.getValorTotal());
    }

    public record CreateExamRequest(String title, String subject, String professorEmail, List<QuestionRequest> questions) {}
    public record QuestionRequest(String prompt, TipoQuestao type, List<String> alternatives,
            Integer correctAlternative, double value) {}
    public record AnswerRequest(List<SubmittedAnswer> answers) {}
    public record SubmittedAnswer(Long questionId, Integer selectedAlternative, String text) {}
    public record GradeRequest(List<GradeItem> grades) {}
    public record GradeItem(Long questionId, double score, String comment) {}
    public record QuestionResponse(Long id, String prompt, TipoQuestao type, List<String> alternatives,
            Integer correctAlternative, double value) {}
    public record ExamResponse(Long id, String title, String subject, String professorEmail,
            String professorName, StatusProva status, List<QuestionResponse> questions,
            LocalDateTime createdAt, LocalDateTime publishedAt, double totalValue) {}
}
