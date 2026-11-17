import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CabecalhoPasso, Carregando, MensagemErro, Tela } from '../components/Layout';
import { IconeSeta } from '../components/Icones';
import { CursoApi, MatriculaApi } from '../services/api';
import { formatarDuracao, formatarMoeda } from '../services/formatar';

const FORMAS = [
  { valor: 'BOLETO', rotulo: 'Boleto', rota: 'boleto' },
  { valor: 'CARTAO', rotulo: 'Cartão de crédito', rota: 'cartao' },
  { valor: 'PIX', rotulo: 'Pix', rota: 'pix' },
];

export default function MatriculaPasso1() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [cursos, setCursos] = useState(null);
  const [cursoId, setCursoId] = useState('');
  const [turno, setTurno] = useState('');
  const [forma, setForma] = useState('');
  const [erro, setErro] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    CursoApi.listar()
      .then((lista) => {
        setCursos(lista);
        const pedido = lista.find((c) => String(c.id) === params.get('curso'));
        const inicial = pedido || lista[0];
        if (inicial) {
          setCursoId(String(inicial.id));
          setTurno(inicial.turnos.at(-1)?.valor || '');
        }
      })
      .catch((e) => setErro(e.message));
  }, [params]);

  const curso = useMemo(() => cursos?.find((c) => String(c.id) === cursoId), [cursos, cursoId]);

  function trocarCurso(id) {
    setCursoId(id);
    const novo = cursos.find((c) => String(c.id) === id);
    if (novo && !novo.turnos.some((t) => t.valor === turno)) {
      setTurno(novo.turnos.at(-1)?.valor || '');
    }
  }

  async function continuar() {
    setErro('');
    if (!curso) return setErro('Escolha o curso.');
    if (!turno) return setErro('Escolha o período.');
    if (!forma) return setErro('Escolha a forma de pagamento.');

    setEnviando(true);
    try {
      const matricula = await MatriculaApi.criar({
        cursoId: curso.id,
        turno,
        formaPagamento: forma,
      });
      const rota = FORMAS.find((f) => f.valor === forma).rota;
      navigate(`/matricula/${matricula.id}/${rota}`);
    } catch (e) {
      setErro(e.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <Tela
      cabecalho={<CabecalhoPasso passo={1} subtitulo="Curso selecionado e forma de pagamento" voltarPara="/" />}
      rodape={
        <>
          <button type="button" className="botao botao--secundario" onClick={() => navigate('/')}>
            Voltar
          </button>
          <button type="button" className="botao botao--primario" onClick={continuar} disabled={enviando || !curso}>
            {enviando ? 'Aguarde...' : 'Continuar'}
          </button>
        </>
      }
    >
      {!cursos && !erro && <Carregando texto="Carregando cursos..." />}

      {cursos && (
        <>
          <label className="campo">
            <span className="campo__rotulo">Escolha o curso</span>
            <div className="select">
              <select value={cursoId} onChange={(e) => trocarCurso(e.target.value)}>
                {cursos.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nome}
                  </option>
                ))}
              </select>
              <IconeSeta />
            </div>
          </label>

          <div className="linha-dupla">
            <label className="campo">
              <span className="campo__rotulo">Duração</span>
              <input readOnly value={curso ? formatarDuracao(curso.duracaoMeses) : ''} />
            </label>
            <label className="campo">
              <span className="campo__rotulo">Período</span>
              <div className="select">
                <select value={turno} onChange={(e) => setTurno(e.target.value)}>
                  {curso?.turnos.map((t) => (
                    <option key={t.valor} value={t.valor}>
                      {t.descricao}
                    </option>
                  ))}
                </select>
                <IconeSeta />
              </div>
            </label>
          </div>

          <label className="campo campo--metade">
            <span className="campo__rotulo">Valor total do investimento</span>
            <input readOnly value={curso ? formatarMoeda(curso.valorTotal) : ''} />
          </label>

          <fieldset className="opcoes">
            <legend className="campo__rotulo">Forma de pagamento</legend>
            <div className="opcoes__caixa">
              {FORMAS.map((f) => (
                <label key={f.valor} className="opcao">
                  <input
                    type="radio"
                    name="forma"
                    value={f.valor}
                    checked={forma === f.valor}
                    onChange={() => setForma(f.valor)}
                  />
                  <span>{f.rotulo}</span>
                </label>
              ))}
            </div>
          </fieldset>
        </>
      )}

      <MensagemErro>{erro}</MensagemErro>
    </Tela>
  );
}
