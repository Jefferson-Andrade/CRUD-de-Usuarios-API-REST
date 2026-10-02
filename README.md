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
| 📖 **OpenAPI / Swagger** | Documentação e testes da API |
| 🧪 **JUnit 5** | Testes automatizados |
| 🎭 **Mockito** | Testes unitários |
| 🗄️ **H2** | Banco em memória para testes |
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

O projeto possui testes automatizados utilizando:

- **JUnit 5**
- **Mockito**
- **MockMvc**
- **Spring Boot Test**
- **H2**

### Cenários testados

- Criação de usuário
- Busca de usuário
- Listagem de usuários
- Atualização de usuário
- Exclusão de usuário
- Usuário não encontrado
- Validação dos dados
- Respostas HTTP
- Cenários de sucesso e erro

### Executar os testes

Windows:

```bash
mvnw.cmd test
```

Ou:

```bash
mvn test
```

Os testes utilizam **H2 em memória**, não sendo necessário iniciar o PostgreSQL ou Docker para executá-los.

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
│       │
│       └── resources
│           └── application.properties
│
├── docker-compose.yml
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
mvnw.cmd spring-boot:run
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
- H2 para testes
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
