import { useNavigate, useParams } from 'react-router-dom';
import { CabecalhoPasso, Carregando, MensagemErro, Tela } from '../components/Layout';
import Logo from '../components/Logo';
import { useMatricula } from '../useMatricula';
import { formatarDuracao } from '../services/formatar';

export default function MatriculaConfirmada() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { matricula, erro } = useMatricula(id);

  return (
    <Tela
      className="tela__conteudo--centro"
      cabecalho={<CabecalhoPasso passo={4} subtitulo="Status da matrícula" voltarPara="/" />}
      rodape={
        <button type="button" className="botao botao--primario" onClick={() => navigate('/')}>
          Fechar
        </button>
      }
    >
      <Logo />
      <MensagemErro>{erro}</MensagemErro>
      {!matricula && !erro && <Carregando />}

      {matricula && matricula.status !== 'CONFIRMADA' && (
        <p className="aviso">A matrícula será confirmada assim que o pagamento for processado.</p>
      )}

      {matricula?.status === 'CONFIRMADA' && (
        <>
          <h2 className="titulo-sucesso">Matrícula confirmada!</h2>
          <dl className="detalhes">
            <div>
              <dt>Curso:</dt>
              <dd>{matricula.curso.nome}</dd>
            </div>
            <div>
              <dt>Período:</dt>
              <dd>{formatarDuracao(matricula.curso.duracaoMeses)}</dd>
            </div>
            <div>
              <dt>Turno:</dt>
              <dd>{matricula.turnoDescricao}</dd>
            </div>
          </dl>
        </>
      )}
    </Tela>
  );
}
