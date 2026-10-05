package com.universidade.gestao.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RespostaProva {

    private Long id;
    private Long provaId;
    private String alunoEmail;
    private String alunoNome;
    private List<RespostaQuestao> respostas = new ArrayList<>();
    private Double notaFinal;
    private boolean corrigida;
    private boolean publicada;
    private LocalDateTime dataEnvio = LocalDateTime.now();
    private LocalDateTime dataCorrecao;

    public RespostaProva() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProvaId() { return provaId; }
    public void setProvaId(Long provaId) { this.provaId = provaId; }

    public String getAlunoEmail() { return alunoEmail; }
    public void setAlunoEmail(String alunoEmail) { this.alunoEmail = alunoEmail; }

    public String getAlunoNome() { return alunoNome; }
    public void setAlunoNome(String alunoNome) { this.alunoNome = alunoNome; }

    public List<RespostaQuestao> getRespostas() { return respostas; }
    public void setRespostas(List<RespostaQuestao> respostas) { this.respostas = respostas; }

    public Double getNotaFinal() { return notaFinal; }
    public void setNotaFinal(Double notaFinal) { this.notaFinal = notaFinal; }

    public boolean isCorrigida() { return corrigida; }
    public void setCorrigida(boolean corrigida) { this.corrigida = corrigida; }

    public boolean isPublicada() { return publicada; }
    public void setPublicada(boolean publicada) { this.publicada = publicada; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }

    public LocalDateTime getDataCorrecao() { return dataCorrecao; }
    public void setDataCorrecao(LocalDateTime dataCorrecao) { this.dataCorrecao = dataCorrecao; }
}
