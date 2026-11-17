package br.com.uniconecta.matricula;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matriculas")
public class MatriculaController {

    private final MatriculaService matriculaService;

    public MatriculaController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaResponse criar(@AuthenticationPrincipal Long alunoId,
                                   @Valid @RequestBody NovaMatriculaRequest request) {
        return matriculaService.criar(alunoId, request);
    }

    @GetMapping("/minhas")
    public List<MatriculaResponse> minhas(@AuthenticationPrincipal Long alunoId) {
        return matriculaService.listarDoAluno(alunoId);
    }

    @GetMapping("/{id}")
    public MatriculaResponse buscar(@AuthenticationPrincipal Long alunoId, @PathVariable Long id) {
        return matriculaService.buscar(alunoId, id);
    }
}
