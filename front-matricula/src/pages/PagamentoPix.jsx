import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import QRCode from 'qrcode';
import { CabecalhoPasso, Carregando, MensagemErro, Tela } from '../components/Layout';
import { IconeCopiar } from '../components/Icones';
import { PagamentoApi } from '../services/api';
import { useMatricula } from '../useMatricula';
import { copiarTexto, formatarMoeda } from '../services/formatar';

const INTERVALO_CONSULTA_MS = 3000;

export default function PagamentoPix() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { matricula, erro } = useMatricula(id);
  const [qrCode, setQrCode] = useState('');
  const [copiado, setCopiado] = useState(false);
  const pagamento = matricula?.pagamento;

  useEffect(() => {
    if (pagamento?.pixCopiaCola) {
      QRCode.toDataURL(pagamento.pixCopiaCola, { width: 360, margin: 1 }).then(setQrCode);
    }
  }, [pagamento?.pixCopiaCola]);

  // Consulta o status a cada 3 segundos até o Pix ser confirmado.
  useEffect(() => {
    if (!pagamento) return undefined;
    if (pagamento.status === 'APROVADO') {
      navigate(`/matricula/${id}/pagamento-confirmado`, { replace: true });
      return undefined;
    }
    const timer = setInterval(async () => {
      try {
        const atual = await PagamentoApi.buscar(pagamento.id);
        if (atual.status === 'APROVADO') navigate(`/matricula/${id}/pagamento-confirmado`, { replace: true });
      } catch {
        /* tenta de novo no próximo ciclo */
      }
    }, INTERVALO_CONSULTA_MS);
    return () => clearInterval(timer);
  }, [pagamento, id, navigate]);

  async function copiar() {
    if (await copiarTexto(pagamento.pixCopiaCola)) {
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2500);
    }
  }

  return (
    <Tela
      cabecalho={<CabecalhoPasso passo={2} subtitulo="Dados do Pix" voltarPara="/matricula" />}
      rodape={
        <button type="button" className="botao botao--secundario" onClick={() => navigate('/matricula')}>
          Voltar
        </button>
      }
    >
      <MensagemErro>{erro}</MensagemErro>
      {!matricula && !erro && <Carregando />}

      {pagamento && (
        <section className="pix">
          <div className="pix__codigo">
            <p>{pagamento.pixCopiaCola}</p>
            <button type="button" className="botao-icone" onClick={copiar}>
              <IconeCopiar /> {copiado ? 'Copiado!' : 'Copia e cola'}
            </button>
          </div>

          <div className="pix__qr">
            {qrCode ? <img src={qrCode} alt="QR Code do Pix" /> : <span className="spinner" aria-hidden />}
          </div>

          <p className="pix__valor">Valor: {formatarMoeda(pagamento.valor)}</p>
          <p className="pix__aguardando">
            <span className="spinner spinner--pequeno" aria-hidden /> Aguardando confirmação do pagamento...
          </p>
        </section>
      )}
    </Tela>
  );
}
