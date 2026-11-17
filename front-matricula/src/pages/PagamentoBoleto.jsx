import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import JsBarcode from 'jsbarcode';
import { CabecalhoPasso, Carregando, MensagemErro, Tela } from '../components/Layout';
import { IconeCopiar, IconeImprimir } from '../components/Icones';
import { useMatricula } from '../useMatricula';
import { copiarTexto, formatarData, formatarMoeda } from '../services/formatar';

export default function PagamentoBoleto() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { matricula, erro } = useMatricula(id);
  const codigoRef = useRef(null);
  const [copiado, setCopiado] = useState(false);
  const pagamento = matricula?.pagamento;

  useEffect(() => {
    if (pagamento?.codigoBarras && codigoRef.current) {
      // Boletos usam o padrão "Intercalado 2 de 5" (ITF) com 44 dígitos.
      JsBarcode(codigoRef.current, pagamento.codigoBarras, {
        format: 'ITF',
        displayValue: false,
        height: 56,
        width: 1.4,
        margin: 0,
      });
    }
  }, [pagamento?.codigoBarras]);

  async function copiar() {
    if (await copiarTexto(pagamento.linhaDigitavel.replace(/\D/g, ''))) {
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2500);
    }
  }

  return (
    <Tela
      cabecalho={<CabecalhoPasso passo={2} subtitulo="Geração do boleto" voltarPara="/matricula" />}
      rodape={
        <>
          <button type="button" className="botao botao--secundario" onClick={() => navigate('/matricula')}>
            Voltar
          </button>
          <button
            type="button"
            className="botao botao--primario"
            disabled={!pagamento}
            onClick={() => navigate(`/matricula/${id}/boleto/status`)}
          >
            Continuar
          </button>
        </>
      }
    >
      <MensagemErro>{erro}</MensagemErro>
      {!matricula && !erro && <Carregando />}

      {pagamento && (
        <section className="boleto area-impressao">
          <p className="boleto__valor">
            <strong>Pagamento único:</strong> {formatarMoeda(pagamento.valor)}
          </p>
          <p className="impressao-apenas">
            {matricula.curso.nome} - vencimento {formatarData(pagamento.vencimento)}
          </p>
          <svg ref={codigoRef} className="boleto__barras" role="img" aria-label="Código de barras do boleto" />
          <p className="boleto__linha">{pagamento.linhaDigitavel}</p>
          <p className="boleto__vencimento">Vencimento: {formatarData(pagamento.vencimento)}</p>

          <div className="boleto__acoes nao-imprimir">
            <button type="button" className="botao-icone" onClick={copiar}>
              <IconeCopiar /> {copiado ? 'Código copiado!' : 'Copiar código de barras'}
            </button>
            <button type="button" className="botao-icone" onClick={() => window.print()}>
              <IconeImprimir /> Imprimir boleto
            </button>
          </div>
        </section>
      )}
    </Tela>
  );
}
