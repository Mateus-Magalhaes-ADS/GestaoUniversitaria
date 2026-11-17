package br.com.uniconecta.pagamento;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados do cartão enviados pelo app. Usados apenas em memória: nunca são gravados por completo. */
public record CartaoRequest(
        @NotNull(message = "Escolha a quantidade de parcelas.")
        @Min(value = 1, message = "Quantidade de parcelas inválida.")
        @Max(value = 24, message = "Quantidade de parcelas inválida.")
        Integer parcelas,

        @NotBlank(message = "Informe o CPF do titular.")
        String cpfTitular,

        @NotBlank(message = "Informe o nome impresso no cartão.")
        @Size(max = 120, message = "Nome muito longo.")
        String nomeTitular,

        @NotBlank(message = "Informe o número do cartão.")
        String numeroCartao,

        @NotBlank(message = "Informe o vencimento do cartão.")
        @Pattern(regexp = "\\d{2}/\\d{2}", message = "Vencimento deve estar no formato MM/AA.")
        String validade,

        @NotBlank(message = "Informe o código CVC.")
        String cvc) {

    @Override
    public String toString() {
        // Evita que dados do cartão apareçam em logs.
        return "CartaoRequest[parcelas=" + parcelas + ", cartao=****]";
    }
}
