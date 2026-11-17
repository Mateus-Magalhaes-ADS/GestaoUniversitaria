package br.com.uniconecta.pagamento.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;

/**
 * Gera o código "Pix Copia e Cola" (BR Code / padrão EMV do Banco Central),
 * com CRC16 válido. A chave e o recebedor são FICTÍCIOS.
 */
public final class GeradorPix {

    private GeradorPix() {}

    public static String gerar(String chave, String recebedor, String cidade, BigDecimal valor, String txid) {
        String contaRecebedor = campo("00", "br.gov.bcb.pix") + campo("01", chave);
        String adicionais = campo("05", somenteAlfanumerico(txid, 25));

        String payload = campo("00", "01")
                + campo("26", contaRecebedor)
                + campo("52", "0000")
                + campo("53", "986")
                + campo("54", valor.setScale(2, RoundingMode.HALF_UP).toPlainString())
                + campo("58", "BR")
                + campo("59", limpar(recebedor, 25))
                + campo("60", limpar(cidade, 15))
                + campo("62", adicionais)
                + "6304";
        return payload + crc16(payload);
    }

    private static String campo(String id, String valor) {
        return id + String.format("%02d", valor.length()) + valor;
    }

    /** CRC16-CCITT (polinômio 0x1021, valor inicial 0xFFFF), exigido pelo BR Code. */
    static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    private static String limpar(String texto, int max) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String resultado = semAcento.toUpperCase().replaceAll("[^A-Z0-9 ]", "").trim();
        return resultado.length() > max ? resultado.substring(0, max) : resultado;
    }

    private static String somenteAlfanumerico(String texto, int max) {
        String resultado = texto.replaceAll("[^A-Za-z0-9]", "");
        return resultado.length() > max ? resultado.substring(0, max) : resultado;
    }
}
