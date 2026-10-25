# E-mail dos usuários

POST /users e PUT /users/{id} exigem name e email. O e-mail não é uma credencial:
as contas de autenticação continuam separadas em auth_accounts.

## Contrato

- Obrigatório, não vazio, validado por Bean Validation (@Email) e limitado a 254 unidades UTF-16 após normalização.
- Espaços nas pontas são removidos (String.strip) e o endereço completo é convertido para minúsculas com Locale.ROOT.
- A API considera o endereço completo sem diferenciar maiúsculas/minúsculas, inclusive a parte antes de @. Essa é uma decisão de negócio.
- A validação de formato não verifica se a caixa postal existe ou pertence à pessoa.
- POST, PUT, GET por ID e a listagem paginada retornam email.
- PUT substitui nome e e-mail: omitir email retorna 400, inclusive em cadastro legado.
- A ordenação continua aceitando apenas id e name.

Exemplo de criação ou atualização:

```json
{
  "name": "Jefferson Andrade",
  "email": "jefferson@example.com"
}
```

| Situação | Resposta |
|---|---|
| E-mail ausente, nulo, vazio ou só espaços | 400 — O e-mail é obrigatório |
| Formato inválido | 400 — O e-mail deve ser válido |
| Mais de 254 unidades UTF-16 normalizadas | 400 — O e-mail deve ter no máximo 254 caracteres |
| E-mail já utilizado por outro usuário | 409 — O e-mail já está cadastrado |
| Atualização mantendo o próprio e-mail | 200 |

Se houver várias violações, a seleção segue o tratamento global existente: campo, constraint e mensagem em ordem lexical.
A resposta 409 usa o mesmo ErrorResponseDTO dos demais erros:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "O e-mail já está cadastrado",
  "timestamp": "2026-10-25T12:00:00"
}
```

## Unicidade e concorrência

A V4 cria email VARCHAR(254) nullable e o índice único uk_users_email sobre lower(email) no PostgreSQL.
O índice garante unicidade mesmo se duas requisições chegarem juntas ou se uma escrita SQL usar outra caixa.
O serviço usa saveAndFlush para concluir a escrita antes de montar a resposta.
O handler converte somente a violação desse índice para 409; outras falhas de integridade não são classificadas como e-mail duplicado.
Um conflito não grava alterações parciais no nome, e-mail ou datas.

## Atualização do banco existente

A migração V4 preserva IDs, nomes, contas de autenticação, datas e sequências.
Os registros anteriores continuam com email nulo até serem completados com um endereço real via PUT.
Não há e-mail fictício, exclusão de usuários ou alteração das migrações V1–V3.
A coluna fica nullable para manter esses legados; as escritas pela API exigem e-mail.
O índice permite vários valores nulos, conforme o comportamento do PostgreSQL.
Escritas SQL diretas não passam pela validação da API e devem ser administradas separadamente.

Com o banco já gerenciado pelo Flyway, faça backup e inicie a aplicação atualizada: V4 será aplicada automaticamente.
Para um banco ainda sem Flyway, siga primeiro a adoção explícita do README.
Confira:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT id, name, email, created_at, updated_at FROM users ORDER BY id;
SELECT indexdef FROM pg_indexes WHERE tablename = 'users' AND indexname = 'uk_users_email';
```

## Verificação

UserEmailTest verifica validação, normalização, limites e tradução específica do conflito.
UserOpenApiTest verifica o campo obrigatório, formato, limite e documentação do 409.
UserPostgresIT verifica persistência, consultas, atualização do próprio e-mail, rejeição sem mudanças parciais,
cadastros legados, migração V3→V4 e duas criações concorrentes.
O H2 dos testes convencionais não substitui o teste do índice funcional PostgreSQL.

Execute a suíte completa com Docker disponível:

```powershell
.\mvnw.cmd -Pintegration verify
```
