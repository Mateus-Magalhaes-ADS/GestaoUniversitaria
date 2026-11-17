import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';
import Login from './pages/Login';
import Inicio from './pages/Inicio';
import Cursos from './pages/Cursos';
import Perfil from './pages/Perfil';
import MatriculaPasso1 from './pages/MatriculaPasso1';
import PagamentoBoleto from './pages/PagamentoBoleto';
import StatusBoleto from './pages/StatusBoleto';
import PagamentoCartao from './pages/PagamentoCartao';
import PagamentoPix from './pages/PagamentoPix';
import PagamentoConfirmado from './pages/PagamentoConfirmado';
import MatriculaConfirmada from './pages/MatriculaConfirmada';

function Protegida({ children }) {
  const { aluno } = useAuth();
  const location = useLocation();
  if (!aluno) return <Navigate to="/login" replace state={{ de: location.pathname + location.search }} />;
  return children;
}

const rotasProtegidas = [
  ['/', Inicio],
  ['/cursos', Cursos],
  ['/perfil', Perfil],
  ['/matricula', MatriculaPasso1],
  ['/matricula/:id/boleto', PagamentoBoleto],
  ['/matricula/:id/boleto/status', StatusBoleto],
  ['/matricula/:id/cartao', PagamentoCartao],
  ['/matricula/:id/pix', PagamentoPix],
  ['/matricula/:id/pagamento-confirmado', PagamentoConfirmado],
  ['/matricula/:id/confirmada', MatriculaConfirmada],
];

export default function App() {
  return (
    <div className="dispositivo">
      <Routes>
        <Route path="/login" element={<Login />} />
        {rotasProtegidas.map(([caminho, Pagina]) => (
          <Route
            key={caminho}
            path={caminho}
            element={
              <Protegida>
                <Pagina />
              </Protegida>
            }
          />
        ))}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </div>
  );
}
