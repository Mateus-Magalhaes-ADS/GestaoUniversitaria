import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { CabecalhoSecao, Carregando, MensagemErro, Tela } from '../components/Layout';
import Logo from '../components/Logo';
import { MatriculaApi, PagamentoApi } from '../services/api';
import { formatarData, formatarMoeda } from '../services/formatar';

const STATUS = {
  CONFIRMADA: { texto: 'Confirmada', classe: 'selo--ok' },
  AGUARDANDO_PAGAMENTO: { texto: 'Aguardando pagamento', classe: 'selo--pendente' },
};

const ROTA_PAGAMENTO = { BOLETO: 'boleto', CARTAO: 'cartao', PIX: 'pix' };

export default function Inicio() {
  const { aluno } = useAuth();
  const navigate = useNavigate();
  const [matriculas, setMatriculas] = useState(null);
  const [erro, setErro] = useState('');
  const [processando, setProcessando] = useState(null);

  const carregar = useCallback(() => {
    MatriculaApi.minhas()
      .then(setMatriculas)
      .catch((e) => setErro(e.message));
  }, []);

  useEffect(carregar, [carregar]);

  async function simularCompensacao(m) {
    setProcessando(m.id);
    try {
      await PagamentoApi.simularConfirmacao(m.pagamento.id);
      navigate(`/matricula/${m.id}/pagamento-confirmado`);
    } catch (e) {
      setErro(e.message);
      setProcessando(null);
    }
  }

  return (
    <Tela
      largura="larga"
      cabecalho={<CabecalhoSecao titulo={`Olá, ${aluno?.nome?.split(' ')[0] ?? ''}!`} subtitulo="Bem-vindo ao portal do aluno" />}
    >
      <div className="inicio__logo">
        <Logo tamanho="pequeno" />
      </div>

      <h2 className="secao-titulo">Minhas matrículas</h2>
      <MensagemErro>{erro}</MensagemErro>
      {!matriculas && !erro && <Carregando />}

      {matriculas?.length === 0 && (
        <div className="vazio">
          <p>Você ainda não possui matrículas.</p>
          <Link to="/matricula" className="botao botao--primario">
            Fazer matrícula
          </Link>
        </div>
      )}

      <div className="grade">
        {matriculas?.map((m) => {
          const status = STATUS[m.status];
          const p = m.pagamento;
          return (
            <article key={m.id} className="cartao">
              <div className="cartao__topo">
                <h3>{m.curso.nome}</h3>
                <span className={`selo ${status.classe}`}>{status.texto}</span>
              </div>
              <p className="cartao__info">
                {m.turnoDescricao} · {formatarMoeda(m.valorTotal)} · {p?.metodoDescricao}
              </p>
              <p className="cartao__info">Solicitada em {formatarData(m.criadoEm)}</p>

              <div className="cartao__acoes">
                {m.status === 'CONFIRMADA' && (
                  <Link to={`/matricula/${m.id}/confirmada`} className="botao botao--secundario botao--pequeno">
                    Ver detalhes
                  </Link>
                )}
                {m.status === 'AGUARDANDO_PAGAMENTO' && (
                  <Link
                    to={`/matricula/${m.id}/${ROTA_PAGAMENTO[m.formaPagamento]}`}
                    className="botao botao--primario botao--pequeno"
                  >
                    {m.formaPagamento === 'BOLETO' ? 'Ver boleto' : 'Continuar pagamento'}
                  </Link>
                )}
                {m.status === 'AGUARDANDO_PAGAMENTO' && m.formaPagamento === 'BOLETO' && (
                  <button
                    type="button"
                    className="botao botao--secundario botao--pequeno"
                    disabled={processando === m.id}
                    onClick={() => simularCompensacao(m)}
                    title="Simula o banco confirmando o pagamento do boleto"
                  >
                    {processando === m.id ? 'Processando...' : 'Simular compensação'}
                  </button>
                )}
              </div>
            </article>
          );
        })}
      </div>

      {matriculas?.length > 0 && (
        <Link to="/matricula" className="link-acao">
          + Nova matrícula
        </Link>
      )}
    </Tela>
  );
}
