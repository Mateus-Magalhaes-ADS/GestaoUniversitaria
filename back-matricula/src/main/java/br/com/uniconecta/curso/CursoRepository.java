package br.com.uniconecta.curso;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByAtivoTrueOrderByNomeAsc();

    Optional<Curso> findByIdAndAtivoTrue(Long id);
}
