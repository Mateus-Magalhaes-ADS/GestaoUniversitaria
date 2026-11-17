package br.com.uniconecta.curso;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record CursoResponse(
        Long id,
        String nome,
        String grau,
        String descricao,
        Integer duracaoMeses,
        BigDecimal valorTotal,
        Integer maxParcelas,
        List<TurnoResponse> turnos) {

    public record TurnoResponse(Turno valor, String descricao) {}

    public static CursoResponse de(Curso curso) {
        List<TurnoResponse> turnos = curso.getTurnos().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .map(t -> new TurnoResponse(t, t.getDescricao()))
                .toList();
        return new CursoResponse(curso.getId(), curso.getNome(), curso.getGrau(), curso.getDescricao(),
                curso.getDuracaoMeses(), curso.getValorTotal(), curso.getMaxParcelas(), turnos);
    }
}
