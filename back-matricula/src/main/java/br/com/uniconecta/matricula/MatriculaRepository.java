package br.com.uniconecta.matricula;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    List<Matricula> findByAlunoIdAndStatusNotOrderByCriadoEmDesc(Long alunoId, StatusMatricula status);

    List<Matricula> findByAlunoIdAndCursoIdAndStatus(Long alunoId, Long cursoId, StatusMatricula status);

    boolean existsByAlunoIdAndCursoIdAndStatus(Long alunoId, Long cursoId, StatusMatricula status);
}
