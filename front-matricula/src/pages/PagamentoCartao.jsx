import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CabecalhoPasso, Carregando, MensagemErro, Tela } from '../components/Layout';
import { IconeSeta } from '../components/Icones';
import { PagamentoApi } from '../services/api';
import { useMatricula } from '../useMatricula';
import { formatarMoeda, mascaraCartao, mascaraCpf, mascaraCvc, mascaraValidade } from '../services/formatar';

export default function PagamentoCartao() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { matricula, erro: erroCarga } = useMatricula(id);
  const [parcelas, setParcelas] = useState('');
  const [form, setForm] = useState({ cpfTitular: '', nomeTitular: '', numeroCartao: '', validade: '', cvc: '' });
  const [erro, setErro] = useState('');
  const [errosCampos, setErrosCampos] = useState({});
  const [enviando, setEnviando] = useState(false);

  const max = matricula?.curso.maxParcelas || 1;
  const valor = matricula?.valorTotal || 0;

  // Começa com o parcelamento máximo (ex.: 6x de R$ 1.500,00), como no protótipo.
  useEffect(() => {
    if (matricula) setParcelas(String(matricula.curso.maxParcelas));
  }, [matricula]);

  // Se o pagamento já foi aprovado (ex.: botão voltar do navegador), segue para a confirmação.
  useEffect(() => {
    if (matricula?.pagamento?.status === 'APROVADO') navigate(`/matricula/${id}/pagamento-confirmado`, { replace: true });
  }, [matricula, id, navigate]);

  const alterar = (campo, mascara) => (e) => {
    setForm((f) => ({ ...f, [campo]: mascara ? mascara(e.target.value) : e.target.value }));
    setErrosCampos((er) => ({ ...er, [campo]: undefined }));
  };

  function validar() {
    const er = {};
    if (form.cpfTitular.replace(/\D/g, '').length !== 11) er.cpfTitular = 'CPF incompleto.';
    if (form.nomeTitular.trim().length < 3) er.nomeTitular = 'Informe o nome impresso no cartão.';
    if (form.numeroCartao.replace(/\D/g, '').length < 13) er.numeroCartao = 'Número do cartão incompleto.';
    if (!/^\d{2}\/\d{2}$/.test(form.validade)) er.validade = 'Use MM/AA.';
    if (form.cvc.length < 3) er.cvc = 'CVC inválido.';
    setErrosCampos(er);
    return Object.keys(er).length === 0;
  }

  async function enviar(e) {
    e.preventDefault();
    setErro('');
    if (!validar()) return;
    setEnviando(true);
    try {
      await PagamentoApi.pagarComCartao(matricula.pagamento.id, { ...form, parcelas: Number(parcelas) });
      navigate(`/matricula/${id}/pagamento-confirmado`);
    } catch (err) {
      setErro(err.message);
      setErrosCampos(err.campos || {});
    } finally {
      setEnviando(false);
    }
  }

  const campo = (nome, rotulo, props = {}) => (
    <label className={`campo ${errosCampos[nome] ? 'campo--erro' : ''}`}>
      <span className="campo__rotulo">{rotulo}</span>
      <input value={form[nome]} {...props} />
      {errosCampos[nome] && <small className="campo__erro">{errosCampos[nome]}</small>}
    </label>
  );

  return (
    <Tela
      cabecalho={<CabecalhoPasso passo={2} subtitulo="Dados do cartão de crédito" voltarPara="/matricula" />}
      rodape={
        <>
          <button type="button" className="botao botao--secundario" onClick={() => navigate('/matricula')}>
            Voltar
          </button>
          <button type="submit" form="form-cartao" className="botao botao--primario" disabled={enviando || !matricula}>
            {enviando ? 'Enviando...' : 'Enviar'}
          </button>
        </>
      }
    >
      <MensagemErro>{erroCarga}</MensagemErro>
      {!matricula && !erroCarga && <Carregando />}

      {matricula && (
        <form id="form-cartao" onSubmit={enviar} noValidate>
          <label className="campo campo--metade">
            <span className="campo__rotulo">Parcelas</span>
            <div className="select">
              <select value={parcelas} onChange={(e) => setParcelas(e.target.value)}>
                {Array.from({ length: max }, (_, i) => i + 1).map((n) => (
                  <option key={n} value={n}>
                    {n}x de {formatarMoeda(Math.round((valor / n) * 100) / 100)}
                  </option>
                ))}
              </select>
              <IconeSeta />
            </div>
          </label>

          {campo('cpfTitular', 'CPF do titular', {
            inputMode: 'numeric',
            placeholder: '000.000.000-00',
            onChange: alterar('cpfTitular', mascaraCpf),
          })}
          {campo('nomeTitular', 'Nome impresso no cartão', {
            autoComplete: 'cc-name',
            onChange: alterar('nomeTitular', (v) => v.toUpperCase()),
          })}
          {campo('numeroCartao', 'Número do cartão', {
            inputMode: 'numeric',
            autoComplete: 'cc-number',
            placeholder: '0000 0000 0000 0000',
            onChange: alterar('numeroCartao', mascaraCartao),
          })}

          <div className="linha-dupla">
            {campo('validade', 'Vencimento', {
              inputMode: 'numeric',
              autoComplete: 'cc-exp',
              placeholder: 'MM/AA',
              onChange: alterar('validade', mascaraValidade),
            })}
            {campo('cvc', 'Código CVC', {
              inputMode: 'numeric',
              autoComplete: 'cc-csc',
              placeholder: '123',
              onChange: alterar('cvc', mascaraCvc),
            })}
          </div>

          <MensagemErro>{erro}</MensagemErro>

          <p className="dica">
            Modo demonstração: use o cartão <strong>4111 1111 1111 1111</strong>, qualquer vencimento futuro e CVC
            123. O cartão 4000 0000 0000 0002 simula uma recusa.
          </p>
        </form>
      )}
    </Tela>
  );
}
