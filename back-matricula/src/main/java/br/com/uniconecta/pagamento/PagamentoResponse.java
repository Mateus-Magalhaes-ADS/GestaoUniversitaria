package br.com.uniconecta.pagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.uniconecta.matricula.StatusMatricula;

public record PagamentoResponse(
        Long id,
        Long matriculaId,
        StatusMatricula matriculaStatus,
        FormaPagamento metodo,
        String metodoDescricao,
        StatusPagamento status,
        BigDecimal valor,
        Integer parcelas,
        BigDecimal valorParcela,
        String codigoTransacao,
        String codigoBarras,
        String linhaDigitavel,
        LocalDate vencimento,
        String pixCopiaCola,
        String cartaoFinal,
        String cartaoBandeira,
        LocalDateTime criadoEm,
        LocalDateTime pagoEm) {

    public static PagamentoResponse de(Pagamento p) {
        return new PagamentoResponse(p.getId(), p.getMatricula().getId(), p.getMatricula().getStatus(),
                p.getMetodo(), p.getMetodo().getDescricao(), p.getStatus(), p.getValor(), p.getParcelas(),
                p.getValorParcela(), p.getCodigoTransacao(), p.getCodigoBarras(), p.getLinhaDigitavel(),
                p.getVencimento(), p.getPixCopiaCola(), p.getCartaoFinal(), p.getCartaoBandeira(),
                p.getCriadoEm(), p.getPagoEm());
    }
}
