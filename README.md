# Gestão Universitária

## Execução com banco JSON criptografado

O banco é salvo em `data/universidade.json.enc` e cifrado integralmente com AES-256-GCM. Configure `APP_DATA_ENCRYPTION_KEY` com uma chave Base64 que represente exatamente 32 bytes antes de iniciar a aplicação. A chave deve ser mantida em um gerenciador de segredos ou variável de ambiente persistente; perder ou trocar a chave torna os dados existentes irrecuperáveis.

No PowerShell, gere uma chave uma única vez:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Configure o valor gerado no ambiente e inicie:

```powershell
$env:APP_DATA_ENCRYPTION_KEY = "<chave-base64-gerada>"
mvn spring-boot:run
```

O cadastro público cria apenas alunos. Para criar a primeira conta de equipe, configure `APP_BOOTSTRAP_STAFF_EMAIL`, `APP_BOOTSTRAP_STAFF_PASSWORD`, `APP_BOOTSTRAP_STAFF_NAME` e `APP_BOOTSTRAP_STAFF_ROLE` (`ATENDENTE`, `COORDENADOR` ou `PROFESSOR`) no ambiente antes da primeira inicialização. Quando a conta for criada, essas variáveis de bootstrap podem ser removidas. Matrículas exigem login de aluno; alunos veem somente suas solicitações, enquanto atendentes e coordenadores veem todas.

Senhas permanecem protegidas com BCrypt. Dados cadastrais, dados de matrícula e conteúdo dos documentos enviados ficam dentro do arquivo cifrado. Os registros que eventualmente existam no antigo H2 não são migrados automaticamente para o JSON.
