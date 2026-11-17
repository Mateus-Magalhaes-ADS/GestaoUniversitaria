import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CabecalhoSecao, Carregando, MensagemErro, Tela } from '../components/Layout';
import { CursoApi } from '../services/api';
import { formatarDuracao, formatarMoeda } from '../services/formatar';

const GRAUS = {
  BACHARELADO: 'Bacharelado',
  LICENCIATURA: 'Licenciatura',
  TECNOLOGO: 'Tecnólogo',
  POS_GRADUACAO: 'Pós-graduação',
};

export default function Cursos() {
  const [cursos, setCursos] = useState(null);
  const [erro, setErro] = useState('');

  useEffect(() => {
    CursoApi.listar()
      .then(setCursos)
      .catch((e) => setErro(e.message));
  }, []);

  return (
    <Tela
      largura="larga"
      cabecalho={<CabecalhoSecao titulo="Cursos" subtitulo="Conheça os cursos disponíveis" />}
    >
      <MensagemErro>{erro}</MensagemErro>
      {!cursos && !erro && <Carregando />}

      <div className="grade">
        {cursos?.map((c) => (
          <article key={c.id} className="cartao">
            <span className="selo selo--neutro">{GRAUS[c.grau] || c.grau}</span>
            <h3>{c.nome}</h3>
            <p className="cartao__descricao">{c.descricao}</p>
            <p className="cartao__info">
              {formatarDuracao(c.duracaoMeses)} · {c.turnos.map((t) => t.descricao).join(', ')}
            </p>
            <p className="cartao__info">
              <strong>{formatarMoeda(c.valorTotal)}</strong> ou até {c.maxParcelas}x de{' '}
              {formatarMoeda(c.valorTotal / c.maxParcelas)}
            </p>
            <div className="cartao__acoes">
              <Link to={`/matricula?curso=${c.id}`} className="botao botao--primario botao--pequeno">
                Matricular-se
              </Link>
            </div>
          </article>
        ))}
      </div>
    </Tela>
  );
}
