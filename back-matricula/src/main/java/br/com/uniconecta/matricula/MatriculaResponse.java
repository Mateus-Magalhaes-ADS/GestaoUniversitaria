package br.com.uniconecta.matricula;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.uniconecta.curso.Turno;
import br.com.uniconecta.pagamento.FormaPagamento;
import br.com.uniconecta.pagamento.Pagamento;
import br.com.uniconecta.pagamento.PagamentoResponse;

public record MatriculaResponse(
        Long id,
        StatusMatricula status,
        CursoResumo curso,
        Turno turno,
        String turnoDescricao,
        BigDecimal valorTotal,
        FormaPagamento formaPagamento,
        LocalDateTime criadoEm,
        LocalDateTime confirmadaEm,
        PagamentoResponse pagamento) {

    public record CursoResumo(Long id, String nome, Integer duracaoMeses, Integer maxParcelas) {}

    public static MatriculaResponse de(Matricula m, Pagamento pagamento) {
        var curso = m.getCurso();
        return new MatriculaResponse(m.getId(), m.getStatus(),
                new CursoResumo(curso.getId(), curso.getNome(), curso.getDuracaoMeses(), curso.getMaxParcelas()),
                m.getTurno(), m.getTurno().getDescricao(), m.getValorTotal(), m.getFormaPagamento(),
                m.getCriadoEm(), m.getConfirmadaEm(),
                pagamento == null ? null : PagamentoResponse.de(pagamento));
    }
}
