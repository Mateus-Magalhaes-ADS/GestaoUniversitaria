package br.com.uniconecta.pagamento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.uniconecta.matricula.Matricula;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", unique = true)
    private Matricula matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private Integer parcelas = 1;

    @Column(name = "valor_parcela", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "codigo_transacao", length = 30)
    private String codigoTransacao;

    // ----- Boleto -----
    @Column(name = "codigo_barras", length = 44)
    private String codigoBarras;

    @Column(name = "linha_digitavel", length = 60)
    private String linhaDigitavel;

    private LocalDate vencimento;

    // ----- Pix -----
    @Column(name = "pix_copia_cola", length = 512)
    private String pixCopiaCola;

    // ----- Cartão (somente dados não sensíveis) -----
    @Column(name = "cartao_final", length = 4)
    private String cartaoFinal;

    @Column(name = "cartao_bandeira", length = 20)
    private String cartaoBandeira;

    @Column(name = "cartao_titular", length = 120)
    private String cartaoTitular;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    protected Pagamento() {}

    public Pagamento(Matricula matricula) {
        this.matricula = matricula;
        this.metodo = matricula.getFormaPagamento();
        this.valor = matricula.getValorTotal();
        this.valorParcela = matricula.getValorTotal();
    }

    public boolean isPendente() {
        return status == StatusPagamento.PENDENTE;
    }

    public void definirParcelas(int parcelas) {
        this.parcelas = parcelas;
        this.valorParcela = valor.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);
    }

    public void registrarBoleto(String codigoBarras, String linhaDigitavel, LocalDate vencimento) {
        this.codigoBarras = codigoBarras;
        this.linhaDigitavel = linhaDigitavel;
        this.vencimento = vencimento;
    }

    public void registrarPix(String copiaCola) {
        this.pixCopiaCola = copiaCola;
    }

    public void registrarCartao(String finalCartao, String bandeira, String titular) {
        this.cartaoFinal = finalCartao;
        this.cartaoBandeira = bandeira;
        this.cartaoTitular = titular;
    }

    /** Aprova o pagamento e confirma a matrícula vinculada. */
    public void aprovar(String codigoTransacao) {
        this.status = StatusPagamento.APROVADO;
        this.codigoTransacao = codigoTransacao;
        this.pagoEm = LocalDateTime.now();
        this.matricula.confirmar();
    }

    public void cancelar() {
        this.status = StatusPagamento.CANCELADO;
    }

    public Long getId() { return id; }
    public Matricula getMatricula() { return matricula; }
    public FormaPagamento getMetodo() { return metodo; }
    public StatusPagamento getStatus() { return status; }
    public BigDecimal getValor() { return valor; }
    public Integer getParcelas() { return parcelas; }
    public BigDecimal getValorParcela() { return valorParcela; }
    public String getCodigoTransacao() { return codigoTransacao; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getLinhaDigitavel() { return linhaDigitavel; }
    public LocalDate getVencimento() { return vencimento; }
    public String getPixCopiaCola() { return pixCopiaCola; }
    public String getCartaoFinal() { return cartaoFinal; }
    public String getCartaoBandeira() { return cartaoBandeira; }
    public String getCartaoTitular() { return cartaoTitular; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getPagoEm() { return pagoEm; }
}
