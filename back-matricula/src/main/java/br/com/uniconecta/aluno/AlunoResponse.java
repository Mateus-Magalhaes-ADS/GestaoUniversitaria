package br.com.uniconecta.aluno;

/** Dados do aluno devolvidos ao app (sem a senha). */
public record AlunoResponse(Long id, String nome, String email, String cpf, String telefone) {

    public static AlunoResponse de(Aluno aluno) {
        return new AlunoResponse(aluno.getId(), aluno.getNome(), aluno.getEmail(),
                mascararCpf(aluno.getCpf()), aluno.getTelefone());
    }

    /** 12345678909 -> ***.456.789-** */
    private static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
    }
}
