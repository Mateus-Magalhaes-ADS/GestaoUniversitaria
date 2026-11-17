import { useNavigate } from 'react-router-dom';
import { CabecalhoPasso, Tela } from '../components/Layout';
import Logo from '../components/Logo';

export default function StatusBoleto() {
  const navigate = useNavigate();
  return (
    <Tela
      className="tela__conteudo--centro"
      cabecalho={<CabecalhoPasso passo={3} subtitulo="Status da matrícula" />}
      rodape={
        <button type="button" className="botao botao--primario" onClick={() => navigate('/')}>
          Fechar
        </button>
      }
    >
      <Logo />
      <div className="caixa-aviso">
        <strong>CARO ALUNO</strong>
        <p>A matrícula será efetivada somente após o processamento do pagamento do boleto.</p>
      </div>
    </Tela>
  );
}
