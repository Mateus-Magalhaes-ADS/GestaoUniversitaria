package com.universidade.gestao.model;

public class RespostaQuestao {

    private Long questaoId;
    private String textoResposta;
    private Integer alternativaEscolhida;
    private Double nota;
    private String comentarioProfessor;

    public RespostaQuestao() {}

    public Long getQuestaoId() { return questaoId; }
    public void setQuestaoId(Long questaoId) { this.questaoId = questaoId; }

    public String getTextoResposta() { return textoResposta; }
    public void setTextoResposta(String textoResposta) { this.textoResposta = textoResposta; }

    public Integer getAlternativaEscolhida() { return alternativaEscolhida; }
    public void setAlternativaEscolhida(Integer alternativaEscolhida) { this.alternativaEscolhida = alternativaEscolhida; }

    public Double getNota() { return nota; }
    public void setNota(Double nota) { this.nota = nota; }

    public String getComentarioProfessor() { return comentarioProfessor; }
    public void setComentarioProfessor(String comentarioProfessor) { this.comentarioProfessor = comentarioProfessor; }
}
