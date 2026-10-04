# 🚀 CRUD de Usuários — API REST

> API REST para gerenciamento de usuários, desenvolvida com **Java 21** e **Spring Boot 4.1.1**, aplicando conceitos de desenvolvimento backend, arquitetura em camadas, DTOs, validação, persistência de dados e testes automatizados.

---

## 🛠️ Tecnologias

| Tecnologia | Utilização |
|---|---|
| ☕ **Java 21** | Linguagem principal |
| 🌱 **Spring Boot 4.1.1** | Framework backend |
| 🌐 **Spring Web MVC** | Desenvolvimento da API REST |
| 🗃️ **Spring Data JPA** | Persistência de dados |
| 🐘 **PostgreSQL 16** | Banco de dados |
| 🐳 **Docker / Docker Compose** | Containerização do banco |
| 📖 **OpenAPI / Swagger** | Documentação interativa e requisições manuais |
| 🧪 **JUnit 5** | Testes automatizados |
| 🎭 **Mockito** | Isolamento das dependências nos testes |
| 🗄️ **H2** | Banco em memória para o teste de contexto |
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
- ✅ Validação de nome obrigatório na criação e atualização
- ✅ Tratamento centralizado de usuário inexistente e falhas de validação
- ✅ Persistência com PostgreSQL
- ✅ Documentação com Swagger / OpenAPI
- ✅ Testes automatizados

O projeto foi desenvolvido para praticar a construção de uma API REST com responsabilidades separadas, contratos de entrada e saída definidos por DTOs e testes de service e controller.

---

## 🏗️ Arquitetura

A aplicação utiliza uma arquitetura em camadas:

```text
                    ┌──────────────────┐
                    │   HTTP Request   │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │   Controller     │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │     Service      │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │    Repository    │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │    PostgreSQL    │
                    └──────────────────┘
```

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

| Método | Endpoint | Descrição | Sucesso | Erros tratados |
|---|---|---|---|---|
| POST | `/users` | Criar usuário | `201 Created` | `400 Bad Request` por validação |
| GET | `/users` | Listar usuários | `200 OK` | - |
| GET | `/users/{id}` | Buscar usuário por ID | `200 OK` | `404 Not Found` |
| PUT | `/users/{id}` | Atualizar usuário existente | `200 OK` | `400 Bad Request`, `404 Not Found` |
| DELETE | `/users/{id}` | Excluir usuário | `204 No Content` | `404 Not Found` |

O PUT recebe o ID no caminho e o novo nome no corpo. Se o usuário não existir, retorna 404; não cria um novo registro. O DELETE de um ID inexistente também retorna 404.

A listagem retorna todos os usuários, sem paginação ou ordenação explícita. Quando não há registros, retorna uma lista vazia (`[]`).

### Modelo de dados

A entidade `User` é persistida na tabela `users`. O ID é gerado pelo banco com a estratégia `IDENTITY`.

| Campo | Tipo Java | Uso |
|---|---|---|
| `id` | `Long` | Identificador gerado; retornado no DTO de resposta |
| `name` | `String` | Nome recebido na criação/atualização e retornado na resposta |

---

## 📥 Exemplos de requisições

### Criar usuário

**POST `/users`**

```json
{
  "name": "Jefferson Andrade"
}
```

### Resposta

```json
{
  "id": 1,
  "name": "Jefferson Andrade"
}
```

---

### Buscar usuário

**GET `/users/1`**

```json
{
  "id": 1,
  "name": "Jefferson Andrade"
}
```

---

### Atualizar usuário

**PUT `/users/1`**

```json
{
  "name": "Jefferson Andrade Atualizado"
}
```

### Resposta

```json
{
  "id": 1,
  "name": "Jefferson Andrade Atualizado"
}
```

---

### Listar usuários

**GET `/users`**

```json
[
  {
    "id": 1,
    "name": "Jefferson Andrade"
  }
]
```

---

### Excluir usuário

**DELETE `/users/1`**

```text
204 No Content
```

---

## 📸 Demonstração

### Swagger / OpenAPI

Documentação interativa da API através do Swagger UI.

