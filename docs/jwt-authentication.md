# Autenticação HTTP Basic e JWT Bearer

JWT HS256 está integrado à `master` desde o PR #11. HTTP Basic continua disponível
para compatibilidade. As duas formas consultam a conta e o perfil atuais no banco.

## Configuração

Defina `APP_JWT_SECRET` como um segredo aleatório de pelo menos 32 bytes UTF-8.
A aplicação falha ao iniciar sem esse valor. Nunca comite o segredo.

No PowerShell, para Maven no host:

```powershell
$env:APP_JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\mvnw.cmd spring-boot:run
```

Para Docker, copie `.env.example` para `.env` e preencha `APP_JWT_SECRET` e as
credenciais de bootstrap ADMIN antes de `docker compose up --build -d`.
No Render, mantenha o segredo entre implantações, mesmo depois de remover o bootstrap.

| Variável | Padrão | Regra |
|---|---|---|
| `APP_JWT_ISSUER` | `crud-api` | Emissor usado na emissão e validação |
| `APP_JWT_TTL_SECONDS` | `900` | De 60 a 3600 segundos |
| `APP_LOGIN_MAX_ATTEMPTS` | `10` | Requisições de login por endereço remoto e janela; positivo |
| `APP_LOGIN_WINDOW_SECONDS` | `60` | Janela positiva de até 86400 segundos |
| `APP_LOGIN_MAX_CLIENTS` | `10000` | Capacidade positiva de endereços em memória |

## Obter e usar o token

```http
POST /auth/login
Content-Type: application/json

{"username":"<conta configurada>","password":"<senha da conta>"}
```

Resposta HTTP 200, com `Cache-Control: no-store` e `Pragma: no-cache`:

```json
{"accessToken":"<jwt>","tokenType":"Bearer","expiresIn":900}
```

Use `Authorization: Bearer <jwt>` nas chamadas protegidas. Não envie aspas nem
o prefixo Bearer dentro do campo de token do Swagger. Username é obrigatório e
limitado a 100 caracteres; senha é obrigatória e limitada a 72 bytes UTF-8 (BCrypt).
Login inválido, conta desconhecida ou desabilitada retorna 401 com a mesma mensagem.
JSON inválido e campos inválidos retornam 400 no contrato de erro da API.

A assinatura, o emissor e a validade temporal são verificados. A conta é consultada
a cada chamada: exclusão/desativação invalida o acesso e alterações de perfil têm
efeito sobre tokens existentes. O JWT contém `sub`, `iat`, `exp`, `iss`; não contém
senha, hash, dados pessoais do CRUD ou perfis desatualizáveis.

## CSRF e permissões

CSRF permanece obrigatório em POST/PUT/DELETE do CRUD com **Basic e Bearer**.
O login recebe credenciais explícitas no JSON e não cria sessão ou cookie de autenticação;
somente `POST /auth/login` está isento dessa verificação.

1. Autentique `GET /csrf` com Basic ou Bearer.
2. Preserve o cookie `XSRF-TOKEN`.
3. Nas escritas, envie o cookie, o cabeçalho `X-XSRF-TOKEN` com seu valor e a autenticação.
4. ADMIN pode escrever; READER recebe 403 mesmo com token CSRF válido.

No Swagger, autorize Basic ou Bearer e execute `GET /csrf` antes de escrever.
A opção CSRF do Swagger envia o cabeçalho usando o cookie. Use ADMIN somente
em manutenção privada; a demonstração pública utiliza READER.

## Limite de tentativas e proxy

Todas as requisições `POST /auth/login`, inclusive sucessos e corpos inválidos, consomem
a janela antes da leitura do corpo e da verificação de senha. Ao exceder o limite,
a API retorna **429 Too Many Requests**, `Retry-After` em segundos, `Cache-Control: no-store`
e `ErrorResponseDTO`. Pedidos bloqueados não estendem a janela.

A chave é o endereço remoto informado pelo servlet. O filtro não interpreta
`X-Forwarded-For`; não ative confiança nesses cabeçalhos sem validar a cadeia
de proxies. Atrás de um proxy, clientes podem compartilhar o mesmo limite.
Configure proteção no ingresso com a política de IP confiável do provedor.

O armazenamento é limitado e local à instância: reinícios apagam as janelas e
múltiplas instâncias não compartilham contadores. Quando a capacidade está cheia
de janelas ativas, novos endereços recebem 429 até haver capacidade.
Este limite cobre o endpoint de login JWT; HTTP Basic exige proteção adicional
no ingresso se exposto publicamente. Não é uma defesa completa contra ataques distribuídos.

## Expiração e recuperação

Não há refresh token, logout de token ou revogação individual. Após expirar,
faça novo login. Desabilitar a conta bloqueia todos os seus tokens e o Basic.
Rotacionar `APP_JWT_SECRET` e reimplantar invalida todos os JWT anteriores;
mantenha o valor fora do Git e dos logs. Use HTTPS fora de localhost.

## Testes

`JwtAuthenticationTest` cobre login, 401/403, assinatura adulterada, expiração,
emissor, conta removida/desabilitada, mudança de perfil, CSRF com cookie real e
o CRUD ADMIN completo. `LoginRateLimiterTest` usa relógio controlado para testar
expiração exata, concorrência e capacidade; `LoginRateLimitTest` verifica o contrato HTTP.

Execute `.\mvnw.cmd -B -ntp -Pintegration verify` no PowerShell ou
`./mvnw -B -ntp -Pintegration verify` em Linux/macOS, com Docker disponível.
