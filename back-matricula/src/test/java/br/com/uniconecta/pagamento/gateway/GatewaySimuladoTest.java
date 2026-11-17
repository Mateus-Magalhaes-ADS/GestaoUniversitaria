package br.com.uniconecta.pagamento.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import br.com.uniconecta.common.ValidadorCpf;

class GatewaySimuladoTest {

    // ---------- Boleto ----------

    @Test
    void linhaDigitavelDeBoletoRealDoExemploFebraban() {
        String codigoBarras = "00193373700000001000500940144816060680935031";
        assertEquals("00190.50095 40144.816069 06809.350314 3 37370000000100",
                GeradorBoleto.linhaDigitavel(codigoBarras));
        assertEquals(3, GeradorBoleto.dvCodigoBarras(codigoBarras.substring(0, 4) + codigoBarras.substring(5)));
    }

    @Test
    void boletoGeradoTem44DigitosEValorCorreto() {
        var boleto = GeradorBoleto.gerar(new BigDecimal("9000.00"), 15, LocalDate.of(2026, 11, 18));
        String cb = boleto.codigoBarras();
        assertEquals(44, cb.length());
        assertTrue(cb.matches("\\d{44}"));
        assertEquals("0000900000", cb.substring(9, 19));
        int dv = Character.getNumericValue(cb.charAt(4));
        assertEquals(dv, GeradorBoleto.dvCodigoBarras(cb.substring(0, 4) + cb.substring(5)));
        assertEquals(54, boleto.linhaDigitavel().length()); // 47 dígitos + separadores
    }

    @Test
    void fatorDeVencimentoReiniciaEm2025() {
        assertEquals("9999", GeradorBoleto.fatorVencimento(LocalDate.of(2025, 2, 21)));
        assertEquals("1000", GeradorBoleto.fatorVencimento(LocalDate.of(2025, 2, 22)));
    }

    // ---------- Pix ----------

    @Test
    void crc16ValorDeReferencia() {
        assertEquals("29B1", GeradorPix.crc16("123456789"));
    }

    @Test
    void pixCopiaEColaTemFormatoBrCode() {
        String pix = GeradorPix.gerar("financeiro@uniconecta.edu.br", "UniConecta Educacional",
                "São Paulo", new BigDecimal("9000"), "MAT1");
        assertTrue(pix.startsWith("000201"));
        assertTrue(pix.contains("br.gov.bcb.pix"));
        assertTrue(pix.contains("54079000.00"));
        assertTrue(pix.contains("6009SAO PAULO"));
        String semCrc = pix.substring(0, pix.length() - 4);
        assertEquals(GeradorPix.crc16(semCrc), pix.substring(pix.length() - 4));
    }

    // ---------- Cartão ----------

    @Test
    void validaNumeroDoCartaoPorLuhn() {
        assertTrue(ValidadorCartao.numeroValido("4111 1111 1111 1111"));
        assertTrue(ValidadorCartao.numeroValido("5555555555554444"));
        assertFalse(ValidadorCartao.numeroValido("4111111111111112"));
        assertFalse(ValidadorCartao.numeroValido("1234"));
    }

    @Test
    void identificaBandeira() {
        assertEquals("Visa", ValidadorCartao.bandeira("4111111111111111"));
        assertEquals("Mastercard", ValidadorCartao.bandeira("5555555555554444"));
        assertEquals("American Express", ValidadorCartao.bandeira("378282246310005"));
        assertEquals("Elo", ValidadorCartao.bandeira("6362970000457013"));
    }

    @Test
    void validaVencimentoECvc() {
        YearMonth nov2026 = YearMonth.of(2026, 11);
        assertTrue(ValidadorCartao.validadeValida("11/26", nov2026));
        assertTrue(ValidadorCartao.validadeValida("01/30", nov2026));
        assertFalse(ValidadorCartao.validadeValida("10/26", nov2026));
        assertFalse(ValidadorCartao.validadeValida("13/30", nov2026));
        assertTrue(ValidadorCartao.cvcValido("123", "Visa"));
        assertFalse(ValidadorCartao.cvcValido("12", "Visa"));
        assertTrue(ValidadorCartao.cvcValido("1234", "American Express"));
    }

    // ---------- CPF ----------

    @Test
    void validaCpf() {
        assertTrue(ValidadorCpf.valido("123.456.789-09"));
        assertTrue(ValidadorCpf.valido("11144477735"));
        assertFalse(ValidadorCpf.valido("123.456.789-00"));
        assertFalse(ValidadorCpf.valido("111.111.111-11"));
    }
}
