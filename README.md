# 🚀 CRUD de Usuários — API REST

[![CI - Testes do CRUD](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/workflows/ci.yml)

> API REST para gerenciamento de usuários, desenvolvida com **Java 21** e **Spring Boot 4.1.1**, aplicando conceitos de desenvolvimento backend, arquitetura em camadas, DTOs, validação, persistência de dados e testes automatizados.

## Autenticação HTTP Basic

A API exige conta com perfil **READER** (consultas) ou **ADMIN** (consultas e alterações).
Senhas usam BCrypt em tabela própria; escritas também exigem CSRF.
Antes de executar os exemplos abaixo, siga o [guia de autenticação](docs/authentication.md) para criar
o administrador por variáveis de ambiente e configurar Swagger ou PowerShell. Não há credenciais padrão da API.

## Monitoramento básico

`GET /actuator/health` retorna o status geral publicamente, incluindo a saúde
da conexão com o banco. Somente ADMIN vê componentes e detalhes; os demais
endpoints operacionais permanecem bloqueados. Falhas 5xx geram logs com
`X-Request-Id` para correlação, sem registrar dados da requisição.
Veja o [guia de monitoramento](docs/monitoring.md).

## E-mail obrigatório e único

Novos cadastros e atualizações exigem **name** e **email**. O e-mail é validado, normalizado para
minúsculas e protegido por índice único no PostgreSQL; duplicidade retorna **409 Conflict**.
A migração **V4** preserva os usuários existentes com e-mail nulo até o preenchimento com um endereço real.
Veja o [guia de e-mail dos usuários](docs/user-email.md) para regras, concorrência e migração.

## Datas de criação e atualização

As respostas do CRUD incluem **email**, **createdAt** e **updatedAt**, preenchidos pelo backend em UTC.
A migração V3 preenche os registros antigos com a data da migração como referência, pois a data original é desconhecida.
Veja o [guia de auditoria dos usuários](docs/user-audit.md) para comportamento, exemplos e atualização do banco.

---

## 🛠️ Tecnologias

| Tecnologia | Utilização |
|---|---|
| ☕ **Java 21** | Linguagem principal |
| 🌱 **Spring Boot 4.1.1** | Framework backend |
| 🌐 **Spring Web MVC** | Desenvolvimento da API REST |
| 🗃️ **Spring Data JPA** | Persistência de dados |
| 🐘 **PostgreSQL 16** | Banco de dados |
| **Spring Boot Actuator** | Saúde da aplicação e do banco |
| **Flyway** | Migrações SQL versionadas e histórico do schema |
| 🐳 **Docker / Docker Compose** | Containerização da API e do banco |
| 📖 **OpenAPI / Swagger** | Documentação e testes da API |
| 🧪 **JUnit Jupiter** | Testes automatizados |
| 🎭 **Mockito** | Testes unitários |
| 🗄️ **H2** | Banco em memória para o teste de contexto |
| 🧰 **Testcontainers** | PostgreSQL temporário nos testes de integração |
| ✅ **Maven Failsafe** | Execução e verificação dos testes de integração |
| 📦 **Maven** | Gerenciamento e build |
| 🔧 **Git / GitHub** | Versionamento |

---

## 📋 Sobre o projeto

Este projeto consiste em uma API REST para gerenciamento de usuários, implementando as principais operações de um CRUD:

- ✅ Criação de usuários
- ✅ Consulta de usuários
- ✅ Consulta por ID
- ✅ Atualização de usuários
- ✅ Exclusão de usuários
- ✅ Validação dos dados de entrada
- ✅ Tratamento global de exceções
- ✅ Persistência com PostgreSQL
- ✅ Migrações SQL com Flyway e validação do schema pelo Hibernate
- ✅ Documentação com Swagger / OpenAPI
- ✅ Testes automatizados
- ✅ Testes de integração com PostgreSQL real e Testcontainers

O projeto foi desenvolvido com foco em organização, separação de responsabilidades e aplicação de boas práticas de desenvolvimento backend.

---

## 🏗️ Arquitetura

A aplicação utiliza uma arquitetura em camadas:

![Arquitetura do CRUD: Controller, Service, Repository e PostgreSQL, com migrações Flyway](docs/images/arquitetura-crud.svg)

### Camadas

**Controller**  
Responsável por receber as requisições HTTP e retornar as respostas da API.

**Service**  
Concentra a lógica de negócio da aplicação.

**Repository**  
Responsável pelo acesso e persistência dos dados através do Spring Data JPA.

**DTOs**  
Utilizados para controlar os dados de entrada e saída da API, evitando a exposição direta da entidade JPA.

---

## 🔌 Endpoints

| Método | Endpoint | Descrição | Status |
|:---:|---|---|:---:|
| 🟢 **POST** | `/users` | Criar usuário | `201 Created` |
| 🔵 **GET** | `/users` | Listar usuários com paginação, ordenação e busca por nome | `200 OK` |
| 🔵 **GET** | `/users/{id}` | Buscar usuário por ID | `200 OK` |
| 🟡 **PUT** | `/users/{id}` | Atualizar usuário | `200 OK` |
| 🔴 **DELETE** | `/users/{id}` | Excluir usuário | `204 No Content` |

---

## 📥 Exemplos de requisições

### Criar usuário

**POST `/users`**

```json
{
  "name": "Jefferson Andrade",
  "email": "jefferson@example.com"
}
```

### Resposta

```json
{
  "id": 1,
  "name": "Jefferson Andrade",
  "email": "jefferson@example.com",
  "createdAt": "2026-10-24T18:00:00Z",
  "updatedAt": "2026-10-24T18:00:00Z"
}
```

---

### Buscar usuário

**GET `/users/1`**

```json
{
  "id": 1,
  "name": "Jefferson Andrade",
  "email": "jefferson@example.com",
  "createdAt": "2026-10-24T18:00:00Z",
  "updatedAt": "2026-10-24T18:00:00Z"
}
```

---

### Atualizar usuário

**PUT `/users/1`**

```json
{
  "name": "Jefferson Andrade Atualizado",
  "email": "jefferson@example.com"
}
```

### Resposta

```json
{
  "id": 1,
  "name": "Jefferson Andrade Atualizado",
  "email": "jefferson@example.com",
  "createdAt": "2026-10-24T18:00:00Z",
  "updatedAt": "2026-10-24T18:30:00Z"
}
```

---

### Listar usuários: paginação, ordenação e busca

**Mudança de contrato:** `GET /users` retorna um objeto com `content` e metadados, inclusive sem parâmetros. Antes retornava um array na raiz. Consumidores devem passar a ler `response.content`. POST, GET por ID e PUT incluem email, createdAt e updatedAt; DELETE continua retornando 204. Não existe listagem ilimitada alternativa.

| Parâmetro | Padrão | Regra |
|---|---|---|
| `page` | `0` | Inteiro a partir de zero |
| `size` | `10` | Inteiro de 1 a 100 |
| `sort` | `id,asc` | Um único par: `id,asc`, `id,desc`, `name,asc` ou `name,desc` |
| `name` | Sem filtro | Até 255 caracteres recebidos; espaços nas pontas são removidos; vazio não filtra |

A busca é parcial e ignora maiúsculas/minúsculas, sem remover acentos. Os caracteres `%` e `_` são tratados literalmente pela consulta derivada do Spring Data JPA. A ordenação por nome usa ID como desempate **na mesma direção**: `name,asc` usa `name ASC, id ASC`; `name,desc` usa `name DESC, id DESC`.

Exemplos:

```http
GET /users
GET /users?page=0&size=2&sort=name,asc
GET /users?name=Jefferson&page=0&size=10
GET /users?page=1&size=5&sort=id,desc
```

Para uma base com Ana, Bruno e Carla, em ordem de ID, `GET /users?page=0&size=2` retorna:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Ana",
      "email": "ana@example.com",
      "createdAt": "2026-10-24T18:00:00Z",
      "updatedAt": "2026-10-24T18:00:00Z"
    },
    {
      "id": 2,
      "name": "Bruno",
      "email": "bruno@example.com",
      "createdAt": "2026-10-24T18:00:00Z",
      "updatedAt": "2026-10-24T18:00:00Z"
    }
  ],
  "page": 0,
  "size": 2,
  "totalElements": 3,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

