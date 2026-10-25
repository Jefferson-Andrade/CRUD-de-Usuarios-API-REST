# Autenticação e permissões

## Modelo

A API usa Spring Security com **HTTP Basic**, sem JWT, refresh token ou sessão de login.
O cliente envia `Authorization: Basic <base64(username:password)>` em cada chamada protegida.
Base64 não criptografa: fora de localhost, disponibilize a API somente por HTTPS.

`users` continua representando os registros do CRUD, apenas com `id` e `name`.
As credenciais ficam em `auth_accounts`, criada pela migração Flyway V2:
`username`, `password_hash`, `role` e `enabled`. Não existe cadastro público de contas.
Contas desabilitadas não autenticam. Usuários desconhecidos e senhas incorretas recebem a mesma mensagem.

| Recurso | Acesso |
|---|---|
| GET/HEAD `/users` e `/users/{id}` | READER ou ADMIN |
| POST `/users`, PUT/DELETE `/users/{id}` | ADMIN e token CSRF válido |
| GET `/csrf` | Qualquer conta autenticada |
| GET Swagger e `/v3/api-docs` | Público |
| Demais rotas/métodos | Negados por padrão |

Sem credenciais, consultas protegidas retornam 401 com `WWW-Authenticate`.
Credenciais inválidas também retornam 401. Permissão insuficiente retorna 403.
O filtro CSRF roda antes da autenticação Basic: escritas sem token válido podem retornar 403
mesmo sem credenciais. Os erros usam `status`, `error`, `message` e `timestamp`, como os erros do CRUD.

## Criar o administrador inicial no Windows

Primeiro inicie o PostgreSQL com `docker compose up -d`. Na pasta do projeto, execute:

```powershell
$env:APP_ADMIN_USERNAME = 'admin'
$adminSecret = Read-Host 'Senha inicial (mínimo 12 caracteres, máximo 72 bytes UTF-8)' -AsSecureString
$env:APP_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminSecret).Password
try {
    .\mvnw.cmd spring-boot:run
} finally {
    Remove-Item Env:APP_ADMIN_PASSWORD, Env:APP_ADMIN_USERNAME -ErrorAction SilentlyContinue
    $adminSecret = $null
}
```

O bootstrap grava apenas o hash BCrypt (custo 12), nunca imprime a senha e nunca sobrescreve
uma conta existente. Ao reiniciar, as variáveis podem ser omitidas: a conta permanece no banco.
Se informar somente uma variável ou valores fora dos limites, a aplicação recusa a inicialização.
Não há senha padrão da API. Sem bootstrap e sem contas provisionadas, nenhuma credencial será aceita.
Alterar as variáveis depois da criação **não troca a senha existente**.

Credenciais do PostgreSQL são independentes: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` configuram
a conexão JDBC. Os padrões `postgres/postgres` correspondem apenas ao Docker Compose local.

Para provisionar READER ou outra conta, um administrador do banco deve gerar um hash com o
mesmo `BCryptPasswordEncoder` e inserir `username`, `password_hash`, `role='READER'` e `enabled=true`.
Nunca use senha em texto puro na coluna de hash. Não há endpoint de provisionamento ou troca de senha nesta etapa.

## Swagger

1. Abra `http://localhost:8080/swagger-ui/index.html`.
2. Clique em **Authorize** e informe a conta criada.
3. Execute GET `/csrf` (ou GET `/users`) antes de escrever.
4. Execute o CRUD. O Swagger usa o cookie `XSRF-TOKEN` e envia `X-XSRF-TOKEN` automaticamente.

O cookie CSRF não é uma credencial de autenticação. Ele pode ser lido pelo JavaScript do Swagger;
o cabeçalho Basic continua obrigatório. Atualize o token com GET `/csrf` se os cookies forem apagados.
A configuração preserva a proteção XOR de tokens renderizados e aceita o token simples no cabeçalho do cookie.
Não existe logout de servidor porque não há sessão autenticada; remova a autorização do cliente.

## PowerShell: chamada autenticada com CSRF

```powershell
$credential = Get-Credential -UserName 'admin' -Message 'Credencial da API'
$pair = '{0}:{1}' -f $credential.UserName, $credential.GetNetworkCredential().Password
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair)) }
$csrf = Invoke-RestMethod 'http://localhost:8080/csrf' -Headers $headers -SessionVariable apiCookies
$headers[$csrf.headerName] = $csrf.token
Invoke-RestMethod 'http://localhost:8080/users' -Method Post -Headers $headers -WebSession $apiCookies `
    -ContentType 'application/json' -Body '{"name":"Jefferson Andrade"}'
$pair = $null
$headers.Clear()
$credential = $null
```

## Fluxo no código

1. `SecurityConfig` filtra a requisição, valida CSRF em escritas e extrai o Basic via Spring Security.
2. `AccountConfig` carrega a conta por consulta JDBC parametrizada. O provedor do Spring verifica
   a senha com BCrypt e verifica se a conta está habilitada.
3. As regras comparam as authorities `ROLE_READER`/`ROLE_ADMIN`; o controller recebe apenas chamadas autorizadas.
4. `UserController` → `UserService` → `UserRepository` mantém o fluxo do CRUD e seus DTOs sem credenciais.

O `SecurityContext` vale somente para a requisição. `CsrfController` expõe o token CSRF simples,
sem criar JWT ou devolver senha/hash. `OpenApiConfig` documenta Basic e as respostas 401/403.

## Migração e testes

Em banco já gerenciado pelo Flyway, V2 cria somente `auth_accounts`, sem alterar `users` ou V1.
Em banco legado sem histórico, siga o procedimento de backup e baseline V1 no README antes de iniciar:
após a baseline, V2 deve ser executada e aparecer com sucesso no histórico, mantendo os registros existentes.

`AuthenticationTest` valida credenciais reais no H2, contas desabilitadas, papéis, bootstrap,
ausência de sessão e fluxo real cookie/cabeçalho CSRF para criar, atualizar e excluir.
Os testes de controller continuam verificando regras de negócio com ADMIN e CSRF simulados.
`UserPostgresIT` valida V1/V2, adoção de banco legado preservando dados e autenticação de contas no PostgreSQL temporário.

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Pintegration verify
```

O segundo comando exige Docker. Nenhum teste aplica baseline ou altera o banco local `crud`.
