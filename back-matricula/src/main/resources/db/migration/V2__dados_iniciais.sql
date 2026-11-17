-- =====================================================================
-- V2: dados FICTÍCIOS para demonstração
-- Senha de todos os alunos: 123456  (armazenada com BCrypt)
-- =====================================================================

INSERT INTO aluno (nome, cpf, email, senha_hash, telefone) VALUES
  ('Lucas Andrade',   '12345678909', 'aluno@uniconecta.com', '$2a$10$hbrUt0SI.p16D5a3CkIXDe5Hi5WId3/aJqM8ErVrgNuJQzsW5GmVS', '(11) 98765-4321'),
  ('Maria Oliveira',  '11144477735', 'maria@uniconecta.com', '$2a$10$hbrUt0SI.p16D5a3CkIXDe5Hi5WId3/aJqM8ErVrgNuJQzsW5GmVS', '(11) 91234-5678');

INSERT INTO curso (nome, grau, descricao, duracao_meses, valor_total, max_parcelas) VALUES
  ('Bacharelado em Ciência da Computação', 'BACHARELADO',
   'Algoritmos, estruturas de dados, engenharia de software e inteligência artificial.', 6, 9000.00, 6),
  ('Bacharelado em Sistemas de Informação', 'BACHARELADO',
   'Tecnologia aplicada à gestão: sistemas, banco de dados e processos de negócio.', 6, 8400.00, 6),
  ('Engenharia de Software', 'BACHARELADO',
   'Desenvolvimento de sistemas com foco em qualidade, arquitetura e metodologias ágeis.', 8, 12000.00, 8),
  ('Tecnólogo em Análise e Desenvolvimento de Sistemas', 'TECNOLOGO',
   'Formação prática e rápida em programação web, mobile e banco de dados.', 4, 6000.00, 4),
  ('Licenciatura em Matemática', 'LICENCIATURA',
   'Formação de professores de matemática para o ensino fundamental e médio.', 6, 7200.00, 6),
  ('MBA em Gestão de Projetos', 'POS_GRADUACAO',
   'Gestão de projetos com PMBOK, Scrum e liderança de equipes.', 12, 14400.00, 12);

INSERT INTO curso_turno (curso_id, turno)
SELECT c.id, t.turno
FROM curso c
JOIN (VALUES
  ('Bacharelado em Ciência da Computação', 'MATUTINO'),
  ('Bacharelado em Ciência da Computação', 'NOTURNO'),
  ('Bacharelado em Sistemas de Informação', 'MATUTINO'),
  ('Bacharelado em Sistemas de Informação', 'NOTURNO'),
  ('Engenharia de Software', 'MATUTINO'),
  ('Engenharia de Software', 'VESPERTINO'),
  ('Engenharia de Software', 'NOTURNO'),
  ('Tecnólogo em Análise e Desenvolvimento de Sistemas', 'NOTURNO'),
  ('Licenciatura em Matemática', 'VESPERTINO'),
  ('Licenciatura em Matemática', 'NOTURNO'),
  ('MBA em Gestão de Projetos', 'NOTURNO')
) AS t(nome, turno) ON t.nome = c.nome;
