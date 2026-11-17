package br.com.uniconecta.pagamento.gateway;

import java.time.YearMonth;
import java.util.List;

/** Validações de cartão de crédito usadas pelo gateway simulado. */
public final class ValidadorCartao {

    /** Cartão de teste que o gateway simulado sempre RECUSA (para demonstrar o erro). */
    public static final String CARTAO_RECUSADO = "4000000000000002";

    private static final List<String> PREFIXOS_ELO = List.of(
            "401178", "401179", "431274", "438935", "451416", "457393", "457631", "457632",
            "504175", "506699", "5067", "509", "627780", "636297", "636368", "6504", "6505", "6516", "6550");

    private ValidadorCartao() {}

    public static String somenteDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    /** Algoritmo de Luhn (módulo 10), usado por todas as bandeiras. */
    public static boolean numeroValido(String numero) {
        String digitos = somenteDigitos(numero);
        if (digitos.length() < 13 || digitos.length() > 19) return false;
        int soma = 0;
        boolean dobrar = false;
        for (int i = digitos.length() - 1; i >= 0; i--) {
            int d = digitos.charAt(i) - '0';
            if (dobrar) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            soma += d;
            dobrar = !dobrar;
        }
        return soma % 10 == 0;
    }

    public static String bandeira(String numero) {
        String n = somenteDigitos(numero);
        if (PREFIXOS_ELO.stream().anyMatch(n::startsWith)) return "Elo";
        if (n.startsWith("606282") || n.startsWith("3841")) return "Hipercard";
        if (n.startsWith("34") || n.startsWith("37")) return "American Express";
        if (n.length() >= 4) {
            int p2 = Integer.parseInt(n.substring(0, 2));
            int p4 = Integer.parseInt(n.substring(0, 4));
            if ((p2 >= 51 && p2 <= 55) || (p4 >= 2221 && p4 <= 2720)) return "Mastercard";
        }
        if (n.startsWith("4")) return "Visa";
        return "Outra";
    }

    /** Validade no formato MM/AA. O cartão vale até o último dia do mês informado. */
    public static boolean validadeValida(String validade, YearMonth mesAtual) {
        if (validade == null || !validade.matches("\\d{2}/\\d{2}")) return false;
        int mes = Integer.parseInt(validade.substring(0, 2));
        int ano = 2000 + Integer.parseInt(validade.substring(3));
        if (mes < 1 || mes > 12) return false;
        return !YearMonth.of(ano, mes).isBefore(mesAtual);
    }

    public static boolean cvcValido(String cvc, String bandeira) {
        String digitos = somenteDigitos(cvc);
        int esperado = "American Express".equals(bandeira) ? 4 : 3;
        return digitos.length() == esperado && digitos.equals(cvc.trim());
    }
}
