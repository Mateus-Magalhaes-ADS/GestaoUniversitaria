package com.universidade.gestao.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Prova {

    private Long id;
    private String titulo;
    private String disciplina;
    private String professorEmail;
    private String professorNome;
    private StatusProva status = StatusProva.RASCUNHO;
    private List<Questao> questoes = new ArrayList<>();
    private LocalDateTime dataCriacao = LocalDateTime.now();
    private LocalDateTime dataPublicacao;

    public Prova() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDisciplina() { return disciplina; }
    public void setDisciplina(String disciplina) { this.disciplina = disciplina; }

    public String getProfessorEmail() { return professorEmail; }
    public void setProfessorEmail(String professorEmail) { this.professorEmail = professorEmail; }

    public String getProfessorNome() { return professorNome; }
    public void setProfessorNome(String professorNome) { this.professorNome = professorNome; }

    public StatusProva getStatus() { return status; }
    public void setStatus(StatusProva status) { this.status = status; }

    public List<Questao> getQuestoes() { return questoes; }
    public void setQuestoes(List<Questao> questoes) { this.questoes = questoes; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataPublicacao() { return dataPublicacao; }
    public void setDataPublicacao(LocalDateTime dataPublicacao) { this.dataPublicacao = dataPublicacao; }

    public double getValorTotal() {
        return questoes.stream().mapToDouble(Questao::getValor).sum();
    }
}
