import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../AuthContext';
import Logo from './Logo';
import { IconeCursos, IconeInicio, IconeMatricula, IconePerfil, IconeSair, IconeVoltar } from './Icones';

/** Cabeçalho com "Passo N" e subtítulo, como no Figma. */
export function CabecalhoPasso({ passo, titulo, subtitulo, voltarPara }) {
  const navigate = useNavigate();
  return (
    <header className="cabecalho">
      <button
        type="button"
        className="cabecalho__voltar"
        aria-label="Voltar"
        onClick={() => (voltarPara ? navigate(voltarPara) : navigate(-1))}
      >
        <IconeVoltar />
      </button>
      <div>
        <h1 className="cabecalho__titulo">{passo ? `Passo ${passo}` : titulo}</h1>
        {subtitulo && <p className="cabecalho__subtitulo">{subtitulo}</p>}
      </div>
    </header>
  );
}

/** Cabeçalho simples (sem botão voltar) das abas Início, Cursos e Perfil. */
export function CabecalhoSecao({ titulo, subtitulo }) {
  return (
    <header className="cabecalho cabecalho--secao">
      <div>
        <h1 className="cabecalho__titulo">{titulo}</h1>
        {subtitulo && <p className="cabecalho__subtitulo">{subtitulo}</p>}
      </div>
    </header>
  );
}

const ITENS_MENU = [
  { para: '/', rotulo: 'Início', Icone: IconeInicio, fim: true },
  { para: '/matricula', rotulo: 'Matrícula', Icone: IconeMatricula },
  { para: '/cursos', rotulo: 'Cursos', Icone: IconeCursos },
  { para: '/perfil', rotulo: 'Perfil', Icone: IconePerfil },
];

/**
 * Menu principal.
 *  - Celular: barra inferior (como no Figma).
 *  - Tablet: barra lateral compacta.
 *  - Desktop: barra lateral com logo, nome do aluno e botão Sair.
 */
export function Menu() {
  const { aluno, logout } = useAuth();
  const navigate = useNavigate();

  function sair() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <nav className="menu" aria-label="Menu principal">
      <div className="menu__logo">
        <Logo tamanho="pequeno" claro />
      </div>

      <div className="menu__itens">
        {ITENS_MENU.map(({ para, rotulo, Icone, fim }) => (
          <NavLink
            key={para}
            to={para}
            end={fim}
            className={({ isActive }) => `menu__item ${isActive ? 'ativo' : ''}`}
          >
            <Icone />
            <span>{rotulo}</span>
          </NavLink>
        ))}
      </div>

      <div className="menu__usuario">
        <span className="menu__nome">{aluno?.nome}</span>
        <span className="menu__email">{aluno?.email}</span>
        <button type="button" className="menu__sair" onClick={sair}>
          <IconeSair /> Sair
        </button>
      </div>
    </nav>
  );
}

/**
 * Estrutura padrão das telas internas: menu + cabeçalho + painel (conteúdo e botões).
 * largura="larga" é usada nas telas de listagem (Início e Cursos).
 */
export function Tela({ cabecalho, children, rodape, className = '', largura = 'padrao', rodapeSoCelular = false }) {
  return (
    <div className="tela">
      <Menu />
      <div className="tela__area">
        {cabecalho}
        <div className={`tela__painel tela__painel--${largura}`}>
          <main className={`tela__conteudo ${className}`}>{children}</main>
          {rodape && (
            <div className={`tela__rodape ${rodapeSoCelular ? 'tela__rodape--so-celular' : ''}`}>{rodape}</div>
          )}
        </div>
      </div>
    </div>
  );
}

export function Carregando({ texto = 'Carregando...' }) {
  return (
    <div className="carregando" role="status">
      <span className="spinner" aria-hidden />
      {texto}
    </div>
  );
}

export function MensagemErro({ children }) {
  if (!children) return null;
  return (
    <p className="erro" role="alert">
      {children}
    </p>
  );
}
