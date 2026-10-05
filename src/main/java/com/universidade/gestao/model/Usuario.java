package com.universidade.gestao.model;

public class Usuario {

    private Long id;

    private String nome;

    private String email;

    private String senha;

    private Role perfil;

    public Usuario() {}

    public Usuario(String nome, String email, String senha, Role perfil) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.perfil = perfil;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public Role getPerfil() { return perfil; }
    public void setPerfil(Role perfil) { this.perfil = perfil; }
    
}
