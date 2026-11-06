package com.universidade.gestao.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "respostas_prova")
public class RespostaProva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long provaId;
    @Column(nullable = false)
    private String alunoEmail;
    @Column(nullable = false)
    private String alunoNome;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<RespostaQuestao> respostas = new ArrayList<>();
    private Double notaFinal;
    @Column(nullable = false)
    private boolean corrigida;
    @Column(nullable = false)
    private boolean publicada;
    @Column(nullable = false)
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
