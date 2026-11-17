package br.com.uniconecta.matricula;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.uniconecta.aluno.Aluno;
import br.com.uniconecta.curso.Curso;
import br.com.uniconecta.curso.Turno;
import br.com.uniconecta.pagamento.FormaPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "matricula")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_id")
    private Curso curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Turno turno;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 20)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusMatricula status = StatusMatricula.AGUARDANDO_PAGAMENTO;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "confirmada_em")
    private LocalDateTime confirmadaEm;

    protected Matricula() {}

    public Matricula(Aluno aluno, Curso curso, Turno turno, FormaPagamento formaPagamento) {
        this.aluno = aluno;
        this.curso = curso;
        this.turno = turno;
        this.formaPagamento = formaPagamento;
        this.valorTotal = curso.getValorTotal();
    }

    public void confirmar() {
        this.status = StatusMatricula.CONFIRMADA;
        this.confirmadaEm = LocalDateTime.now();
    }

    public void cancelar() {
        this.status = StatusMatricula.CANCELADA;
    }

    public Long getId() { return id; }
    public Aluno getAluno() { return aluno; }
    public Curso getCurso() { return curso; }
    public Turno getTurno() { return turno; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public StatusMatricula getStatus() { return status; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getConfirmadaEm() { return confirmadaEm; }
}
