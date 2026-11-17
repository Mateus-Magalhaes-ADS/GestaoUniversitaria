package br.com.uniconecta.pagamento;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    /** Consulta o status (a tela do Pix chama esta rota a cada poucos segundos). */
    @GetMapping("/{id}")
    public PagamentoResponse buscar(@AuthenticationPrincipal Long alunoId, @PathVariable Long id) {
        return pagamentoService.buscar(alunoId, id);
    }

    @PostMapping("/{id}/cartao")
    public PagamentoResponse pagarComCartao(@AuthenticationPrincipal Long alunoId, @PathVariable Long id,
                                            @Valid @RequestBody CartaoRequest request) {
        return pagamentoService.pagarComCartao(alunoId, id, request);
    }

    /** Apenas para demonstração: simula o banco confirmando o pagamento de boleto ou Pix. */
    @PostMapping("/{id}/simular-confirmacao")
    public PagamentoResponse simularConfirmacao(@AuthenticationPrincipal Long alunoId, @PathVariable Long id) {
        return pagamentoService.simularConfirmacao(alunoId, id);
    }
}
