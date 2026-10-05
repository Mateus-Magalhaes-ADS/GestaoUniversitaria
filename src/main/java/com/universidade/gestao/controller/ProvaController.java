package com.universidade.gestao.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.universidade.gestao.model.Prova;
import com.universidade.gestao.model.Questao;
import com.universidade.gestao.model.RespostaProva;
import com.universidade.gestao.model.RespostaQuestao;
import com.universidade.gestao.model.StatusProva;
import com.universidade.gestao.model.TipoQuestao;
import com.universidade.gestao.model.Usuario;
import com.universidade.gestao.repository.ProvaRepository;
import com.universidade.gestao.repository.RespostaProvaRepository;
import com.universidade.gestao.repository.UsuarioRepository;

@Controller
@RequestMapping("/prova")
public class ProvaController {

    private static final int QUESTOES_OBJETIVAS = 6;
    private static final int TOTAL_QUESTOES = QUESTOES_OBJETIVAS + 1;

    private final ProvaRepository provaRepository;
    private final RespostaProvaRepository respostaProvaRepository;
    private final UsuarioRepository usuarioRepository;

    public ProvaController(ProvaRepository provaRepository,
                            RespostaProvaRepository respostaProvaRepository,
                            UsuarioRepository usuarioRepository) {
        this.provaRepository = provaRepository;
        this.respostaProvaRepository = respostaProvaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ---------- PROFESSOR: criação e gestão do ciclo de vida da prova ----------

    @GetMapping("/nova")
    public String formularioNovaProva(Model model) {
        model.addAttribute("tituloPage", "Nova Prova");
        return "prova/nova";
    }

    @PostMapping("/nova")
    public String criarProva(
            @RequestParam String titulo,
            @RequestParam String disciplina,
            @RequestParam List<String> enunciado,
            @RequestParam List<String> tipo,
            @RequestParam List<String> valor,
            @RequestParam(required = false) List<String> alternativas,
            @RequestParam(required = false) List<String> alternativaCorreta,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes redirectAttributes) {

        Usuario professor = usuarioRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (titulo.isBlank() || disciplina.isBlank()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Informe o título e a disciplina da prova.");
            return "redirect:/prova/nova";
        }

        if (enunciado.size() != TOTAL_QUESTOES || tipo.size() != TOTAL_QUESTOES
                || valor.size() != TOTAL_QUESTOES) {
            redirectAttributes.addFlashAttribute("mensagemErro", "A prova deve ter exatamente seis questões objetivas e uma dissertativa.");
            return "redirect:/prova/nova";
        }

        for (int i = 0; i < TOTAL_QUESTOES; i++) {
            String tipoEsperado = i < QUESTOES_OBJETIVAS ? TipoQuestao.OBJETIVA.name() : TipoQuestao.DISSERTATIVA.name();
            if (!tipoEsperado.equals(tipo.get(i)) || enunciado.get(i).isBlank()) {
                redirectAttributes.addFlashAttribute("mensagemErro", "Preencha seis questões objetivas e uma dissertativa, nessa ordem.");
                return "redirect:/prova/nova";
            }
        }

        Prova prova = new Prova();
        prova.setTitulo(titulo.trim());
        prova.setDisciplina(disciplina.trim());
        prova.setProfessorEmail(professor.getEmail());
        prova.setProfessorNome(professor.getNome());
        prova.setStatus(StatusProva.RASCUNHO);

        try {
            for (int i = 0; i < enunciado.size(); i++) {
                Questao questao = new Questao();
                questao.setEnunciado(enunciado.get(i).trim());
                TipoQuestao tipoQuestao = TipoQuestao.valueOf(tipo.get(i));
                questao.setTipo(tipoQuestao);
                double valorQuestao = Double.parseDouble(valor.get(i).replace(",", "."));
                if (!Double.isFinite(valorQuestao) || valorQuestao <= 0) {
                    redirectAttributes.addFlashAttribute("mensagemErro", "O valor de cada questão deve ser maior que zero.");
                    return "redirect:/prova/nova";
                }
                questao.setValor(valorQuestao);

                if (tipoQuestao == TipoQuestao.OBJETIVA) {
                    String alternativasBrutas = alternativas != null && alternativas.size() > i ? alternativas.get(i) : "";
                    List<String> listaAlternativas = alternativasBrutas.lines()
                            .map(String::trim)
                            .filter(linha -> !linha.isEmpty())
                            .toList();
                    if (listaAlternativas.size() < 2) {
                        redirectAttributes.addFlashAttribute("mensagemErro", "Questões objetivas precisam de ao menos duas alternativas.");
                        return "redirect:/prova/nova";
                    }

                    String correntaTexto = alternativaCorreta != null && alternativaCorreta.size() > i ? alternativaCorreta.get(i) : "";
                    if (correntaTexto.isBlank()) {
                        redirectAttributes.addFlashAttribute("mensagemErro", "Selecione a alternativa correta de cada questão objetiva.");
                        return "redirect:/prova/nova";
                    }

                    int correta = Integer.parseInt(correntaTexto);
                    if (correta < 0 || correta >= listaAlternativas.size()) {
                        redirectAttributes.addFlashAttribute("mensagemErro", "A alternativa correta informada é inválida.");
                        return "redirect:/prova/nova";
                    }

                    questao.setAlternativas(listaAlternativas);
                    questao.setAlternativaCorreta(correta);
                }

                prova.getQuestoes().add(questao);
            }
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Verifique os valores preenchidos nas questões.");
            return "redirect:/prova/nova";
        }

        if (prova.getQuestoes().isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Adicione ao menos uma questão válida.");
            return "redirect:/prova/nova";
        }

        provaRepository.save(prova);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Prova criada como rascunho. Abra-a quando estiver pronta para os alunos.");
        return "redirect:/prova/professor";
    }

    @GetMapping("/professor")
    public String listarProvasDoProfessor(@AuthenticationPrincipal UserDetails user, Model model) {
        model.addAttribute("provas", provaRepository.findByProfessorEmail(user.getUsername()));
        return "prova/professor-lista";
    }

    @PostMapping("/{id}/abrir")
    public String abrirProva(@PathVariable Long id, @AuthenticationPrincipal UserDetails user, RedirectAttributes redirectAttributes) {
        Prova prova = buscarProvaDoProfessor(id, user);
        if (prova.getStatus() != StatusProva.RASCUNHO) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Somente provas em rascunho podem ser abertas.");
            return "redirect:/prova/professor";
        }
        prova.setStatus(StatusProva.ABERTA);
        provaRepository.save(prova);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Prova aberta. Os alunos já podem respondê-la.");
        return "redirect:/prova/professor";
    }

