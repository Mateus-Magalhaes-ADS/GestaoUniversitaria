// Comunicação com o back-end Java (Spring Boot).
const TOKEN_KEY = 'uniconecta.token';
const ALUNO_KEY = 'uniconecta.aluno';

export class ApiError extends Error {
  constructor(mensagem, status, campos = {}) {
    super(mensagem);
    this.status = status;
    this.campos = campos;
  }
}

function ler(chave) {
  try {
    return localStorage.getItem(chave);
  } catch {
    return null;
  }
}

export const sessao = {
  token: () => ler(TOKEN_KEY),
  aluno: () => {
    try {
      return JSON.parse(ler(ALUNO_KEY));
    } catch {
      return null;
    }
  },
  salvar(token, aluno) {
    try {
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(ALUNO_KEY, JSON.stringify(aluno));
    } catch {
      /* navegação privada: segue sem persistir */
    }
  },
  limpar() {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(ALUNO_KEY);
    } catch {
      /* ignora */
    }
  },
};

const SEM_CONEXAO = 'Não foi possível conectar ao servidor. Verifique se a API está rodando.';

export async function api(caminho, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  const token = sessao.token();
  if (token) headers.Authorization = `Bearer ${token}`;

  let resposta;
  try {
    resposta = await fetch(`/api${caminho}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(SEM_CONEXAO, 0);
  }

  const dados = await resposta.json().catch(() => null);

  if (resposta.status === 401 && caminho !== '/auth/login') {
    sessao.limpar();
    window.dispatchEvent(new Event('uniconecta:sessao-expirada'));
  }
  if (!resposta.ok) {
    const mensagem = dados?.mensagem || (resposta.status >= 500 ? SEM_CONEXAO : 'Não foi possível concluir a operação.');
    throw new ApiError(mensagem, resposta.status, dados?.campos || {});
  }
  return dados;
}

export const AuthApi = {
  login: (email, senha) => api('/auth/login', { method: 'POST', body: { email, senha } }),
  me: () => api('/auth/me'),
};

export const CursoApi = {
  listar: () => api('/cursos'),
};

export const MatriculaApi = {
  criar: (dados) => api('/matriculas', { method: 'POST', body: dados }),
  minhas: () => api('/matriculas/minhas'),
  buscar: (id) => api(`/matriculas/${id}`),
};

export const PagamentoApi = {
  buscar: (id) => api(`/pagamentos/${id}`),
  pagarComCartao: (id, dados) => api(`/pagamentos/${id}/cartao`, { method: 'POST', body: dados }),
  simularConfirmacao: (id) => api(`/pagamentos/${id}/simular-confirmacao`, { method: 'POST' }),
};
