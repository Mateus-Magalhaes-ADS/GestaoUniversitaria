# UniConecta — Sistema de Gestão Universitária

Projeto Integrador acadêmico de TADS/TSI — SENAC EAD. O UniConecta é um MVP web para solicitações de matrícula e atividades acadêmicas, com interface estática, API REST e autenticação OAuth 2.0.

## Índice

- [Funcionalidades do MVP](#funcionalidades-do-mvp)
- [Tecnologias](#tecnologias)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Requisitos](#requisitos)
- [Executar com Docker Compose](#executar-com-docker-compose)
- [Executar localmente](#executar-localmente)
- [Autenticação OAuth 2.0](#autenticação-oauth-20)
- [Testar com Postman](#testar-com-postman)
- [API REST](#api-rest)
- [Configuração](#configuração)
- [Testes automatizados](#testes-automatizados)
- [Segurança e limitações do MVP](#segurança-e-limitações-do-mvp)

## Funcionalidades do MVP

- Cadastro de alunos e autenticação OAuth 2.0 Authorization Code com PKCE.
- Perfis de aluno, professor, coordenador e atendente.
- Solicitações de matrícula com documentos de identidade e histórico escolar.
- Consulta de solicitações conforme o perfil: alunos veem somente as próprias; atendentes e coordenadores podem consultar todas.
- Criação, abertura, resposta, encerramento, correção e publicação de provas.
- Interface web separada em `front/` e backend/API em `backend/`.

## Tecnologias

- Java 25, Spring Boot 3.5.16 e Maven.
- Spring MVC, Spring Security, Spring Data JPA e Spring Authorization Server.
- PostgreSQL 17.
- Frontend HTML, CSS e JavaScript sem framework.
- Postman para explorar e testar a API.

## Estrutura do repositório

```text
.
├── backend/
│   ├── src/main/java/com/universidade/gestao/
│   │   ├── config/        # Segurança e autorização OAuth 2.0
│   │   ├── controller/    # Controladores web e REST (/api)
│   │   ├── model/         # Entidades persistidas no PostgreSQL
│   │   ├── repository/    # Repositórios Spring Data JPA
│   │   └── service/       # Serviços de autenticação e bootstrap
│   ├── src/main/resources/
│   ├── src/test/           # Testes de integração
│   └── pom.xml
├── front/
│   ├── index.html
│   ├── callback.html
│   ├── app.js
│   ├── callback.js
│   └── styles.css
├── compose.yaml
└── Dockerfile
```

## Requisitos

- JDK 25 para execução local; Maven 3.9 ou superior.
- Docker e Docker Compose (opcional, para iniciar backend e banco juntos).
- Postman (opcional, para testar endpoints manualmente).

## Executar com Docker Compose

Na raiz do repositório, defina uma senha para o banco e inicie os serviços:

```powershell
$env:DATABASE_PASSWORD = "defina-uma-senha-local"
docker compose up --build
```

O PostgreSQL fica disponível internamente para o backend; a API é publicada em `http://localhost:8080`. Para abrir a interface, sirva a pasta `front/` na porta `5500` (por exemplo, com a extensão Live Server do VS Code) e acesse `http://localhost:5500`.

O volume Docker `postgres-data` mantém os registros entre reinicializações. Para encerrar os serviços, use `Ctrl+C`; para executar em segundo plano, use `docker compose up --build -d`.

## Executar localmente

Inicie um servidor PostgreSQL e crie um banco e usuário para o projeto. Configure as variáveis de conexão no PowerShell:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/uniconecta"
$env:DATABASE_USERNAME = "uniconecta"
$env:DATABASE_PASSWORD = "defina-uma-senha-local"
$env:FRONTEND_ORIGIN = "http://localhost:5500"
```

Opcionalmente, crie a primeira conta de equipe antes de iniciar pela primeira vez:

```powershell
$env:APP_BOOTSTRAP_STAFF_EMAIL = "equipe@exemplo.com"
$env:APP_BOOTSTRAP_STAFF_PASSWORD = "troque-esta-senha"
$env:APP_BOOTSTRAP_STAFF_NAME = "Equipe UniConecta"
$env:APP_BOOTSTRAP_STAFF_ROLE = "COORDENADOR"
```

Inicie o backend na raiz do repositório:

```powershell
mvn -f backend\pom.xml spring-boot:run
```

Use um servidor estático para servir `front/` na porta 5500. A aplicação está configurada para permitir por padrão `http://localhost:5500`; ao usar outra origem, atualize `FRONTEND_ORIGIN`.

## Autenticação OAuth 2.0

O servidor OAuth 2.0 está em `http://localhost:8080`. O cliente público de desenvolvimento está configurado como:

| Propriedade | Valor |
|---|---|
| Client ID | `uniconecta-web` |
| Client secret | Nenhum — cliente público com PKCE |
| Authorization URL | `http://localhost:8080/oauth2/authorize` |
| Access token URL | `http://localhost:8080/oauth2/token` |
| Redirect URI da interface | `http://localhost:5500/callback.html` |
| Redirect URI do Postman | `https://oauth.pstmn.io/v1/callback` |
| Grant type | Authorization Code (com PKCE) |
| Scopes | `openid profile` |

O fluxo `grant_type=password` do material antigo não é utilizado: trata-se de uma abordagem obsoleta. A interface e o Postman devem autenticar com Authorization Code + PKCE.

## Testar com Postman

1. Inicie o backend e crie uma conta de aluno pela interface ou pelo endpoint público de cadastro.
2. Crie uma requisição ou collection e abra a aba **Authorization**.
3. Selecione **OAuth 2.0** e configure:

| Campo | Valor |
|---|---|
| Grant Type | Authorization Code (With PKCE) |
| Callback URL | `https://oauth.pstmn.io/v1/callback` |
| Auth URL | `http://localhost:8080/oauth2/authorize` |
| Access Token URL | `http://localhost:8080/oauth2/token` |
| Client ID | `uniconecta-web` |
| Client Secret | Deixe vazio |
| Code Challenge Method | `S256` |
| Scope | `openid profile` |
| Client Authentication | Send client credentials in body / sem autenticação de cliente |

4. Clique em **Get New Access Token**, entre com o e-mail e a senha da conta na página do UniConecta e conclua a autorização.
5. Use **Use Token**. As requisições protegidas devem enviar `Authorization: Bearer <access_token>`.
6. Para confirmar o acesso autenticado, envie `GET http://localhost:8080/api/me`.

## API REST

Base URL: `http://localhost:8080`. Exceto pelo cadastro, os endpoints abaixo exigem Bearer token e aplicam autorização por perfil.

### Autenticação e perfil

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Cadastra aluno. Aceita `name`, `email`, `password`. |
| `GET` | `/api/me` | Autenticado | Retorna e-mail e perfis do token. |

Exemplo de cadastro:

```json
{
  "name": "Ana Souza",
  "email": "ana@example.com",
  "password": "uma-senha-segura"
}
```

### Matrículas

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/matriculas` | Aluno, atendente, coordenador | Aluno recebe apenas as próprias; equipe recebe todas. |
| `GET` | `/api/v1/matriculas/{id}` | Aluno, atendente, coordenador | Consulta uma solicitação, respeitando a propriedade do aluno. |
| `GET` | `/api/v1/matriculas/{id}/documentos/{tipo}` | Aluno titular, atendente, coordenador | Baixa documento `identidade` ou `historico`. |
| `POST` | `/api/v1/matriculas` | Aluno | Envia matrícula como `multipart/form-data`. |

Campos do `POST /api/v1/matriculas`: `cpf`, `endereco`, `cep`, `cursoId`, `documentoIdentidade` e `historicoEscolar`. Os dois documentos aceitam PDF, PNG, JPG ou JPEG. Tamanho máximo: 10 MB por arquivo.

### Provas

| Método | Endpoint | Acesso | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/provas` | Aluno, professor, coordenador | Lista provas abertas para alunos, próprias para professores ou todas para coordenadores. |
| `GET` | `/api/v1/provas/{id}` | Aluno, professor, coordenador | Consulta uma prova; respostas corretas não são exibidas antes da publicação para alunos. |
| `POST` | `/api/v1/provas` | Professor ou coordenador | Cria prova com seis questões objetivas seguidas por uma dissertativa. |
| `POST` | `/api/v1/provas/{id}/abrir` | Professor responsável | Abre prova em rascunho para respostas. |
| `POST` | `/api/v1/provas/{id}/encerrar` | Professor responsável | Encerra o recebimento de respostas. |
| `POST` | `/api/v1/provas/{id}/responder` | Aluno | Envia respostas uma única vez para uma prova aberta. |
| `GET` | `/api/v1/provas/{id}/respostas` | Professor responsável | Lista respostas para correção. |
| `POST` | `/api/v1/provas/{id}/respostas/{answerId}/correcao` | Professor responsável | Corrige a questão dissertativa. |
| `POST` | `/api/v1/provas/{id}/publicar` | Coordenador | Publica resultados após a correção. |
| `GET` | `/api/v1/provas/resultados` | Aluno | Lista resultados publicados do aluno autenticado. |

Para criar uma prova, o JSON contém `title`, `subject` e uma lista `questions`. Cada questão usa `prompt`, `type` (`OBJETIVA` ou `DISSERTATIVA`), `alternatives`, `correctAlternative` (questões objetivas) e `value`. Para atribuição por coordenador, informe também `professorEmail`.

## Configuração

| Variável | Uso |
|---|---|
| `DATABASE_URL` | JDBC URL do PostgreSQL. Padrão: `jdbc:postgresql://localhost:5432/uniconecta`. |
| `DATABASE_USERNAME` | Usuário do PostgreSQL. |
| `DATABASE_PASSWORD` | Senha do PostgreSQL. |
| `FRONTEND_ORIGIN` | Origem do frontend permitida pelo CORS. Padrão: `http://localhost:5500`. |
| `PORT` | Porta HTTP do backend. Padrão: `8080`. |
| `APP_BOOTSTRAP_STAFF_EMAIL` | E-mail para criação inicial de conta de equipe. |
| `APP_BOOTSTRAP_STAFF_PASSWORD` | Senha para a conta inicial. |
| `APP_BOOTSTRAP_STAFF_NAME` | Nome da conta inicial. |
| `APP_BOOTSTRAP_STAFF_ROLE` | `ATENDENTE`, `COORDENADOR` ou `PROFESSOR`. |
| `APP_PROFESSOR_REGISTRATION_CODE` | Convite para cadastro de professor pelo formulário MVC legado. |
| `APP_COORDINATOR_REGISTRATION_CODE` | Convite para cadastro de coordenador pelo formulário MVC legado. |

Mantenha senhas e códigos fora do repositório. O Hibernate cria ou atualiza o esquema com `ddl-auto=update`; use migrações controladas antes de operar com dados de produção.

## Testes automatizados

Os testes utilizam H2 em modo PostgreSQL e não exigem uma instância PostgreSQL:

```powershell
mvn -f backend\pom.xml test
```

## Segurança e limitações do MVP

- Senhas de usuários são protegidas com BCrypt.
- O token de acesso é JWT e carrega o perfil usado na autorização dos endpoints.
- A chave RSA de assinatura é gerada durante a inicialização do servidor; reinicializações invalidam tokens existentes. Para produção, configure uma chave persistente gerenciada com segurança e uma URL de issuer estável.
- O cliente OAuth2 público e as URLs `localhost` são para desenvolvimento. Registre redirect URIs explícitas e HTTPS antes de disponibilizar o sistema publicamente.
- O armazenamento JSON criptografado anterior foi substituído por PostgreSQL. Não há migração automática dos dados locais `data/universidade.json.enc` nem de registros H2 antigos.
- A aplicação possui interface/API MVC anterior junto da nova API durante a transição; o frontend separado usa os endpoints `/api`.

---

Projeto acadêmico desenvolvido para fins educacionais.
