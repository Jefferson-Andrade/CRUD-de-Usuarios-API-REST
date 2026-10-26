# JWT Bearer — implementação em revisão

Esta branch adiciona autenticação JWT HS256 em paralelo ao HTTP Basic. **Não mesclar até a CI passar.**

## Configuração obrigatória

Defina `APP_JWT_SECRET` como um segredo aleatório de pelo menos 32 bytes UTF-8 em todos os ambientes de execução, incluindo Docker e CI. Nunca comite o valor. A aplicação falha ao iniciar sem essa variável.

Variáveis opcionais:
- `APP_JWT_ISSUER` (padrão `crud-api`)
- `APP_JWT_TTL_SECONDS` (padrão `900`, faixa 60–3600)

Em PowerShell, gere um segredo aleatório de 48 bytes:

```powershell
$env:APP_JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
```

## Autenticação

```http
POST /auth/login
Content-Type: application/json

{"username":"admin","password":"<senha configurada pelo administrador>"}
```

Resposta de sucesso:

```json
{"accessToken":"<jwt>","tokenType":"Bearer","expiresIn":900}
```

Use `Authorization: Bearer <jwt>` para chamadas protegidas. A validação consulta a conta atual no banco para conferir `enabled` e permissões, em vez de confiar em papéis gravados no token.

## Compatibilidade e limites

- HTTP Basic continua disponível.
- **CSRF continua obrigatório nas operações de escrita**, inclusive quando o cliente usa Bearer, enquanto coexistir a configuração de CSRF baseada em cookie. Para escrever, obtenha cookie/token via `GET /csrf` e envie `X-XSRF-TOKEN`.
- O login está isento de CSRF, pois recebe credenciais no corpo e não autentica por cookie.
- O JWT não implementa refresh token, revogação individual ou rate limiting; evite expor publicamente o login antes de adicionar proteção contra força bruta.
- Não use tokens ou senhas em logs. Sirva a API apenas por HTTPS fora de localhost.

## Validação pendente

Execute `./mvnw -B -ntp test` e `./mvnw -B -ntp -Pintegration verify` em ambiente com Docker. Confirme a CI e revise o comportamento de 401/403, CSRF e Swagger antes do merge.
