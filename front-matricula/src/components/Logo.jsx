// Logo UniConecta recriado em SVG a partir do protótipo.
// claro = versão para fundo azul (menu lateral e login no desktop).
export default function Logo({ tamanho = 'grande', claro = false }) {
  const classes = ['logo', tamanho === 'pequeno' && 'logo--pequeno', claro && 'logo--claro'].filter(Boolean).join(' ');
  return (
    <div className={classes} aria-label="UniConecta - Gestão Universitária Inteligente">
      <svg className="logo__icone" viewBox="0 0 64 64" aria-hidden>
        {/* capelo */}
        <path d="M32 6 61 18 32 30 3 18z" fill="currentColor" />
        <path d="M56 20v14" stroke="currentColor" strokeWidth="2.5" />
        <circle cx="56" cy="36" r="3" fill="#f7941d" />
        {/* "U" laranja que forma a pessoa / conexão */}
        <path d="M16 26v12a16 16 0 0 0 32 0V26" fill="none" stroke="#f7941d" strokeWidth="7" strokeLinecap="round" />
        <path d="M25 30v8a7 7 0 0 0 14 0v-8" fill="none" stroke="currentColor" strokeWidth="5" strokeLinecap="round" />
        <circle cx="32" cy="58" r="3" fill="currentColor" />
      </svg>
      <div className="logo__texto">
        <span className="logo__nome">
          <span className="logo__uni">Uni</span>
          <span className="logo__conecta">Conecta</span>
        </span>
        <span className="logo__slogan">Gestão Universitária Inteligente</span>
      </div>
    </div>
  );
}
