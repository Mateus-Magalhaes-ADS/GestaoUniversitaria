package br.com.uniconecta.pagamento.gateway;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gera um boleto FICTÍCIO, mas com o formato real da FEBRABAN:
 * código de barras de 44 dígitos (com dígito verificador módulo 11)
 * e linha digitável de 47 dígitos (com dígitos verificadores módulo 10).
 */
public final class GeradorBoleto {

    /** Código de banco fictício usado apenas para demonstração. */
    static final String BANCO = "999";
    static final String MOEDA_REAL = "9";

    private static final LocalDate BASE_FATOR = LocalDate.of(1997, 10, 7);
    private static final LocalDate REINICIO_FATOR = LocalDate.of(2025, 2, 22);

    private GeradorBoleto() {}

    public record Boleto(String codigoBarras, String linhaDigitavel, LocalDate vencimento) {}

    public static Boleto gerar(BigDecimal valor, long referencia, LocalDate vencimento) {
        String fator = fatorVencimento(vencimento);
        String valorCentavos = String.format("%010d", valor.movePointRight(2).longValueExact());
        String campoLivre = String.format("%010d", referencia) + numerosAleatorios(15);

        String semDv = BANCO + MOEDA_REAL + fator + valorCentavos + campoLivre; // 43 dígitos
        int dv = dvCodigoBarras(semDv);
        String codigoBarras = semDv.substring(0, 4) + dv + semDv.substring(4);

        return new Boleto(codigoBarras, linhaDigitavel(codigoBarras), vencimento);
    }

    /** Fator de vencimento (regra FEBRABAN, reiniciado em 1000 a partir de 22/02/2025). */
    static String fatorVencimento(LocalDate vencimento) {
        long fator = vencimento.isBefore(REINICIO_FATOR)
                ? ChronoUnit.DAYS.between(BASE_FATOR, vencimento)
                : 1000 + ChronoUnit.DAYS.between(REINICIO_FATOR, vencimento);
        return String.format("%04d", fator);
    }

    /** Dígito verificador geral do código de barras (módulo 11, pesos 2 a 9). */
    static int dvCodigoBarras(String numero) {
        int soma = 0;
        int peso = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            soma += Character.getNumericValue(numero.charAt(i)) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = 11 - (soma % 11);
        return (resto == 0 || resto == 10 || resto == 11) ? 1 : resto;
    }

    /** Dígito verificador módulo 10 (usado nos campos da linha digitável). */
    static int modulo10(String numero) {
        int soma = 0;
        int multiplicador = 2;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int produto = Character.getNumericValue(numero.charAt(i)) * multiplicador;
            soma += produto > 9 ? produto - 9 : produto;
            multiplicador = multiplicador == 2 ? 1 : 2;
        }
        return (10 - (soma % 10)) % 10;
    }

    /** Monta a linha digitável formatada: AAAAA.AAAAA BBBBB.BBBBBB CCCCC.CCCCCC D EEEEEEEEEEEEEE */
    static String linhaDigitavel(String codigoBarras) {
        String campoLivre = codigoBarras.substring(19);
        String c1 = codigoBarras.substring(0, 4) + campoLivre.substring(0, 5);
        String c2 = campoLivre.substring(5, 15);
        String c3 = campoLivre.substring(15, 25);
        c1 += modulo10(c1);
        c2 += modulo10(c2);
        c3 += modulo10(c3);
        String c4 = codigoBarras.substring(4, 5);
        String c5 = codigoBarras.substring(5, 19);
        return c1.substring(0, 5) + "." + c1.substring(5) + " "
                + c2.substring(0, 5) + "." + c2.substring(5) + " "
                + c3.substring(0, 5) + "." + c3.substring(5) + " "
                + c4 + " " + c5;
    }

    private static String numerosAleatorios(int tamanho) {
        StringBuilder sb = new StringBuilder(tamanho);
        for (int i = 0; i < tamanho; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}
