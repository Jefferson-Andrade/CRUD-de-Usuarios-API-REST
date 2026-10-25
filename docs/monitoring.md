# Saude da API e investigacao de falhas

## Consultar disponibilidade

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Sem credenciais ou com perfil READER, a resposta contem somente o estado geral:

```json
{"status":"UP"}
```

UP retorna HTTP 200. DOWN ou OUT_OF_SERVICE retorna HTTP 503.
A verificacao inclui a conexao JDBC com o banco (PostgreSQL no runtime, H2 nos testes)
e os indicadores padrao disponiveis, como espaco em disco. Nao representa um
teste completo de todas as funcionalidades nem substitui alertas externos.

## Permissoes

| Recurso | Anonimo | READER | ADMIN |
|---|---|---|---|
| GET/HEAD /actuator/health | Resumo | Resumo | Componentes e detalhes |
| GET/HEAD /actuator/health/db | 401 | 403 | Estado e detalhes do banco |
| Outros endpoints /actuator | Negado | Negado | Negado |
| POST/PUT/DELETE /actuator/health | Negado | Negado | Negado |

Somente health e exposto por HTTP; a descoberta de endpoints esta desabilitada.
env, configprops, beans, metrics e loggers nao ficam acessiveis.
Detalhes e componentes usam when-authorized com papel ADMIN; nao ha senha nova
de monitoramento. A autenticacao segue o [guia HTTP Basic](authentication.md).
Credenciais incorretas podem retornar 401 mesmo na rota publica.
Em ambiente publicado, use HTTPS.

O arquivo monitoring.properties concentra a politica e e importado tanto no
runtime quanto nos contextos de teste. Vale para dev e prod. Nao habilite
show-details=always nem exposicao curinga para a demonstracao publica.

## Localizar falhas nos logs

Cada requisicao recebe X-Request-Id gerado pelo servidor. Em respostas 5xx,
FailureLoggingFilter registra uma linha como:

```text
http_failure requestId=<uuid> method=GET status=503
```

Excecoes nao tratadas tambem registram o tipo da excecao, sem sua mensagem,
e sao relancadas para manter o tratamento HTTP normal. Respostas esperadas
4xx nao geram esse aviso. O contexto de correlacao e limpo/restaurado ao final.

O filtro nao registra URL, parametros, corpo, cabecalhos nem credenciais.
Ele nao substitui os logs internos do framework: nunca coloque senhas em URLs,
mensagens de excecao ou logs personalizados. O perfil prod ja desativa SQL/bind.
Os logs padrao do Spring Boot indicam inicializacao, Flyway, conexoes e encerramento.

```powershell
docker compose logs --tail=100 api
docker compose logs -f api
```

Use o X-Request-Id da resposta para encontrar o aviso correspondente.
Um health DOWN gera aviso 503 quando o endpoint e consultado. Esta etapa
nao agenda verificacoes, nao envia alertas e nao inclui coleta centralizada.

## Validacao sem alterar o banco local

HealthEndpointTest verifica resumo publico, detalhes ADMIN, recusas de acesso
e uma falha de indicador simulada com HTTP 503 e correlacao no log.
FailureLoggingFilterTest verifica erros HTTP, excecoes e ausencia de dados sensiveis.

No CI, o smoke test Docker roda em dev e prod com PostgreSQL descartavel.
Confirma detalhes de PostgreSQL para ADMIN, bloqueio de /actuator/env, CRUD e
persistencia; depois para somente o banco temporario, verifica DOWN/503 e o aviso,
sobe o banco novamente e confirma UP/200. Nao execute esse roteiro de falha
no banco local ou em ambiente compartilhado.

A checagem JDBC pode aguardar ate o tempo de conexao do pool; uma falha nao
implica resposta instantanea. A verificacao Docker do banco continua usando
pg_isready. O endpoint health nao reinicia containers automaticamente.

Referencia: [Spring Boot Actuator - Endpoints](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html).
