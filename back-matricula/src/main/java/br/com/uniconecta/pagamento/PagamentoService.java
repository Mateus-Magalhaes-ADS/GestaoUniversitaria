package br.com.uniconecta.pagamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.uniconecta.common.ApiException;
import br.com.uniconecta.common.ValidadorCpf;
import br.com.uniconecta.matricula.Matricula;
import br.com.uniconecta.pagamento.gateway.GeradorBoleto;
import br.com.uniconecta.pagamento.gateway.GeradorPix;
import br.com.uniconecta.pagamento.gateway.ValidadorCartao;

/**
 * Regras de pagamento. O "gateway" é SIMULADO (trabalho acadêmico):
 * nenhum valor é cobrado de verdade.
 */
@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final long pixConfirmacaoSegundos;
    private final String pixChave;
    private final int boletoDiasVencimento;

    public PagamentoService(PagamentoRepository pagamentoRepository,
                            @Value("${uniconecta.pagamento.pix-confirmacao-segundos}") long pixConfirmacaoSegundos,
                            @Value("${uniconecta.pagamento.pix-chave}") String pixChave,
                            @Value("${uniconecta.pagamento.boleto-dias-vencimento}") int boletoDiasVencimento) {
        this.pagamentoRepository = pagamentoRepository;
        this.pixConfirmacaoSegundos = pixConfirmacaoSegundos;
        this.pixChave = pixChave;
        this.boletoDiasVencimento = boletoDiasVencimento;
    }

    /** Cria o pagamento pendente da matrícula, já gerando boleto ou Pix quando for o caso. */
    @Transactional
    public Pagamento criarPara(Matricula matricula) {
        Pagamento pagamento = new Pagamento(matricula);
        switch (matricula.getFormaPagamento()) {
            case BOLETO -> {
                var boleto = GeradorBoleto.gerar(matricula.getValorTotal(), matricula.getId(),
                        LocalDate.now().plusDays(boletoDiasVencimento));
                pagamento.registrarBoleto(boleto.codigoBarras(), boleto.linhaDigitavel(), boleto.vencimento());
            }
            case PIX -> pagamento.registrarPix(GeradorPix.gerar(pixChave, "UniConecta Educacional",
                    "Sao Paulo", matricula.getValorTotal(), "MAT" + matricula.getId() + "T" + System.currentTimeMillis()));
            case CARTAO -> { /* dados do cartão chegam no passo 2 */ }
        }
        return pagamentoRepository.save(pagamento);
    }

    @Transactional
    public PagamentoResponse buscar(Long alunoId, Long pagamentoId) {
        Pagamento pagamento = carregarDoAluno(alunoId, pagamentoId);
        confirmarPixSeNecessario(pagamento);
        return PagamentoResponse.de(pagamento);
    }

    @Transactional
    public PagamentoResponse pagarComCartao(Long alunoId, Long pagamentoId, CartaoRequest req) {
        Pagamento pagamento = carregarDoAluno(alunoId, pagamentoId);
        exigirPendente(pagamento, FormaPagamento.CARTAO);

        int maxParcelas = pagamento.getMatricula().getCurso().getMaxParcelas();
        if (req.parcelas() > maxParcelas) {
            throw ApiException.requisicaoInvalida("Este curso pode ser parcelado em no máximo " + maxParcelas + "x.");
        }
        if (!ValidadorCpf.valido(req.cpfTitular())) {
            throw ApiException.requisicaoInvalida("CPF do titular inválido.");
        }
        String numero = ValidadorCartao.somenteDigitos(req.numeroCartao());
        if (!ValidadorCartao.numeroValido(numero)) {
            throw ApiException.requisicaoInvalida("Número do cartão inválido.");
        }
        if (!ValidadorCartao.validadeValida(req.validade(), YearMonth.now())) {
            throw ApiException.requisicaoInvalida("Cartão vencido ou data de vencimento inválida.");
        }
        String bandeira = ValidadorCartao.bandeira(numero);
        if (!ValidadorCartao.cvcValido(req.cvc(), bandeira)) {
            throw ApiException.requisicaoInvalida("Código CVC inválido.");
        }

        // Simulação da operadora: um cartão de teste específico é sempre recusado.
        if (ValidadorCartao.CARTAO_RECUSADO.equals(numero)) {
            throw ApiException.naoProcessavel(
                    "Pagamento recusado pela operadora do cartão. Verifique os dados ou use outro cartão.");
        }

        pagamento.definirParcelas(req.parcelas());
        pagamento.registrarCartao(numero.substring(numero.length() - 4), bandeira,
                req.nomeTitular().trim().toUpperCase());
        pagamento.aprovar(novoCodigoTransacao());
        return PagamentoResponse.de(pagamento);
    }

    /** Usado na apresentação para simular o banco avisando que o boleto/Pix foi pago. */
    @Transactional
    public PagamentoResponse simularConfirmacao(Long alunoId, Long pagamentoId) {
        Pagamento pagamento = carregarDoAluno(alunoId, pagamentoId);
        if (pagamento.getMetodo() == FormaPagamento.CARTAO) {
            throw ApiException.requisicaoInvalida("Pagamentos com cartão são confirmados no envio dos dados do cartão.");
        }
        exigirPendente(pagamento, pagamento.getMetodo());
        pagamento.aprovar(novoCodigoTransacao());
        return PagamentoResponse.de(pagamento);
    }

    /** Simulação: o Pix é "pago" automaticamente alguns segundos depois de gerado. */
    public void confirmarPixSeNecessario(Pagamento pagamento) {
        if (pagamento.getMetodo() == FormaPagamento.PIX && pagamento.isPendente()
                && pagamento.getCriadoEm().plusSeconds(pixConfirmacaoSegundos).isBefore(LocalDateTime.now())) {
            pagamento.aprovar(novoCodigoTransacao());
        }
    }

    private Pagamento carregarDoAluno(Long alunoId, Long pagamentoId) {
        return pagamentoRepository.findById(pagamentoId)
                .filter(p -> p.getMatricula().getAluno().getId().equals(alunoId))
                .orElseThrow(() -> ApiException.naoEncontrado("Pagamento não encontrado."));
    }

    private void exigirPendente(Pagamento pagamento, FormaPagamento esperado) {
        if (pagamento.getMetodo() != esperado) {
            throw ApiException.requisicaoInvalida("Forma de pagamento incorreta para esta matrícula.");
        }
        switch (pagamento.getStatus()) {
            case APROVADO -> throw ApiException.conflito("Este pagamento já foi aprovado.");
            case CANCELADO -> throw ApiException.conflito(
                    "Este pagamento foi cancelado. Inicie a matrícula novamente.");
            default -> { }
        }
    }

    private static String novoCodigoTransacao() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(100_000_000_000L, 999_999_999_999L));
    }
}
