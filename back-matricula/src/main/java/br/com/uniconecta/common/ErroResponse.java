package br.com.uniconecta.common;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato padrão de erro devolvido pela API. */
public record ErroResponse(int status, String mensagem, Map<String, String> campos, LocalDateTime dataHora) {

    public static ErroResponse de(int status, String mensagem) {
        return new ErroResponse(status, mensagem, Map.of(), LocalDateTime.now());
    }
}
