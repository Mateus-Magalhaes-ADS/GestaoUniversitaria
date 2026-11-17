package br.com.uniconecta.curso;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.uniconecta.common.ApiException;

@RestController
@RequestMapping("/api/cursos")
public class CursoController {

    private final CursoRepository cursoRepository;

    public CursoController(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CursoResponse> listar() {
        return cursoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(CursoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CursoResponse buscar(@PathVariable Long id) {
        return cursoRepository.findByIdAndAtivoTrue(id)
                .map(CursoResponse::de)
                .orElseThrow(() -> ApiException.naoEncontrado("Curso não encontrado."));
    }
}
