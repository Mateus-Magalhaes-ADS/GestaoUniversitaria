const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export const formatarMoeda = (valor) => moeda.format(Number(valor || 0));

export function formatarData(iso) {
  if (!iso) return '-';
  // "2026-11-15" (LocalDate) ou "2026-11-15T22:30:00" (LocalDateTime)
  const [ano, mes, dia] = iso.substring(0, 10).split('-');
  return `${dia}/${mes}/${ano}`;
}

export const formatarDuracao = (meses) => `${meses} ${meses === 1 ? 'mês' : 'meses'}`;

// ---------- Máscaras de digitação ----------
const digitos = (v) => v.replace(/\D/g, '');

export function mascaraCpf(v) {
  return digitos(v)
    .slice(0, 11)
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
}

export function mascaraCartao(v) {
  return digitos(v).slice(0, 19).replace(/(\d{4})(?=\d)/g, '$1 ');
}

export function mascaraValidade(v) {
  return digitos(v).slice(0, 4).replace(/(\d{2})(\d)/, '$1/$2');
}

export const mascaraCvc = (v) => digitos(v).slice(0, 4);

export async function copiarTexto(texto) {
  try {
    await navigator.clipboard.writeText(texto);
    return true;
  } catch {
    // Alternativa para navegadores sem permissão de área de transferência
    const area = document.createElement('textarea');
    area.value = texto;
    document.body.appendChild(area);
    area.select();
    const ok = document.execCommand('copy');
    area.remove();
    return ok;
  }
}
