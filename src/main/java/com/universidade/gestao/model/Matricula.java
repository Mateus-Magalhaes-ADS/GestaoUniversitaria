package com.universidade.gestao.model;

import java.time.LocalDateTime;

public class Matricula {

    private Long id;

    private String nomeCompleto;
    private String email;
    private String cpf;
    private String endereco;
    private String cep;
    private Long cursoId;
    
    private String documentoIdentidadeNome;
    private String documentoIdentidadeContentType;
    private byte[] documentoIdentidadeConteudo;
    private String historicoEscolarNome;
    private String historicoEscolarContentType;
    private byte[] historicoEscolarConteudo;
    
    private LocalDateTime dataSolicitacao = LocalDateTime.now();

    public Matricula() {}

    public Matricula(String nomeCompleto, String email, String cpf, Long cursoId) {
        this.nomeCompleto = nomeCompleto;
        this.email = email;
        this.cpf = cpf;
        this.cursoId = cursoId;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }

    public String getDocumentoIdentidadeNome() { return documentoIdentidadeNome; }
    public void setDocumentoIdentidadeNome(String documentoIdentidadeNome) { this.documentoIdentidadeNome = documentoIdentidadeNome; }

    public String getDocumentoIdentidadeContentType() { return documentoIdentidadeContentType; }
    public void setDocumentoIdentidadeContentType(String documentoIdentidadeContentType) { this.documentoIdentidadeContentType = documentoIdentidadeContentType; }

    public byte[] getDocumentoIdentidadeConteudo() { return documentoIdentidadeConteudo; }
    public void setDocumentoIdentidadeConteudo(byte[] documentoIdentidadeConteudo) { this.documentoIdentidadeConteudo = documentoIdentidadeConteudo; }

    public String getHistoricoEscolarNome() { return historicoEscolarNome; }
    public void setHistoricoEscolarNome(String historicoEscolarNome) { this.historicoEscolarNome = historicoEscolarNome; }

    public String getHistoricoEscolarContentType() { return historicoEscolarContentType; }
    public void setHistoricoEscolarContentType(String historicoEscolarContentType) { this.historicoEscolarContentType = historicoEscolarContentType; }

    public byte[] getHistoricoEscolarConteudo() { return historicoEscolarConteudo; }
    public void setHistoricoEscolarConteudo(byte[] historicoEscolarConteudo) { this.historicoEscolarConteudo = historicoEscolarConteudo; }

    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(LocalDateTime dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }
}