    @PostMapping("/{id}/encerrar")
    public String encerrarProva(@PathVariable Long id, @AuthenticationPrincipal UserDetails user, RedirectAttributes redirectAttributes) {
        Prova prova = buscarProvaDoProfessor(id, user);
        if (prova.getStatus() != StatusProva.ABERTA) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Somente provas abertas podem ser encerradas.");
            return "redirect:/prova/professor";
        }
        prova.setStatus(StatusProva.ENCERRADA);
        provaRepository.save(prova);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Prova encerrada. Agora você pode corrigir as respostas.");
        return "redirect:/prova/professor";
    }

    @GetMapping("/{id}/corrigir")
    public String listarRespostasParaCorrecao(@PathVariable Long id, @AuthenticationPrincipal UserDetails user, Model model) {
        Prova prova = buscarProvaDoProfessor(id, user);
        model.addAttribute("prova", prova);
        model.addAttribute("respostas", respostaProvaRepository.findByProvaId(id));
        return "prova/corrigir-lista";
    }

    @GetMapping("/{id}/corrigir/{respostaId}")
    public String formularioCorrecao(@PathVariable Long id, @PathVariable Long respostaId,
                                      @AuthenticationPrincipal UserDetails user, Model model) {
        Prova prova = buscarProvaDoProfessor(id, user);
        RespostaProva resposta = buscarRespostaDaProva(prova, respostaId);

        Map<Long, RespostaQuestao> respostasPorQuestao = new HashMap<>();
        for (RespostaQuestao respostaQuestao : resposta.getRespostas()) {
            respostasPorQuestao.put(respostaQuestao.getQuestaoId(), respostaQuestao);
        }

        model.addAttribute("prova", prova);
        model.addAttribute("resposta", resposta);
        model.addAttribute("respostasPorQuestao", respostasPorQuestao);
        return "prova/corrigir";
    }

    @PostMapping("/{id}/corrigir/{respostaId}")
    public String salvarCorrecao(@PathVariable Long id, @PathVariable Long respostaId,
                                  @RequestParam Map<String, String> parametros,
                                  @AuthenticationPrincipal UserDetails user,
                                  RedirectAttributes redirectAttributes) {
        Prova prova = buscarProvaDoProfessor(id, user);
        RespostaProva resposta = buscarRespostaDaProva(prova, respostaId);

        double notaFinal = 0;
        for (Questao questao : prova.getQuestoes()) {
            String notaTexto = parametros.get("nota_" + questao.getId());
            double notaBruta = notaTexto == null || notaTexto.isBlank() ? 0 : Double.parseDouble(notaTexto.replace(",", "."));
            double nota = Math.max(0, Math.min(notaBruta, questao.getValor()));
            String comentario = parametros.get("comentario_" + questao.getId());

            for (RespostaQuestao respostaQuestao : resposta.getRespostas()) {
                if (respostaQuestao.getQuestaoId().equals(questao.getId())) {
                    respostaQuestao.setNota(nota);
                    respostaQuestao.setComentarioProfessor(comentario);
                    break;
                }
            }

            notaFinal += nota;
        }

        resposta.setNotaFinal(notaFinal);
        resposta.setCorrigida(true);
        resposta.setDataCorrecao(LocalDateTime.now());
        respostaProvaRepository.save(resposta);

        // Quando todas as respostas da prova estiverem corrigidas, libera a validação da coordenação
        boolean todasCorrigidas = respostaProvaRepository.findByProvaId(id).stream().allMatch(RespostaProva::isCorrigida);
        if (todasCorrigidas && prova.getStatus() == StatusProva.ENCERRADA) {
            prova.setStatus(StatusProva.CORRIGIDA);
            provaRepository.save(prova);
        }

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Correção salva com sucesso.");
        return "redirect:/prova/" + id + "/corrigir";
    }

    // ---------- COORDENADOR: validação e publicação ----------

    @GetMapping("/coordenacao")
    public String listarProvasParaValidacao(Model model) {
        List<Prova> provas = provaRepository.findAll().stream()
                .filter(prova -> prova.getStatus() == StatusProva.CORRIGIDA || prova.getStatus() == StatusProva.PUBLICADA)
                .toList();
        model.addAttribute("provas", provas);
        return "prova/coordenacao-lista";
    }

    @GetMapping("/coordenacao/{id}")
    public String detalharProvaParaValidacao(@PathVariable Long id, Model model) {
        Prova prova = provaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("prova", prova);
        model.addAttribute("respostas", respostaProvaRepository.findByProvaId(id));
        return "prova/coordenacao-detalhe";
    }

    @PostMapping("/{id}/publicar")
    public String publicarProva(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Prova prova = provaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (prova.getStatus() != StatusProva.CORRIGIDA) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Somente provas totalmente corrigidas podem ser publicadas.");
            return "redirect:/prova/coordenacao";
        }

        for (RespostaProva resposta : respostaProvaRepository.findByProvaId(id)) {
            resposta.setPublicada(true);
            respostaProvaRepository.save(resposta);
        }

        prova.setStatus(StatusProva.PUBLICADA);
        prova.setDataPublicacao(LocalDateTime.now());
        provaRepository.save(prova);

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Prova publicada. Os alunos já podem ver as notas.");
        return "redirect:/prova/coordenacao";
    }

    // ---------- ALUNO: resposta e resultados ----------

    @GetMapping("/disponiveis")
    public String listarProvasDisponiveis(@AuthenticationPrincipal UserDetails user, Model model) {
        List<Long> jaRespondidas = respostaProvaRepository.findByAlunoEmail(user.getUsername()).stream()
                .map(RespostaProva::getProvaId)
                .toList();
        List<Prova> disponiveis = provaRepository.findByStatus(StatusProva.ABERTA).stream()
                .filter(prova -> !jaRespondidas.contains(prova.getId()))
                .toList();
        model.addAttribute("provas", disponiveis);
        return "prova/aluno-lista";
    }

    @GetMapping("/{id}/responder")
    public String formularioResposta(@PathVariable Long id, @AuthenticationPrincipal UserDetails user, Model model) {
        Prova prova = buscarProvaAbertaNaoRespondida(id, user);
        model.addAttribute("prova", prova);
        return "prova/responder";
    }

    @PostMapping("/{id}/responder")
    public String enviarResposta(@PathVariable Long id,
                                  @RequestParam Map<String, String> parametros,
                                  @AuthenticationPrincipal UserDetails user,
                                  RedirectAttributes redirectAttributes) {
        Prova prova = buscarProvaAbertaNaoRespondida(id, user);
        Usuario aluno = usuarioRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        RespostaProva respostaProva = new RespostaProva();
        respostaProva.setProvaId(id);
        respostaProva.setAlunoEmail(aluno.getEmail());
        respostaProva.setAlunoNome(aluno.getNome());

        for (Questao questao : prova.getQuestoes()) {
            RespostaQuestao respostaQuestao = new RespostaQuestao();
            respostaQuestao.setQuestaoId(questao.getId());
            if (questao.getTipo() == TipoQuestao.OBJETIVA) {
                String escolhida = parametros.get("alternativa_" + questao.getId());
                if (escolhida == null || escolhida.isBlank()) {
                    redirectAttributes.addFlashAttribute("mensagemErro", "Responda todas as questões antes de enviar.");
                    return "redirect:/prova/" + id + "/responder";
                }
                try {
                    int indiceEscolhido = Integer.parseInt(escolhida);
                    if (indiceEscolhido < 0 || indiceEscolhido >= questao.getAlternativas().size()) {
                        redirectAttributes.addFlashAttribute("mensagemErro", "Uma das alternativas selecionadas é inválida.");
                        return "redirect:/prova/" + id + "/responder";
                    }
                    respostaQuestao.setAlternativaEscolhida(indiceEscolhido);
                } catch (NumberFormatException exception) {
                    redirectAttributes.addFlashAttribute("mensagemErro", "Uma das alternativas selecionadas é inválida.");
                    return "redirect:/prova/" + id + "/responder";
                }
            } else {
                String textoResposta = parametros.get("texto_" + questao.getId());
                if (textoResposta == null || textoResposta.isBlank()) {
                    redirectAttributes.addFlashAttribute("mensagemErro", "Preencha a questão dissertativa antes de enviar.");
                    return "redirect:/prova/" + id + "/responder";
                }
                respostaQuestao.setTextoResposta(textoResposta.trim());
            }
            respostaProva.getRespostas().add(respostaQuestao);
        }

        respostaProvaRepository.save(respostaProva);
        redirectAttributes.addFlashAttribute("mensagemSucesso", "Respostas enviadas com sucesso. Aguarde a correção do professor.");
        return "redirect:/prova/disponiveis";
    }

    @GetMapping("/resultados")
    public String listarResultados(@AuthenticationPrincipal UserDetails user, Model model) {
        List<RespostaProva> respostas = respostaProvaRepository.findByAlunoEmail(user.getUsername()).stream()
                .filter(RespostaProva::isPublicada)
                .toList();

        Map<Long, Prova> provasPorId = new HashMap<>();
        for (RespostaProva resposta : respostas) {
            provaRepository.findById(resposta.getProvaId()).ifPresent(prova -> provasPorId.put(prova.getId(), prova));
        }

        model.addAttribute("respostas", respostas);
        model.addAttribute("provas", provasPorId);
        return "prova/resultados";
    }

    // ---------- Helpers ----------

    private Prova buscarProvaDoProfessor(Long id, UserDetails user) {
        Prova prova = provaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!normalizeEmail(prova.getProfessorEmail()).equals(normalizeEmail(user.getUsername()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return prova;
    }

    private RespostaProva buscarRespostaDaProva(Prova prova, Long respostaId) {
        RespostaProva resposta = respostaProvaRepository.findById(respostaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!resposta.getProvaId().equals(prova.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return resposta;
    }

    private Prova buscarProvaAbertaNaoRespondida(Long id, UserDetails user) {
        Prova prova = provaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (prova.getStatus() != StatusProva.ABERTA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta prova não está disponível para respostas.");
        }
        if (respostaProvaRepository.findByProvaIdAndAlunoEmail(id, user.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você já respondeu esta prova.");
        }
        return prova;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
