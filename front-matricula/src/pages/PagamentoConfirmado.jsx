import { useNavigate, useParams } from 'react-router-dom';
import { CabecalhoPasso, Carregando, MensagemErro, Tela } from '../components/Layout';
import { IconeCheck } from '../components/Icones';
import Logo from '../components/Logo';
import { useMatricula } from '../useMatricula';
import { formatarData, formatarMoeda } from '../services/formatar';

export default function PagamentoConfirmado() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { matricula, erro } = useMatricula(id);
  const p = matricula?.pagamento;

  const valor =
    p && p.parcelas > 1 ? `${p.parcelas}x ${formatarMoeda(p.valorParcela)}` : p && formatarMoeda(p.valor);

  return (
    <Tela
      className="tela__conteudo--centro"
      cabecalho={<CabecalhoPasso passo={3} subtitulo="Status do pagamento" voltarPara="/" />}
      rodape={
        <button
          type="button"
          className="botao botao--primario"
          disabled={!p}
          onClick={() => navigate(`/matricula/${id}/confirmada`)}
        >
          Finalizar
        </button>
      }
    >
      <Logo />
      <MensagemErro>{erro}</MensagemErro>
      {!matricula && !erro && <Carregando />}

      {p && p.status !== 'APROVADO' && <p className="aviso">Este pagamento ainda não foi confirmado.</p>}

      {p && p.status === 'APROVADO' && (
        <>
          <IconeCheck className="check" />
          <h2 className="titulo-sucesso">Pagamento confirmado!</h2>
          <dl className="detalhes">
            <div>
              <dt>Valor:</dt>
              <dd>{valor}</dd>
            </div>
            <div>
              <dt>Data:</dt>
              <dd>{formatarData(p.pagoEm)}</dd>
            </div>
            <div>
              <dt>ID da transação:</dt>
              <dd>{p.codigoTransacao}</dd>
            </div>
            {p.cartaoFinal && (
              <div>
                <dt>Cartão:</dt>
                <dd>
                  {p.cartaoBandeira} final {p.cartaoFinal}
                </dd>
              </div>
            )}
          </dl>
        </>
      )}
    </Tela>
  );
}
