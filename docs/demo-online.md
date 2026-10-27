# Demonstração online com HTTPS

**Status em 26/10/2026:** API publicada no Render Free e conectada ao Neon Free,
ambos em Ohio. Swagger e health acessíveis por HTTPS; READER e dados fictícios
provisionados. As verificações administrativas listadas ao final continuam pendentes.

- Swagger: https://crud-usuarios-demo.onrender.com/swagger-ui/index.html
- Health: https://crud-usuarios-demo.onrender.com/actuator/health
- Conta de consulta: `demo-reader`; senha fornecida pelo autor, fora do repositório.
- Branch de implantação: `master`. A publicação inicial ocorreu após o PR #10;
  a v1.0.0 foi publicada após o PR #12, no commit `ee566dc206ad19f58f13ecaf0706b8c20027081f`.
- Deploy automático: **After CI Checks Pass**.
- Health check HTTP configurado no Render: `/actuator/health`.
- O banco hospedado informa PostgreSQL 18.6; os testes CI usam PostgreSQL 16.
  As quatro migrações foram aplicadas com sucesso no banco hospedado.

## Escolha do serviço e orçamento

Use um serviço compatível com o Dockerfile (Java 21) e PostgreSQL persistente independente
do container da API. Antes de contratar, compare o custo mensal total da API + banco,
memória disponível para a JVM, suspensão por inatividade, expiração do banco,
retenção, backups e cobrança de tráfego. A demonstração utiliza os planos Free do Render e Neon.
Confirme os limites atuais antes de ampliar recursos ou alterar o plano.

## Configuração e primeiro início

1. Crie um banco **exclusivo de demonstração**, sem dados pessoais reais.
2. No painel da hospedagem, configure as variáveis de [deploy.env.example](../deploy.env.example).
   O arquivo é um modelo; nunca envie uma cópia preenchida ao Git.
3. Ative `SPRING_PROFILES_ACTIVE=prod`. O processo escuta em `0.0.0.0` e usa
   `PORT` (8080 se ausente); o provedor deve terminar TLS e fornecer a URL HTTPS.
   Não habilite confiança em cabeçalhos de proxy sem conferir a topologia do provedor.
4. Informe `DB_URL` no formato JDBC, `DB_USERNAME` e `DB_PASSWORD`.
   Defina também `APP_JWT_SECRET` (segredo aleatório de pelo menos 32 bytes UTF-8);
   mantenha essa variável após desativar o bootstrap. Consulte o [guia JWT](jwt-authentication.md).
   Confira a CA, o hostname e o modo TLS recomendado para o PostgreSQL gerenciado.
   O exemplo `sslmode=verify-full` exige certificado confiável e nome correspondente;
   adapte o truststore/certificado quando o provedor exigir.
5. Use `/actuator/health` como health check. Flyway aplica as migrações existentes
   no banco novo e Hibernate permanece em `validate`. Nunca altere migrações já aplicadas.
6. Defina um ADMIN privado com `APP_ADMIN_USERNAME/PASSWORD`.
   Para criar o READER, ative `APP_DEMO_ENABLED=true` e configure
   `APP_DEMO_READER_USERNAME/PASSWORD`. Os nomes devem ser distintos.
   As senhas têm mínimo de 12 caracteres e máximo de 72 bytes UTF-8.
7. Opcionalmente ative `APP_DEMO_SEED_DATA=true`: três registros com nomes de
   demonstração e e-mails em `example.com` são inseridos. Novas execuções preservam
   registros que já tenham o mesmo e-mail, inclusive diferenças de maiúsculas.
   Os dados usam o índice único existente; nenhuma nova migração é necessária.
8. Após confirmar as contas, desative `APP_DEMO_ENABLED` e `APP_DEMO_SEED_DATA`,
   remova as variáveis de bootstrap ADMIN e READER e reimplante.
   As contas e os registros continuam no banco. A API não redefine senhas existentes.

Uma conta READER desativada ou com papel diferente impede o bootstrap de demonstração.
O bootstrap não reativa nem promove contas. As credenciais ADMIN nunca devem aparecer no
README, Swagger ou logs. Compartilhe somente credenciais da conta READER destinada ao público.
Se precisar trocar a senha, faça manutenção privada do hash BCrypt no banco; alterar a
variável de bootstrap não redefine uma conta existente.

## Como apresentar a API

