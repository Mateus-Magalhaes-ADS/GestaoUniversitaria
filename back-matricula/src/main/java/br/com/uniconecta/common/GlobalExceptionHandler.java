package br.com.uniconecta.common;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte exceções em respostas JSON amigáveis (em português). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroResponse> tratarApi(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErroResponse.de(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        String mensagem = campos.isEmpty() ? "Dados inválidos." : campos.values().iterator().next();
        return ResponseEntity.badRequest()
                .body(new ErroResponse(400, mensagem, campos, LocalDateTime.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErroResponse.de(400, "Requisição inválida. Verifique os dados enviados."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(404, "Recurso não encontrado."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarGenerico(Exception ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErroResponse.de(500, "Ocorreu um erro inesperado. Tente novamente."));
    }
}
