import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import { CabecalhoSecao, Carregando, MensagemErro, Tela } from '../components/Layout';
import { IconePerfil } from '../components/Icones';
import { AuthApi } from '../services/api';

export default function Perfil() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [dados, setDados] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    AuthApi.me()
      .then(setDados)
      .catch((e) => setErro(e.message));
  }, []);

  function sair() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <Tela
      className="tela__conteudo--centro"
      cabecalho={<CabecalhoSecao titulo="Perfil" subtitulo="Seus dados cadastrais" />}
      rodapeSoCelular
      rodape={
        <button type="button" className="botao botao--secundario" onClick={sair}>
          Sair
        </button>
      }
    >
      <MensagemErro>{erro}</MensagemErro>
      {!dados && !erro && <Carregando />}
      {dados && (
        <>
          <IconePerfil className="perfil__avatar" width={72} height={72} />
          <h2 className="perfil__nome">{dados.nome}</h2>
          <dl className="detalhes">
            <div>
              <dt>E-mail:</dt>
              <dd>{dados.email}</dd>
            </div>
            <div>
              <dt>CPF:</dt>
              <dd>{dados.cpf}</dd>
            </div>
            <div>
              <dt>Telefone:</dt>
              <dd>{dados.telefone || '-'}</dd>
            </div>
            <div>
              <dt>Matrícula nº:</dt>
              <dd>{String(dados.id).padStart(6, '0')}</dd>
            </div>
          </dl>
        </>
      )}
    </Tela>
  );
}
