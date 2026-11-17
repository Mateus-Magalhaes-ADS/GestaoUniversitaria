package br.com.uniconecta.curso;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "curso")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150, unique = true)
    private String nome;

    @Column(nullable = false, length = 30)
    private String grau;

    @Column(length = 500)
    private String descricao;

    @Column(name = "duracao_meses", nullable = false)
    private Integer duracaoMeses;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "max_parcelas", nullable = false)
    private Integer maxParcelas;

    @Column(nullable = false)
    private boolean ativo = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "curso_turno", joinColumns = @JoinColumn(name = "curso_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "turno", length = 20)
    private Set<Turno> turnos = EnumSet.noneOf(Turno.class);

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getGrau() { return grau; }
    public String getDescricao() { return descricao; }
    public Integer getDuracaoMeses() { return duracaoMeses; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public Integer getMaxParcelas() { return maxParcelas; }
    public boolean isAtivo() { return ativo; }
    public Set<Turno> getTurnos() { return turnos; }
}
