package br.com.uniconecta.matricula;

import br.com.uniconecta.curso.Turno;
import br.com.uniconecta.pagamento.FormaPagamento;
import jakarta.validation.constraints.NotNull;

/** Dados do Passo 1: curso, turno e forma de pagamento. */
public record NovaMatriculaRequest(
        @NotNull(message = "Escolha o curso.") Long cursoId,
        @NotNull(message = "Escolha o período.") Turno turno,
        @NotNull(message = "Escolha a forma de pagamento.") FormaPagamento formaPagamento) {
}