Os totais refletem o filtro. Sem resultados, retorna HTTP 200 com `content: []`, `totalElements: 0` e `totalPages: 0`. Uma página além da última também retorna 200 e conteúdo vazio, preservando os totais reais. `first` indica índice zero e `last` indica ausência de página seguinte.

Retorna **400 Bad Request**, no padrão `status`, `error`, `message` e `timestamp`, para página negativa, tamanho fora da faixa, números inválidos ou fora do intervalo de um inteiro Java, offset (`page × size`) acima de 2.147.483.647 (limite do JPA), parâmetros de paginação vazios, filtro acima de 255 caracteres e sort inválido ou repetido. Exemplos inválidos: `size=101`, `page=abc`, `sort=email,asc` e `sort=name,asc&sort=id,desc`. A validação ocorre antes da criação do Pageable; falhas inesperadas não são convertidas em 400.

A paginação e o filtro são executados pelo PostgreSQL através de `findAll(Pageable)` ou `findByNameContainingIgnoreCase(name, pageable)`. A aplicação converte somente os resultados da página para DTOs e usa `PageResponseDTO` como contrato público, sem expor a serialização interna de `PageImpl`.

Esta melhoria não altera a tabela nem a migração V1. Busca parcial e contagem de totais ainda podem percorrer muitos registros; medir antes de propor índices. Paginação por offset não fornece um snapshot: inserções, alterações ou exclusões concorrentes podem mudar a composição das páginas entre requisições.

