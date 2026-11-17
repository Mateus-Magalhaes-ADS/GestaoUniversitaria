import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import Logo from '../components/Logo';
import { MensagemErro } from '../components/Layout';

export default function Login() {
  const { aluno, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [aviso, setAviso] = useState('');
  const [enviando, setEnviando] = useState(false);

  if (aluno) return <Navigate to="/" replace />;

  async function entrar(e) {
    e.preventDefault();
    setErro('');
    if (!email.trim() || !senha) {
      setErro('Informe o e-mail e a senha.');
      return;
    }
    setEnviando(true);
    try {
      await login(email.trim(), senha);
      navigate(location.state?.de || '/', { replace: true });
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="login">
      {/* Painel da marca: aparece só em telas largas (tablet e desktop) */}
      <aside className="login__marca">
        <Logo claro />
        <h2>Sua matrícula em poucos passos.</h2>
        <p>Escolha o curso, defina o turno e pague como preferir. Tudo em um só lugar.</p>
        <ul>
          <li>Matrícula 100% online</li>
          <li>Boleto, cartão de crédito parcelado ou Pix</li>
          <li>Confirmação imediata no cartão e no Pix</li>
        </ul>
      </aside>

      <div className="login__painel">
        <div className="login__caixa">
          <div className="login__logo">
            <Logo />
          </div>
          <h1 className="login__titulo">Acesse sua conta</h1>

          <form className="login__form" onSubmit={entrar} noValidate>
            <label className="campo-linha">
              <span>E-mail</span>
              <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
            </label>
            <label className="campo-linha">
              <span>Senha</span>
              <input
                type="password"
                autoComplete="current-password"
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
              />
            </label>
            <button
              type="button"
              className="link-discreto"
              onClick={() => setAviso('Para redefinir a senha, procure a secretaria acadêmica.')}
            >
              Esqueci a senha
            </button>

            <MensagemErro>{erro}</MensagemErro>
            {aviso && <p className="aviso">{aviso}</p>}

            <button type="submit" className="botao botao--primario botao--login" disabled={enviando}>
              {enviando ? 'Entrando...' : 'Login'}
            </button>
          </form>

          <p className="login__demo">
            Acesso de demonstração: <strong>aluno@uniconecta.com</strong> / <strong>123456</strong>
          </p>
        </div>
      </div>
    </div>
  );
}
