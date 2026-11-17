import { useCallback, useEffect, useState } from 'react';
import { MatriculaApi } from './services/api';

/** Carrega a matrícula (com o pagamento) pelo id da URL. */
export function useMatricula(id) {
  const [matricula, setMatricula] = useState(null);
  const [erro, setErro] = useState('');

  const recarregar = useCallback(async () => {
    try {
      const dados = await MatriculaApi.buscar(id);
      setMatricula(dados);
      setErro('');
      return dados;
    } catch (e) {
      setErro(e.message);
      return null;
    }
  }, [id]);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  return { matricula, erro, recarregar };
}