![Swagger da API](https://github.com/user-attachments/assets/72f5de4c-5b12-4935-9441-ba753a13e739)

---

### Criando um usuário

Exemplo de execução do endpoint `POST /users`.

![POST /users](https://github.com/user-attachments/assets/27e9ef95-168d-46c7-903d-18b8b23e29ed)

---

### Excluindo um usuário

Exemplo de execução do endpoint `DELETE /users/{id}`.

![DELETE /users/{id}](https://github.com/user-attachments/assets/49dc4fd8-615f-414b-b00d-4f6cce461a18)

---

## 📖 Documentação da API

Com a aplicação em execução:

### Swagger UI

A entrada configurada no projeto é:

```text
http://localhost:8080/swagger-ui.html
```

A interface também pode ser acessada em `http://localhost:8080/swagger-ui/index.html`.

### OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

O Swagger permite visualizar os endpoints e modelos de dados e executar requisições manualmente pela interface web. Essas requisições não substituem os testes automatizados.

A configuração OpenAPI define título, versão e descrição. Os endpoints são descobertos pelo springdoc; o controller ainda não possui anotações específicas detalhando operações e respostas de erro.

---

## 🗄️ Banco de dados

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

As credenciais acima são padrões locais de desenvolvimento. A aplicação se conecta ao banco em `localhost:5432`; o Compose executa apenas o PostgreSQL, enquanto a API é iniciada separadamente pelo Maven.

O Hibernate atualiza a estrutura da tabela através da configuração:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Essa configuração facilita o desenvolvimento local. O projeto ainda não possui migrações versionadas de banco.

---

## 🐳 Docker

O arquivo `docker-compose.yaml` executa a imagem `postgres:16-alpine`, publica a porta 5432 e utiliza o volume nomeado `postgres_data` para persistir os dados.

### Iniciar o banco

```bash
docker compose up -d
```

### Verificar o container

```bash
docker compose ps
```

### Parar o banco

```bash
docker compose down
```

O comando remove o container e a rede do Compose, mas preserva o volume `postgres_data`. Os dados permanecem disponíveis na próxima inicialização.

---

## 🧪 Testes automatizados

A suíte utiliza JUnit 5, Mockito, MockMvc e Spring Boot Test. Existem **13 métodos de teste**, distribuídos assim:

| Classe | Quantidade | Escopo |
|---|---:|---|
| [UserServiceTest](src/test/java/com/example/crud/UserServiceTest.java) | 6 | Service com repository mockado: criar, buscar, listar, atualizar, excluir e lançar exceção na busca de usuário inexistente |
| [UserControllerTest](src/test/java/com/example/crud/UserControllerTest.java) | 6 | Camada web com MockMvc e service mockado: os cinco endpoints em sucesso e POST com nome vazio retornando 400 |
| [CrudApplicationTests](src/test/java/com/example/crud/CrudApplicationTests.java) | 1 | Carregamento do contexto Spring Boot, com H2 em memória configurado em `src/test/resources/application.properties` |

Os testes de service e controller usam mocks e não validam persistência real. O teste de contexto utiliza H2, mas não executa um fluxo completo de CRUD. Não é necessário iniciar PostgreSQL ou Docker para essa suíte.

Ainda não existem testes de integração de CRUD com PostgreSQL, testes do HTTP 404 ou verificações do corpo personalizado dos erros. A validação no PUT e nomes nulos ou compostos apenas de espaços também não possuem testes específicos. Não há relatório de cobertura configurado no projeto.

### Executar os testes

Windows (PowerShell):

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

Se necessário, conceda permissão de execução ao wrapper: `chmod +x mvnw`.

Com Maven instalado, também é possível usar `mvn test`.

---

## ✅ Validação e tratamento de exceções

### Regra de entrada

O [UserRequestDTO](src/main/java/com/example/crud/dto/UserRequestDTO.java) possui `@NotBlank` em `name`. O [controller](src/main/java/com/example/crud/controller/UserController.java) aplica `@Valid` no POST e no PUT.

O nome é obrigatório: valores ausentes, nulos, vazios ou compostos apenas de espaços são rejeitados com **400 Bad Request**. Não há regras de tamanho, unicidade ou normalização do nome.

### Erros personalizados

O [GlobalExceptionHandler](src/main/java/com/example/crud/exception/GlobalExceptionHandler.java) usa `@RestControllerAdvice` para centralizar dois casos:

| Exceção | Status | Comportamento |
|---|---|---|
| `UserNotFoundException` | `404 Not Found` | Usuário inexistente na busca, atualização ou exclusão |
| `MethodArgumentNotValidException` | `400 Bad Request` | Falha de Bean Validation; retorna somente a primeira mensagem de erro de campo |

As respostas desses casos possuem `status`, `error`, `message` e `timestamp`. O timestamp é gerado com `LocalDateTime.now()`, sem offset de fuso horário.

Exemplo ilustrativo de POST ou PUT com `{"name": ""}`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "O nome é obrigatório",
  "timestamp": "2026-10-04T12:00:00"
}
```

Exemplo ilustrativo de usuário inexistente com ID 99:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Usuário não encontrado: 99",
  "timestamp": "2026-10-04T12:00:00"
}
```

O timestamp varia a cada resposta. JSON malformado, IDs com tipo inválido e falhas de banco não possuem tratamento personalizado nesse handler; não há garantia de que seus erros usem o mesmo formato.

---

## 📦 DTOs

A API utiliza DTOs para separar os dados recebidos e enviados pela aplicação.

### UserRequestDTO

Utilizado para criação e atualização:

```json
{
  "name": "Jefferson Andrade"
}
```

### UserResponseDTO

Utilizado nas respostas:

```json
{
  "id": 1,
  "name": "Jefferson Andrade"
}
```

Essa abordagem evita expor diretamente a entidade JPA através da API.

---

## 📁 Estrutura do projeto

```text
CRUD-de-Usuarios-API-REST
├── .mvn/wrapper/maven-wrapper.properties
├── src
│   ├── main
│   │   ├── java/com/example/crud
│   │   │   ├── config/OpenApiConfig.java
│   │   │   ├── controller/UserController.java
│   │   │   ├── dto
│   │   │   │   ├── UserRequestDTO.java
│   │   │   │   └── UserResponseDTO.java
│   │   │   ├── exception
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── UserNotFoundException.java
│   │   │   ├── model/User.java
│   │   │   ├── repository/UserRepository.java
│   │   │   ├── service/UserService.java
│   │   │   └── CrudApplication.java
│   │   └── resources/application.properties
│   └── test
│       ├── java/com/example/crud
│       │   ├── CrudApplicationTests.java
│       │   ├── UserControllerTest.java
│       │   └── UserServiceTest.java
│       └── resources/application.properties
├── docker-compose.yaml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

## ▶️ Como executar

### Pré-requisitos

- JDK 21
- Docker e Docker Compose (por exemplo, via Docker Desktop)
- Git
- Acesso à internet para baixar as dependências na primeira execução

O Maven Wrapper está incluído. Não é necessário instalar Maven para usar `mvnw` ou `mvnw.cmd`; os comandos com `mvn` exigem Maven instalado.

### 1. Clone o repositório

```bash
git clone https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST.git
```

Entre na pasta:

```bash
cd CRUD-de-Usuarios-API-REST
```

### 2. Inicie o PostgreSQL

```bash
docker compose up -d
```

Aguarde o banco estar pronto para aceitar conexões. Se a API falhar por conexão recusada, consulte `docker compose logs db` e tente novamente após a inicialização. As portas 5432 (banco) e 8080 (API) precisam estar disponíveis.

### 3. Execute a aplicação

Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Se necessário: `chmod +x mvnw`.

Alternativa com Maven instalado:

```bash
mvn spring-boot:run
```

A aplicação será executada em:

```text
http://localhost:8080
```

### 4. Acesse o Swagger

```text
http://localhost:8080/swagger-ui.html
```

---

## 💡 Decisões de implementação

- **Camadas:** controller recebe HTTP, service executa o CRUD e converte entidades em DTOs, repository acessa o banco via Spring Data JPA.
- **DTOs:** `UserRequestDTO` define a entrada e `UserResponseDTO` define a saída, evitando retornar a entidade JPA diretamente.
- **Injeção por construtor:** controller e service recebem suas dependências explicitamente, facilitando testes isolados.
- **Validação:** `@NotBlank` no DTO e `@Valid` no controller rejeitam nomes em branco antes de chamar o service.
- **Erros centralizados:** o handler personaliza os retornos 400 por validação e 404 por usuário inexistente.
- **Testes com mocks:** permitem verificar service e camada web separadamente, com H2 reservado ao carregamento do contexto.
- **Ambiente local:** Compose disponibiliza PostgreSQL com volume persistente; OpenAPI oferece documentação interativa.

## Limitações e próximos passos

A API atualmente não possui autenticação/autorização, paginação, filtros ou ordenação explícita. O banco utiliza credenciais locais padrão e `ddl-auto=update`, sem migrações versionadas. Não há pipeline de CI/CD no repositório.

Evoluções propostas:

- Testar HTTP 404, o formato dos erros e os demais casos de validação.
- Adicionar testes de integração com PostgreSQL, por exemplo com Testcontainers.
- Versionar mudanças de esquema com uma ferramenta de migração.
- Documentar configuração por ambiente e remover a dependência dos valores locais padrão.
- Adicionar paginação, autenticação e um pipeline de CI para executar os testes.
- Detalhar respostas de sucesso e erro no OpenAPI.

---

## 🎯 Objetivo

Projeto desenvolvido como parte do meu processo de aprendizado em **Java Backend**, com foco na construção de APIs REST utilizando **Java, Spring Boot, Spring Data JPA e PostgreSQL**.

O projeto também foi utilizado para colocar em prática conceitos de arquitetura em camadas, DTOs, validação, tratamento de exceções, testes automatizados, Docker e documentação de APIs.

---

## 👨‍💻 Autor

**Jefferson Andrade**

Estudante de Análise e Desenvolvimento de Sistemas na **UNINTER**, com foco em **Java, Spring Boot e desenvolvimento Backend**.

---

⭐ Se este projeto foi útil ou interessante, fique à vontade para explorar o código e acompanhar minha evolução em Java Backend.
