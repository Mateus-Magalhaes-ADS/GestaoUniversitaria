package com.universidade.gestao.model;

import java.util.ArrayList;
import java.util.List;

public class Questao {

    private Long id;
    private String enunciado;
    private TipoQuestao tipo;
    private List<String> alternativas = new ArrayList<>();
    private Integer alternativaCorreta;
    private double valor;

    public Questao() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }

    public TipoQuestao getTipo() { return tipo; }
    public void setTipo(TipoQuestao tipo) { this.tipo = tipo; }

    public List<String> getAlternativas() { return alternativas; }
    public void setAlternativas(List<String> alternativas) { this.alternativas = alternativas; }

    // Índice (0-based) da alternativa correta; usado apenas pelo professor/coordenador, nunca exibido ao aluno antes da publicação
    public Integer getAlternativaCorreta() { return alternativaCorreta; }
    public void setAlternativaCorreta(Integer alternativaCorreta) { this.alternativaCorreta = alternativaCorreta; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}
