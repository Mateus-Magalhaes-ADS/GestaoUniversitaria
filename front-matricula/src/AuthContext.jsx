import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { AuthApi, sessao } from './services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [aluno, setAluno] = useState(() => (sessao.token() ? sessao.aluno() : null));

  const login = useCallback(async (email, senha) => {
    const { token, aluno: dados } = await AuthApi.login(email, senha);
    sessao.salvar(token, dados);
    setAluno(dados);
    return dados;
  }, []);

  const logout = useCallback(() => {
    sessao.limpar();
    setAluno(null);
  }, []);

  // Se a API responder 401 (token expirado), volta para o login.
  useEffect(() => {
    const aoExpirar = () => setAluno(null);
    window.addEventListener('uniconecta:sessao-expirada', aoExpirar);
    return () => window.removeEventListener('uniconecta:sessao-expirada', aoExpirar);
  }, []);

  return <AuthContext.Provider value={{ aluno, login, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
