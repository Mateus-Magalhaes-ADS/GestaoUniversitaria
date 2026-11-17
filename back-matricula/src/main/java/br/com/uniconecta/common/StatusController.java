package br.com.uniconecta.common;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota pública simples para verificar se a API está no ar. */
@RestController
public class StatusController {

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of("aplicacao", "UniConecta API", "status", "online", "dataHora", LocalDateTime.now());
    }
}