Os links HTTPS estão no início deste guia e no README. O Swagger abre sem login;
para consultar, use a conta READER. Compartilhe somente a senha destinada à
demonstração, nunca a senha do PostgreSQL. Render Free suspende a API após
inatividade; Neon Free também pode suspender o processamento do banco.
O primeiro acesso pode ser mais lento. Os planos possuem cotas de uso.

No Swagger, abra **Authorize** e informe usuário e senha READER (HTTP Basic).
A conta consulta usuários; POST, PUT e DELETE exigem ADMIN e CSRF.
Health anônimo ou READER retorna somente status. Detalhes são restritos a ADMIN
e outros endpoints operacionais continuam bloqueados.

## Verificação do ambiente real

Configure `DEMO_URL` com a origem HTTPS final (sem caminho), e
`DEMO_READER_USERNAME` / `DEMO_READER_PASSWORD` em variáveis locais.
Execute `python scripts/demo-smoke.py`. O script verifica TLS com a validação padrão
do Python, recusa redirects e faz apenas consultas. Não use credenciais ADMIN.
Ele confere Swagger, OpenAPI, health público, 401 anônimo, consulta READER
e restrição dos endpoints operacionais.

A suíte automatizada verifica também 403 nas escritas READER com CSRF válido,
CSRF obrigatório para ADMIN, hash das senhas, bootstrap e fixtures idempotentes.
Execute `./mvnw -B -ntp -Pintegration verify` (PowerShell: `.\\mvnw.cmd -B -ntp -Pintegration verify`).

Para concluir a publicação, valide com ADMIN privado: criar, consultar, atualizar e
excluir um registro temporário usando CSRF. Crie outro registro fictício, anote seu ID,
reinicie/reimplante a API e confirme os mesmos dados antes de excluí-lo.
Isso verifica persistência real; o teste de fixtures repetidas não substitui essa etapa.
Confira Flyway e `X-Request-Id` nos logs. Use ambiente descartável para simular falhas.

## Atualização e recuperação

Antes de atualizar, confirme o backup e a compatibilidade das migrações.
Publique o commit aprovado pela CI, acompanhe health/logs e execute o smoke HTTPS.
O banco não pode ser recriado a cada deploy. Um rollback de imagem só é seguro se
continuar compatível com o schema; não remova registros de `flyway_schema_history`.
Desative o acesso READER se encerrar a demonstração.

## Verificação realizada em 26/10/2026

- Health público retornou `{"status":"UP"}`; Swagger carregou por HTTPS.
- Flyway aplicou V1–V4 e validou o schema nos deploys seguintes.
- READER autenticado: `GET /users` retornou HTTP 200 e os três registros fictícios.
- `POST /users` com READER e CSRF retornou HTTP 403, mensagem
  `Permissão insuficiente`. O corpo vazio evitou criar dados mesmo se houvesse
  uma regressão de autorização.
- A criação automática de conta e fixtures foi desativada após o provisionamento;
  os valores das credenciais de bootstrap READER foram esvaziados no ambiente.

- Após desativar o bootstrap e reimplantar (deploy `dep-dbfq0aon74is739srujg`),
  a mesma conta READER retornou HTTP 200. Foram preservados IDs 1–3, nomes,
  e-mails e timestamps dos três registros. Isso confirma persistência entre deploys.

## Verificação final da v1.0.0 em 26/10/2026

Conforme o [registro de conclusão do PR #12](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/pull/12),
o deploy `dep-dbfufigm7kps73ar0ot0` ficou LIVE às 20:56 (Brasília), no commit `ee566dc`.
A verificação HTTPS registrou health 200/UP, Swagger 200, OpenAPI 200 com versão 1.0.0
e alternativas Basic/Bearer, consulta anônima 401 e login sem campos 400.
Nenhum registro foi alterado nessa verificação. As evidências de persistência acima
correspondem à publicação anterior e permanecem identificadas como históricas.

A [CI final](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/runs/40422226402)
e o [workflow Docker dev/prod](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/runs/40422226471)
foram aprovados para esse mesmo commit. A validação em CI não substitui as verificações
administrativas do ambiente hospedado listadas abaixo.

## Etapas administrativas pendentes

- Nenhuma conta ADMIN foi criada nesta etapa. Provisionar acesso privado de
  manutenção se necessário, com senha diferente da conta pública READER.
- Validar o CRUD ADMIN com CSRF e um registro temporário no ambiente hospedado.
- Definir o procedimento de backup do banco conforme os limites do plano escolhido.
