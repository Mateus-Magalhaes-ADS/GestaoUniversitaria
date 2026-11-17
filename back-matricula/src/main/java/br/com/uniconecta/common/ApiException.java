package br.com.uniconecta.common;

import org.springframework.http.HttpStatus;

/**
 * Exceção de regra de negócio. A mensagem é exibida diretamente para o aluno no app.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException naoEncontrado(String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ApiException requisicaoInvalida(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, mensagem);
    }

    public static ApiException naoProcessavel(String mensagem) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, mensagem);
    }
}
