# Demonstração online com HTTPS

**Status:** código preparado; hospedagem, banco e URL pública ainda não criados.
A etapa só estará concluída após validar o ambiente publicado.

## Escolha do serviço e orçamento

Use um serviço compatível com o Dockerfile (Java 21) e PostgreSQL persistente independente
do container da API. Antes de contratar, compare o custo mensal total da API + banco,
memória disponível para a JVM, suspensão por inatividade, expiração do banco,
retenção, backups e cobrança de tráfego. Nenhum provedor ou plano foi escolhido.
Confirme os preços e limites atuais no provedor antes de criar recursos.

## Configuração e primeiro início

1. Crie um banco **exclusivo de demonstração**, sem dados pessoais reais.
2. No painel da hospedagem, configure as variáveis de [deploy.env.example](../deploy.env.example).
   O arquivo é um modelo; nunca envie uma cópia preenchida ao Git.
3. Ative `SPRING_PROFILES_ACTIVE=prod`. O processo escuta em `0.0.0.0` e usa
   `PORT` (8080 se ausente); o provedor deve terminar TLS e fornecer a URL HTTPS.
   Não habilite confiança em cabeçalhos de proxy sem conferir a topologia do provedor.
4. Informe `DB_URL` no formato JDBC, `DB_USERNAME` e `DB_PASSWORD`.
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

Depois da publicação, registre aqui a URL HTTPS, o link do Swagger,
o acesso READER aprovado para divulgação e os limites confirmados do plano.
Até lá, não anuncie uma demonstração como disponível.

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

## Critérios pendentes de conclusão

- Escolher provedor e custo total aceitável.
- Criar serviços, verificar HTTPS, Flyway e banco persistente.
- Confirmar consulta READER, escritas bloqueadas e ADMIN privado com CSRF.
- Confirmar dados após reinício/redeploy.
- Publicar URL e limitações reais no README.