Referência: [Spring Data JPA — Query Methods](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).

---

### Excluir usuário

**DELETE `/users/1`**

```text
204 No Content
```

---

# 📸 Demonstração

## Swagger / OpenAPI

Documentação interativa da API através do Swagger UI.

![Swagger da API](https://github.com/user-attachments/assets/72f5de4c-5b12-4935-9441-ba753a13e739)

---

## Criando um usuário

Exemplo de execução do endpoint `POST /users`.

![POST /users](https://github.com/user-attachments/assets/27e9ef95-168d-46c7-903d-18b8b23e29ed)

---

## Excluindo um usuário

Exemplo de execução do endpoint `DELETE /users/{id}`.

![DELETE /users/{id}](https://github.com/user-attachments/assets/49dc4fd8-615f-414b-b00d-4f6cce461a18)

---

# 📖 Documentação da API

Com a aplicação em execução:

### Swagger UI

```text
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

O Swagger permite visualizar os endpoints, modelos de dados e executar requisições diretamente pela interface web.

---

# 🗄️ Banco de dados

O projeto utiliza **PostgreSQL 16** para persistência dos usuários.

### Configuração

```text
Database: crud
Username: postgres
Password: postgres
Host: localhost
Port: 5432
```

A comunicação com o banco é realizada através do **Spring Data JPA**.

### Migrações com Flyway

<p align="center">
  <img src="docs/images/flyway.jpg" alt="Ilustração da evolução do CRUD com Flyway, Hibernate em validate e 18 testes aprovados" width="640" />
</p>

*Ilustração da melhoria implementada. As migrações disponíveis são V1 (users), V2 (auth_accounts), V3 (datas de auditoria) e V4 (e-mail único). A imagem registra uma etapa anterior. A validação dos 18 testes foi realizada em PostgreSQL temporário, sem aplicar baseline no banco local.*

[Ver execução aprovada no GitHub Actions](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/runs/39037493707).

O **Flyway** cria e evolui a estrutura através de scripts em `src/main/resources/db/migration`. O Hibernate apenas valida o mapeamento das entidades:

```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=false
```

A migração `V1__create_users_table.sql` cria `users` com `id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY` e `name VARCHAR(255)` nullable, preservando o mapeamento atual. Em um banco vazio, inicie a aplicação normalmente: o Flyway aplica V1, V2, V3 e V4 antes da validação JPA. Em novas inicializações, as versões aplicadas são conferidas e não são executadas novamente.

Confira o histórico:

```powershell
docker compose exec db psql -U postgres -d crud -c "SELECT version, description, type, success FROM public.flyway_schema_history ORDER BY installed_rank;"
```

**Não edite migrações já aplicadas.** Para mudanças futuras, adicione `V5__descricao.sql`, `V6__descricao.sql` etc., com dois sublinhados. Flyway detecta alterações de checksum. Não use `repair` para ocultar divergências.

### Adoção em um banco existente

Se o volume já contém a tabela `users`, a primeira inicialização será interrompida até a adoção explícita. A baseline registra o ponto de partida; **não verifica nem corrige a estrutura existente**. Não habilite `baseline-on-migrate` globalmente e não execute `docker compose down -v`.

1. Pare a aplicação e outras gravações. Confirme o banco, schema e conexão de destino. O procedimento abaixo usa o Compose deste repositório (`crud`, usuário `postgres`, schema `public`).
2. Gere backup e copie-o para o host. Os comandos evitam redirecionar bytes binários pelo PowerShell:

```powershell
docker compose exec db pg_dump -U postgres -d crud -Fc -f /tmp/crud_before_flyway.dump
docker compose cp db:/tmp/crud_before_flyway.dump ./crud_before_flyway.dump
```

Guarde esse backup fora do repositório. Não sobrescreva um backup anterior; use um nome diferente para cada execução.

3. Restaure em **outro banco vazio**, usando um nome ainda não existente, e confira o resultado. Exemplo de cópia de validação:

```powershell
docker compose exec db createdb -U postgres crud_flyway_check
docker compose exec db pg_restore -U postgres -d crud_flyway_check --no-owner --exit-on-error /tmp/crud_before_flyway.dump
docker compose exec db psql -U postgres -d crud_flyway_check -c "\d+ public.users"
docker compose exec db psql -U postgres -d crud_flyway_check -c "SELECT id, name FROM public.users ORDER BY id;"
```

Compare a estrutura com a V1: chave primária `id bigint`, geração de ID identity (`BY DEFAULT` ou equivalente compatível), `name character varying(255)` nullable, schema `public`. Confira também outras tabelas/objetos existentes, sequências e quantidade de registros. Resolva diferenças antes da baseline; não adapte a V1 aplicada em outros ambientes para esconder divergências.

4. Com o **Flyway CLI instalado**, registre a baseline primeiro na cópia. Use uma versão compatível com a dependência gerenciada no POM efetivo (`./mvnw help:effective-pom`). No PowerShell, configure a senha somente para esta sessão:

```powershell
$env:FLYWAY_PASSWORD = 'postgres'
flyway -url=jdbc:postgresql://localhost:5432/crud_flyway_check -user=postgres -schemas=public -defaultSchema=public -baselineVersion=1 baseline
Remove-Item Env:FLYWAY_PASSWORD
```

Inicie a aplicação contra a cópia, sem alterar a conexão padrão do projeto:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://localhost:5432/crud_flyway_check"
```

Confira `flyway_schema_history` nessa cópia: versão `1`, tipo `BASELINE`, sucesso, e versões `2`, `3` e `4`, migrações SQL, sucesso. A V1 não será executada sobre a tabela existente. Confira IDs, nomes, contagem e geração de novos IDs, exercite o CRUD e reinicie a aplicação. Compare os registros anteriores antes de aceitar a adoção.

5. Somente depois dessa validação, pare a aplicação e faça a mesma operação explícita no banco original (`-url=jdbc:postgresql://localhost:5432/crud`), conferindo novamente o destino e mantendo o backup. Inicie normalmente e verifique histórico e dados. Este procedimento exige acesso e inspeção do banco local; os testes automatizados usam apenas bancos temporários.

Referências: [Spring Boot — Database Initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html) e [Flyway — baseline](https://documentation.red-gate.com/fd/baseline-277578974.html).

---

# 🐳 Docker

API Java 21 e PostgreSQL iniciam juntos. Copie `.env.example` para `.env`,
configure a conta ADMIN e execute:

```powershell
docker compose up --build -d
docker compose logs -f api
```

Swagger: http://localhost:8080/swagger-ui/index.html.
O banco possui healthcheck e o volume persiste entre reinicializações.
Consulte o [guia Docker](docs/docker.md) para credenciais, banco existente,
logs, parada, execução Maven no host e verificação automatizada.
O Compose permanece local; a demonstração online é uma etapa posterior.

## Perfis de configuração

O perfil `dev` é o padrão local. O perfil `prod` exige `DB_URL`, `DB_USERNAME` e
`DB_PASSWORD` não vazios e desativa a exibição de SQL. O bootstrap ADMIN continua
opcional e sem senha padrão. Não combine `dev` e `prod`.
Consulte o [guia de perfis](docs/profiles.md) para execução, variáveis e testes.
O workflow Docker verifica o CRUD e a persistência nos dois perfis.

---

# 🧪 Testes automatizados

A suíte utiliza **JUnit Jupiter**, **Mockito**, **MockMvc**, **Spring Boot Test**, **H2** e **Testcontainers**. Os testes com mocks verificam comportamentos isolados; os testes de integração percorrem as camadas da aplicação e verificam a persistência em PostgreSQL real.

### Organização da suíte

| Classe | Tipo | O que verifica | Testes |
|---|---|---|:---:|
| `UserServiceTest` | Unitário, com repository mockado | CRUD, filtro normalizado, seleção da consulta paginada, conversão dos DTOs e metadados | 9 |
| `UserControllerTest` | Web, com service mockado | CRUD, padrões e parâmetros de listagem, limites, erros 400 e falhas inesperadas | 30 |
| `CrudApplicationTests` | Contexto, com H2 em memória | Carregamento da aplicação | 1 |
| `UserPostgresIT` | Integração, com PostgreSQL 16 e sem mocks | CRUD, paginação, busca, ordenação, OpenAPI, Flyway e integridade de escritas inválidas | 25 |
| `UserValidationTest` | Web, com service mockado | Limites POST/PUT, corpos inválidos, contrato 400/404, timestamp e falhas inesperadas | 29 |
| `UserOpenApiTest` | Aplicação completa com H2 | Schemas de validação e de respostas de erro no OpenAPI | 1 |
| `AuthenticationTest` | Aplicação completa com H2 | Credenciais, papéis, BCrypt, bootstrap e cookie CSRF real | 17 |
| `UserAuditTest` | Aplicação completa com H2 | Datas automáticas, leitura/listagem, alterações, entrada não controlável e preservação | 5 |
| `UserEmailTest` | Web, com service mockado | E-mail obrigatório, formato, normalização, limite e conflito específico | 19 |
| `ProfileConfigurationTest` | Configuração real, sem banco | Perfis, variáveis obrigatórias e isolamento de credenciais | 10 |
| `HealthEndpointTest` | Aplicação completa com H2 | Resumo, detalhes, permissões e falha simulada | 15 |
| `FailureLoggingFilterTest` | Filtro isolado | Correlação, exceções e proteção de dados nos logs | 3 |
| **Total** | | | **164** |

### Executar os testes sem Docker

Windows (PowerShell):

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

Esses comandos executam **139 testes**: serviço, controllers, validação de nome/e-mail, autenticação, auditoria, contexto H2, contrato OpenAPI, configuração dos perfis, saúde e logs de falha. Não exigem PostgreSQL ou Docker.

### Executar a suíte completa com PostgreSQL

Pré-requisitos: Java 21 e Docker acessível. No Windows, mantenha o Docker Desktop em execução com containers Linux.

Windows (PowerShell):

```powershell
.\mvnw.cmd -Pintegration verify
```

Linux/macOS:

```bash
./mvnw -Pintegration verify
```

O perfil `integration` prevê os 139 testes convencionais e os 25 testes `UserPostgresIT`, totalizando **164 testes** quando o Docker está acessível. O Maven Failsafe executa as classes `*IT` e verifica o resultado na etapa `verify`.

Não é necessário iniciar o banco com `docker compose up`. O Testcontainers cria um PostgreSQL temporário com a imagem `postgres:16-alpine`, banco `crud_integration` e porta aleatória, separado do banco da aplicação. A primeira execução pode baixar as imagens necessárias. A suíte falha se o Docker não estiver acessível.

### Cenários de integração

- **Paginação e busca:** primeira/segunda página sem sobreposição, totais filtrados, página além do fim, filtro parcial sem diferenciar caixa, filtros vazios e sem resultados, ordenação asc/desc com desempate por ID e busca literal por `%` e `_`.
- **OpenAPI:** contrato paginado, parâmetros e limites conferidos no documento gerado em `/v3/api-docs`.

- **Criação, consulta e listagem:** POST retorna `201` e um ID gerado; consultas SQL conferem o nome e a quantidade de registros; GET por ID e listagem retornam os dados salvos.
- **Atualização:** PUT retorna `200`, mantém o ID e altera o nome; uma consulta SQL e um novo GET confirmam a alteração persistida, sem criar outro usuário.
- **Exclusão:** DELETE retorna `204` com corpo vazio; uma consulta SQL confirma a remoção; GET posterior retorna `404` com os campos de erro esperados, e a listagem fica vazia.

- **Migrações:** a inicialização usa Flyway com Hibernate em `validate`; o histórico registra V1, V2, V3 e V4. Uma nova instância de Flyway não reaplica a migração e preserva os dados.
- **Baseline:** um schema isolado com dados preexistentes recusa adoção automática. A baseline explícita na versão 1 preserva IDs, nomes, contagem e geração de novos IDs. Esse cenário não substitui a inspeção do banco local.

As requisições usam MockMvc com a aplicação completa, passando por **controller → service → repository → PostgreSQL**. Não há mocks nem uma transação de teste envolvendo as requisições; as consultas SQL verificam as gravações confirmadas no banco.

Os dados são limpos antes de cada teste. O PostgreSQL é declarado como um bean de teste com `@ServiceConnection`, que fornece os dados de conexão ao Spring Boot. Ao final da classe, o Spring encerra a aplicação e as conexões antes de parar o container.

### Resultado histórico (antes do Flyway)

Na execução de **05/10/2026**, a suíte completa terminou com **`BUILD SUCCESS`**:

- **13 testes existentes:** nenhuma falha, erro ou teste ignorado.
- **3 testes de integração com PostgreSQL:** nenhuma falha, erro ou teste ignorado.
- Encerramento normal das conexões, sem o erro anterior de finalização do processo.

Os relatórios ficam em `target/surefire-reports` para os testes existentes e em `target/failsafe-reports` para os testes de integração.

---

### Integração contínua com GitHub Actions

O workflow [CI - Testes do CRUD](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/workflows/ci.yml), definido em `.github/workflows/ci.yml`, executa a suíte completa:

- A cada push na branch `master`.
- Em pull requests destinados à `master`.
- Manualmente pela aba **Actions → CI - Testes do CRUD → Run workflow**.

O job **Testes do CRUD** utiliza Ubuntu, Java 21 Temurin, cache de dependências Maven e Docker para Testcontainers. Ele compila, executa os testes convencionais e os testes de integração com PostgreSQL temporário usando:

```bash
bash ./mvnw -B -ntp -Pintegration verify
```

No Windows (PowerShell), o comando equivalente é:

```powershell
.\mvnw.cmd -B -ntp -Pintegration verify
```

Não é necessário iniciar o PostgreSQL com Docker Compose para essa suíte. Uma falha de compilação, de teste ou de infraestrutura deixa o job com status de falha.

Os relatórios Surefire e Failsafe produzidos são enviados como o artefato **relatorios-testes-crud**, mesmo quando os testes falham, e ficam disponíveis por **14 dias**. Se a execução falhar antes de gerar relatórios, consulte os logs das etapas. Para conferir o resultado, abra a execução na aba **Actions** e baixe o artefato.

Em 24/10/2026, a suíte sem Docker passou com Java 21. No projeto local do usuário, o commit `84d51ab` foi validado com `mvnw.cmd -Pintegration verify`: **70 testes convencionais e 15 de integração PostgreSQL, totalizando 85 testes, sem falhas, erros ou testes ignorados**. Resultado conferido nos relatórios Surefire e Failsafe gerados às 12:59. O commit anterior `76d1c07` registrava 52 testes. A validação local não substitui o CI: consulte a execução correspondente ao commit na aba Actions.

---

# ✅ Validação e tratamento de exceções

A API utiliza **Bean Validation** em POST e PUT. `name` é obrigatório (`@NotBlank`) e limitado por `@Size(max = 255)`. O limite usa `String.length()` (unidades UTF-16); caracteres suplementares, como alguns emojis, contam duas unidades. O schema OpenAPI informa `maxLength: 255`; para esses caracteres, o limite efetivo Java pode ser mais restritivo. O nome recebido não é truncado nem normalizado.

| Entrada | Resposta |
|---|---|
| Nome ausente, nulo, vazio ou só espaços | 400 — `O nome é obrigatório` |
| Nome acima de 255 unidades UTF-16 | 400 — `O nome deve ter no máximo 255 caracteres` |
| JSON malformado, tipo incompatível ou corpo obrigatório ausente | 400 — `Corpo da requisição ausente ou JSON inválido` |
| ID inexistente em GET, PUT ou DELETE | 404 — `Usuário não encontrado: <id>` |

E-mail também é obrigatório e validado em POST/PUT; duplicidade retorna 409. Consulte o [guia de e-mail](docs/user-email.md).

As respostas de erro tratadas usam `ErrorResponseDTO`, preservando os campos existentes e timestamp como texto ISO de data/hora local, sem fuso:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "O nome deve ter no máximo 255 caracteres",
  "timestamp": "2026-10-24T12:00:00"
}
```

Para testar corpo ausente, envie POST `/users` sem corpo; para JSON malformado, envie `{"name":`. Para limite, envie `{"name":"...","email":"teste@example.com"}` com 255 letras no nome (aceito) ou 256 letras (400). PUT usa as mesmas regras; utilize um ID existente. Para 404, consulte, atualize com corpo válido ou exclua um ID inexistente. Os exemplos de erro não contêm detalhes de SQL nem mensagens internas do parser.

Se houver múltiplas violações, a seleção é estável: campo, código da constraint e mensagem em ordem lexical (`NotBlank` antes de `Size` para `name`). Validação do corpo ocorre antes da chamada ao serviço; o tratamento de falhas inesperadas não foi convertido em 400. A listagem paginada mantém sua estrutura; a V1 permanece intacta. A V4 acrescenta e-mail à tabela users.

Os testes PostgreSQL verificam que POST inválido não insere, PUT inválido preserva o nome e o limite de 255 é persistido corretamente. Esses testes usam somente o banco temporário do Testcontainers.

Também possui tratamento global de exceções através de um `GlobalExceptionHandler`.

Quando um usuário não é encontrado, é lançada uma exceção personalizada:

```text
UserNotFoundException
```

A aplicação então retorna uma resposta HTTP adequada ao cenário.

---

# 📦 DTOs

A API utiliza DTOs para separar os dados recebidos e enviados pela aplicação.

### UserRequestDTO

Utilizado para criação e atualização:

```json
{
  "name": "Jefferson Andrade",
  "email": "jefferson@example.com"
}
```

### UserResponseDTO

Utilizado nas respostas:

```json
{
  "id": 1,
  "name": "Jefferson Andrade",
  "email": "jefferson@example.com",
  "createdAt": "2026-10-24T18:00:00Z",
  "updatedAt": "2026-10-24T18:00:00Z"
}
```

Essa abordagem evita expor diretamente a entidade JPA através da API.

---

# 📁 Estrutura do projeto

```text
CRUD-de-Usuarios-API-REST
│
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com.example.crud
│   │   │       ├── controller
│   │   │       ├── dto
│   │   │       ├── exception
│   │   │       ├── model
│   │   │       ├── repository
│   │   │       └── service
│   │   │
│   │   └── resources
│   │       ├── application.properties
│   │       └── db/migration/V1__create_users_table.sql
│   │
│   └── test
│       ├── java
│       │   └── com.example.crud
│       │       ├── CrudApplicationTests.java
│       │       ├── UserControllerTest.java
│       │       ├── UserServiceTest.java
│       │       └── UserPostgresIT.java
│       │
│       └── resources
│           └── application.properties
│
├── docker-compose.yaml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

# ▶️ Como executar

## Pré-requisitos

- Docker Desktop com containers Linux e Docker Compose v2
- Git
- Java 21 somente para executar Maven no host

## 1. Clone o repositório

```bash
git clone https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST.git
```

Entre na pasta:

```bash
cd CRUD-de-Usuarios-API-REST
```

## 2. Configure e inicie os serviços

Copie `.env.example` para `.env` e preencha `APP_ADMIN_USERNAME` e
`APP_ADMIN_PASSWORD`. Não existe senha padrão da API.

```powershell
docker compose up --build -d
```

Se o volume já possui dados, confira as credenciais e siga **Adoção em um banco existente**
quando ainda não houver histórico Flyway.

## 3. Acesse o Swagger

http://localhost:8080/swagger-ui/index.html

Siga o [guia de autenticação](docs/authentication.md). Para iniciar somente o banco
e executar Maven no host, consulte o [guia Docker](docs/docker.md).

---

# 💡 Boas práticas aplicadas

- Arquitetura em camadas
- Separação de responsabilidades
- DTOs para entrada e saída
- Injeção de dependências por construtor
- Bean Validation
- Tratamento global de exceções
- Exceção personalizada
- Status HTTP adequados
- Spring Data JPA
- PostgreSQL
- Docker Compose
- Testes unitários
- Testes Web com MockMvc
- H2 para o teste de contexto
- Testes de integração com PostgreSQL real
- Testcontainers com banco temporário e isolado
- Ciclo de vida do banco gerenciado pelo Spring Boot
- OpenAPI / Swagger
- Maven
- Git / GitHub

---

# 🎯 Objetivo

Projeto desenvolvido como parte do meu processo de aprendizado em **Java Backend**, com foco na construção de APIs REST utilizando **Java, Spring Boot, Spring Data JPA e PostgreSQL**.

O projeto também foi utilizado para colocar em prática conceitos de arquitetura em camadas, DTOs, validação, tratamento de exceções, testes automatizados, Docker e documentação de APIs.

---

# 👨‍💻 Autor

**Jefferson Andrade**

Estudante de Análise e Desenvolvimento de Sistemas na **UNINTER**, com foco em **Java, Spring Boot e desenvolvimento Backend**.

---

⭐ Se este projeto foi útil ou interessante, fique à vontade para explorar o código e acompanhar minha evolução em Java Backend.
