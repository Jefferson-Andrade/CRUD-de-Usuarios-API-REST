# Datas de criação e atualização dos usuários

As respostas de POST, PUT, GET por ID e dos itens da listagem incluem `createdAt` e `updatedAt`.
Ambos são instantes em UTC no formato ISO 8601, com precisão de até seis casas decimais.

```json
{
  "id": 1,
  "name": "Jefferson Andrade",
  "email": "jefferson@example.com",
  "createdAt": "2026-10-24T18:00:00.123456Z",
  "updatedAt": "2026-10-24T18:30:00.654321Z"
}
```

Os valores do exemplo são ilustrativos. Na criação, as duas datas recebem o mesmo instante do servidor.
Uma alteração efetiva do nome atualiza somente `updatedAt`; repetir PUT com o mesmo nome não altera as datas.
Falhas de validação também preservam as datas. A exclusão mantém o comportamento atual (204, sem corpo).

O corpo de entrada contém `name` e `email` obrigatório desde a V4 (veja [e-mail dos usuários](user-email.md)). As datas não são aceitas como valores controláveis:
se enviadas como propriedades extras, são ignoradas pelo DTO de entrada. O Swagger as documenta como
campos de resposta `date-time` e somente leitura. A ordenação continua limitada a `id` e `name`.

## Implementação

- `User` usa callbacks JPA `@PrePersist` e `@PreUpdate` e `Instant`, truncado em microssegundos.
- `created_at` é marcado `updatable=false` no mapeamento JPA e não tem setter público.
- `UserService.updateUser` usa `saveAndFlush`, executando o callback antes de montar a resposta do PUT.
- `UserResponseDTO` recebe os valores persistidos. Não há campos de auditoria no `UserRequestDTO`.

São duas datas do estado atual, não um histórico de mudanças nem identificação de quem fez a alteração.
Os callbacks usam o relógio do servidor da aplicação; mantenha os relógios sincronizados em ambientes com várias instâncias.

## Flyway V3 e dados existentes

`V3__add_user_audit_timestamps.sql` adiciona `created_at` e `updated_at` como
`TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP` no PostgreSQL.
IDs, nomes, sequência de IDs e contas de autenticação são preservados.

**A data original dos cadastros antigos é desconhecida.** Para eles, as duas colunas recebem o instante
da transação de migração V3 como referência. Esse preenchimento não recupera o histórico anterior.
A migração é aplicada uma vez; reiniciar não recalcula as datas.

Os defaults permitem INSERT SQL sem informar datas. Não existe trigger de UPDATE: alterações feitas
diretamente por SQL devem atribuir `updated_at = CURRENT_TIMESTAMP` explicitamente. Escritas pela API
são auditadas pelos callbacks JPA.

Em um banco vazio, Flyway aplica V1, V2, V3 e V4. Em um banco com V1/V2 registradas, aplica V3 e V4.
Em banco legado sem histórico, siga primeiro o procedimento de backup e baseline V1 do README;
na inicialização seguinte, V2, V3 e V4 são aplicadas. Não altere scripts já executados.

Confira o histórico após atualizar o projeto e iniciar a aplicação:

```powershell
docker compose exec db psql -U postgres -d crud -c "SELECT version, description, success FROM public.flyway_schema_history ORDER BY installed_rank;"
docker compose exec db psql -U postgres -d crud -c "SELECT id, name, created_at, updated_at FROM public.users ORDER BY id;"
```

Os testes não migram o banco local. `UserAuditTest` verifica criação, leitura/listagem, alteração,
propriedades extras, PUT sem mudança e falha de validação. `UserPostgresIT` verifica os valores reais
do PostgreSQL e a atualização de V2 para V3, incluindo preservação dos dados, preenchimento dos legados,
restrição NOT NULL, novos IDs e idempotência. O teste de OpenAPI verifica o contrato de resposta.

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Pintegration verify
```

A suíte convencional contém 92 testes. Com Docker, o perfil completo inclui 18 testes PostgreSQL,
totalizando 110 testes. Consulte o resultado da execução correspondente ao commit no GitHub Actions.
