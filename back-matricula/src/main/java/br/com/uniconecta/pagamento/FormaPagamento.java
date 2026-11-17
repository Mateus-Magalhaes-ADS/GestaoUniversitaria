package br.com.uniconecta.pagamento;

public enum FormaPagamento {
    BOLETO("Boleto"),
    CARTAO("Cartão de crédito"),
    PIX("Pix");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
