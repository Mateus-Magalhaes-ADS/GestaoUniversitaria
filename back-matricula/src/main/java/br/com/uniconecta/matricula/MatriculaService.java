package br.com.uniconecta.matricula;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.uniconecta.aluno.Aluno;
import br.com.uniconecta.aluno.AlunoRepository;
import br.com.uniconecta.common.ApiException;
import br.com.uniconecta.curso.Curso;
import br.com.uniconecta.curso.CursoRepository;
import br.com.uniconecta.pagamento.Pagamento;
import br.com.uniconecta.pagamento.PagamentoRepository;
import br.com.uniconecta.pagamento.PagamentoService;

@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final CursoRepository cursoRepository;
    private final AlunoRepository alunoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoService pagamentoService;

    public MatriculaService(MatriculaRepository matriculaRepository, CursoRepository cursoRepository,
                            AlunoRepository alunoRepository, PagamentoRepository pagamentoRepository,
                            PagamentoService pagamentoService) {
        this.matriculaRepository = matriculaRepository;
        this.cursoRepository = cursoRepository;
        this.alunoRepository = alunoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.pagamentoService = pagamentoService;
    }

    /**
     * Passo 1 da matrícula. Regras:
     *  - o curso precisa estar ativo e oferecer o turno escolhido;
     *  - o aluno não pode ter matrícula CONFIRMADA no mesmo curso;
     *  - se houver uma matrícula AGUARDANDO_PAGAMENTO no mesmo curso (ex.: o aluno voltou
     *    e trocou a forma de pagamento), ela é cancelada e substituída pela nova.
     */
    @Transactional
    public MatriculaResponse criar(Long alunoId, NovaMatriculaRequest req) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> ApiException.naoEncontrado("Aluno não encontrado."));
        Curso curso = cursoRepository.findByIdAndAtivoTrue(req.cursoId())
                .orElseThrow(() -> ApiException.naoEncontrado("Curso não encontrado."));

        if (!curso.getTurnos().contains(req.turno())) {
            throw ApiException.requisicaoInvalida("O turno " + req.turno().getDescricao().toLowerCase()
                    + " não está disponível para este curso.");
        }
        if (matriculaRepository.existsByAlunoIdAndCursoIdAndStatus(alunoId, curso.getId(), StatusMatricula.CONFIRMADA)) {
            throw ApiException.conflito("Você já possui matrícula confirmada neste curso.");
        }

        for (Matricula pendente : matriculaRepository.findByAlunoIdAndCursoIdAndStatus(
                alunoId, curso.getId(), StatusMatricula.AGUARDANDO_PAGAMENTO)) {
            pendente.cancelar();
            pagamentoRepository.findByMatriculaId(pendente.getId()).ifPresent(Pagamento::cancelar);
        }

        Matricula matricula = matriculaRepository.save(new Matricula(aluno, curso, req.turno(), req.formaPagamento()));
        Pagamento pagamento = pagamentoService.criarPara(matricula);
        return MatriculaResponse.de(matricula, pagamento);
    }

    @Transactional
    public List<MatriculaResponse> listarDoAluno(Long alunoId) {
        return matriculaRepository.findByAlunoIdAndStatusNotOrderByCriadoEmDesc(alunoId, StatusMatricula.CANCELADA)
                .stream()
                .map(this::montarResposta)
                .toList();
    }

    @Transactional
    public MatriculaResponse buscar(Long alunoId, Long matriculaId) {
        return matriculaRepository.findById(matriculaId)
                .filter(m -> m.getAluno().getId().equals(alunoId))
                .map(this::montarResposta)
                .orElseThrow(() -> ApiException.naoEncontrado("Matrícula não encontrada."));
    }

    private MatriculaResponse montarResposta(Matricula matricula) {
        Pagamento pagamento = pagamentoRepository.findByMatriculaId(matricula.getId()).orElse(null);
        if (pagamento != null) {
            pagamentoService.confirmarPixSeNecessario(pagamento);
        }
        return MatriculaResponse.de(matricula, pagamento);
    }
}
