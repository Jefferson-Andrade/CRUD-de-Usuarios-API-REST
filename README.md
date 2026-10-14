# 🚀 CRUD de Usuários — API REST

[![CI - Testes do CRUD](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/workflows/ci.yml)

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
- ✅ Documentação com Swagger / OpenAPI
- ✅ Testes automatizados
- ✅ Testes de integração com PostgreSQL real e Testcontainers

O projeto foi desenvolvido com foco em organização, separação de responsabilidades e aplicação de boas práticas de desenvolvimento backend.

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

| Método | Endpoint | Descrição | Status |
|:---:|---|---|:---:|
| 🟢 **POST** | `/users` | Criar usuário | `201 Created` |
| 🔵 **GET** | `/users` | Listar usuários | `200 OK` |
| 🔵 **GET** | `/users/{id}` | Buscar usuário por ID | `200 OK` |
| 🟡 **PUT** | `/users/{id}` | Atualizar usuário | `200 OK` |
| 🔴 **DELETE** | `/users/{id}` | Excluir usuário | `204 No Content` |

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

O Hibernate gerencia a estrutura da tabela através da configuração:

```properties
spring.jpa.hibernate.ddl-auto=update
```

---

# 🐳 Docker

O PostgreSQL é executado utilizando Docker Compose.

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

---

# 🧪 Testes automatizados

A suíte utiliza **JUnit Jupiter**, **Mockito**, **MockMvc**, **Spring Boot Test**, **H2** e **Testcontainers**. Os testes com mocks verificam comportamentos isolados; os testes de integração percorrem as camadas da aplicação e verificam a persistência em PostgreSQL real.

### Organização da suíte

| Classe | Tipo | O que verifica | Testes |
|---|---|---|:---:|
| `UserServiceTest` | Unitário, com repository mockado | Criação, consulta, listagem, atualização, exclusão e exceção para usuário inexistente | 6 |
| `UserControllerTest` | Web, com service mockado | Os cinco endpoints em sucesso e rejeição de nome vazio no POST com `400` | 6 |
| `CrudApplicationTests` | Contexto, com H2 em memória | Carregamento da aplicação | 1 |
| `UserPostgresIT` | Integração, com PostgreSQL 16 e sem mocks | Criação, consulta, listagem, atualização e exclusão com verificação direta no banco | 3 |
| **Total** | | | **16** |

### Executar os testes sem Docker

Windows (PowerShell):

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

Esses comandos executam **13 testes**: os testes de serviço e de controller com mocks e o teste de contexto com H2. Não exigem PostgreSQL ou Docker.

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

O perfil `integration` executa os 13 testes existentes e os 3 testes `UserPostgresIT`, totalizando **16 testes**. O Maven Failsafe executa as classes `*IT` e verifica o resultado na etapa `verify`.

Não é necessário iniciar o banco com `docker compose up`. O Testcontainers cria um PostgreSQL temporário com a imagem `postgres:16-alpine`, banco `crud_integration` e porta aleatória, separado do banco da aplicação. A primeira execução pode baixar as imagens necessárias. A suíte falha se o Docker não estiver acessível.

### Cenários de integração verificados

- **Criação, consulta e listagem:** POST retorna `201` e um ID gerado; consultas SQL conferem o nome e a quantidade de registros; GET por ID e listagem retornam os dados salvos.
- **Atualização:** PUT retorna `200`, mantém o ID e altera o nome; uma consulta SQL e um novo GET confirmam a alteração persistida, sem criar outro usuário.
- **Exclusão:** DELETE retorna `204` com corpo vazio; uma consulta SQL confirma a remoção; GET posterior retorna `404` com os campos de erro esperados, e a listagem fica vazia.

As requisições usam MockMvc com a aplicação completa, passando por **controller → service → repository → PostgreSQL**. Não há mocks nem uma transação de teste envolvendo as requisições; as consultas SQL verificam as gravações confirmadas no banco.

Os dados são limpos antes de cada teste. O PostgreSQL é declarado como um bean de teste com `@ServiceConnection`, que fornece os dados de conexão ao Spring Boot. Ao final da classe, o Spring encerra a aplicação e as conexões antes de parar o container.

### Resultado validado

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

A suíte possui **16 testes no commit de referência `1a934d2`**. Conforme novos cenários forem adicionados, a quantidade pode crescer; a validação deve confirmar que os testes convencionais e de integração previstos foram executados, sem falhas, erros ou testes ignorados.

---

# ✅ Validação e tratamento de exceções

A API utiliza **Bean Validation** para validar os dados recebidos nas requisições.

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
│   │       └── application.properties
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

- Java 21
- Docker Desktop
- Git

## 1. Clone o repositório

```bash
git clone https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST.git
```

Entre na pasta:

```bash
cd CRUD-de-Usuarios-API-REST
```

## 2. Inicie o PostgreSQL

```bash
docker compose up -d
```

## 3. Execute a aplicação

No Windows:

```bash
.\mvnw.cmd spring-boot:run
```

Ou:

```bash
mvn spring-boot:run
```

A aplicação será executada em:

```text
http://localhost:8080
```

## 4. Acesse o Swagger

```text
http://localhost:8080/swagger-ui/index.html
```

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
