# API e PostgreSQL com Docker Compose

## Primeira execucao

Requisitos: Docker Desktop com containers Linux e Docker Compose v2. Java e Maven
no host sao opcionais: o Dockerfile compila com Maven/Java 21 e executa com JRE 21
em um processo sem privilegios de root.

1. Copie `.env.example` para `.env`.
2. Preencha `APP_ADMIN_USERNAME` e `APP_ADMIN_PASSWORD` para criar a primeira conta.
   A senha deve ter pelo menos 12 caracteres e no maximo 72 bytes UTF-8.
   Nao publique o arquivo .env. Para valores contendo $ ou #, use aspas simples no .env.
3. Execute:

```powershell
docker compose up --build -d
docker compose ps
docker compose logs -f api
```

A API fica em http://localhost:8080; Swagger em
http://localhost:8080/swagger-ui/index.html. Autorize com a conta criada,
consulte GET /csrf antes de escrever e siga o [guia de autenticacao](authentication.md).
Sem bootstrap e sem contas existentes, a API inicia, mas consultas protegidas recusam acesso.

O banco precisa passar no pg_isready antes de iniciar a API. Essa verificacao indica
que o PostgreSQL aceita conexoes; a inicializacao da API ainda precisa concluir
Flyway, JPA e bootstrap. Confira o log de inicializacao e o Swagger.
Uma falha de migracao nao e corrigida automaticamente. A saude da API pode ser
consultada em /actuator/health; o healthcheck do Compose continua verificando somente
o banco. Consulte o [guia de monitoramento](monitoring.md).

## Configuracao local

| Variavel | Padrao | Uso |
|---|---|---|
| SPRING_PROFILES_ACTIVE | dev | Perfil da API (dev ou prod) |
| POSTGRES_DB | crud | Banco |
| POSTGRES_USER | postgres | Usuario do banco |
| POSTGRES_PASSWORD | postgres | Senha local do banco |
| DB_PORT | 5432 | Porta do banco no host |
| API_PORT | 8080 | Porta da API no host |
| APP_ADMIN_USERNAME | vazio | Bootstrap opcional do ADMIN |
| APP_ADMIN_PASSWORD | vazio | Senha inicial do ADMIN |

A API acessa `db:5432` na rede interna. API e banco publicam portas apenas em
127.0.0.1. Este Compose permanece local. Os perfis dev/prod estao descritos no
[guia de configuracao](profiles.md); a demonstracao HTTPS sera uma etapa posterior.

As credenciais POSTGRES_* inicializam somente um volume novo. Em volume existente,
mudar o .env nao altera usuarios ou senhas: configure os valores reais ja utilizados.
O bootstrap nao altera uma conta existente. Apos criar o ADMIN, remova os valores
APP_ADMIN_* do .env e recrie somente a API com `docker compose up -d api`.

## Reiniciar e parar sem apagar dados

```powershell
docker compose restart
docker compose down
docker compose up -d
```

O volume `postgres_data` permanece entre reinicializacoes e `down`.
Execute na mesma pasta e com o mesmo nome de projeto Compose para reutilizar o volume.
Mudar pasta ou `-p` pode selecionar outro volume. Nao use `down -v` no banco local.
O nome do servico db, do container postgres_db e do volume foi preservado.

Se o banco e legado e ainda nao possui historico Flyway, siga a adocao com backup
e baseline explicita no README antes de iniciar a API. Nao habilite baseline automatica
e nao remova o volume para contornar erros de migracao.

## Executar a API pelo Maven no host

Inicie apenas o banco para evitar duas APIs usando a mesma porta:

```powershell
docker compose stop api
docker compose up -d db
.\mvnw.cmd spring-boot:run
```

O Maven no host usa DB_URL, DB_USERNAME e DB_PASSWORD; ele nao carrega .env
automaticamente. Para valores diferentes dos padroes, configure essas variaveis
na sessao, assim como APP_ADMIN_USERNAME e APP_ADMIN_PASSWORD se necessario.

## Verificacao automatizada

O workflow Docker compila a imagem e usa um banco descartavel do runner.
O smoke test verifica acesso anonimo negado, autenticacao Basic, CSRF, CRUD,
migracoes V1-V4 e leitura de um registro depois de parar e subir API/banco.
A limpeza com volumes ocorre somente no runner CI, nunca no banco local.
A verificacao Docker roda nos perfis dev e prod e tambem simula indisponibilidade
do banco temporario para conferir health DOWN/UP e logs de falha. A suite completa Maven continua
no workflow CI - Testes do CRUD.

## Segredo JWT obrigatório

Preencha também `APP_JWT_SECRET` no `.env` antes de iniciar a API; o Compose repassa esse valor ao container. Gere um segredo aleatório de pelo menos 32 bytes UTF-8, conforme o [guia JWT](jwt-authentication.md). Para Maven no host, defina a mesma variável na sessão PowerShell. Não use o segredo descartável da CI em ambientes reais.
