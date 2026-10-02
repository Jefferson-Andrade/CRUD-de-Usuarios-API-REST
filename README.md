# CRUD de Usuários — API REST

API REST desenvolvida com **Java 21 e Spring Boot 4.1.1** para gerenciamento de usuários.

O projeto aplica conceitos de **REST, arquitetura em camadas, DTOs, Bean Validation, tratamento global de exceções, Spring Data JPA, PostgreSQL, Docker e testes automatizados**.

## Tecnologias

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Bean Validation
- PostgreSQL 16
- Maven
- Docker / Docker Compose
- OpenAPI / Swagger UI
- JUnit 5
- Mockito
- H2 para testes
- Git / GitHub

## Arquitetura

~~~text
HTTP Request
     ↓
Controller
     ↓
Service
     ↓
Repository
     ↓
PostgreSQL
~~~

A API utiliza DTOs para entrada e saída, evitando expor diretamente a entidade JPA.

## Endpoints

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| POST | /users | Criar usuário | 201 Created |
| GET | /users/{id} | Buscar usuário por ID | 200 OK |
| GET | /users | Listar usuários | 200 OK |
| PUT | /users/{id} | Atualizar usuário | 200 OK |
| DELETE | /users/{id} | Excluir usuário | 204 No Content |

### Criar usuário

~~~json
{
  "name": "Jefferson Andrade"
}
~~~

### Atualizar usuário

~~~json
{
  "name": "Jefferson Andrade Atualizado"
}
~~~

## Swagger / OpenAPI 
<img width="1917" height="1078" alt="API Swagger" src="https://github.com/user-attachments/assets/72f5de4c-5b12-4935-9441-ba753a13e739" />

<img width="1917" height="1078" alt="POST" src="https://github.com/user-attachments/assets/27e9ef95-168d-46c7-903d-18b8b23e29ed" />

<img width="1917" height="1078" alt="DELETE" src="https://github.com/user-attachments/assets/49dc4fd8-615f-414b-b00d-4f6cce461a18" />



Com a aplicação em execução, acesse:

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

O Springdoc OpenAPI suporta Spring Boot 4 e disponibiliza Swagger UI automaticamente. 

## Como executar

### Pré-requisitos

- Java 21
- Docker Desktop
- Git

### 1. Clonar o projeto

~~~bash
git clone <URL_DO_REPOSITORIO>
cd CRUD-de-Usuarios-API-REST
~~~

### 2. Subir o PostgreSQL

~~~bash
docker compose up -d
~~~

Configuração:

~~~text
Database: crud
Username: postgres
Password: postgres
Port: 5432
~~~

### 3. Executar a aplicação

Windows:

~~~bash
mvnw.cmd spring-boot:run
~~~

Ou:

~~~bash
mvn spring-boot:run
~~~

A API será executada na porta 8080.

## Testes automatizados

O projeto possui:

- teste de carregamento do contexto;
- testes unitários do Service com Mockito;
- testes da camada Web com MockMvc;
- validação de entrada;
- cenários de sucesso e erro.

MockMvc permite testar a camada Spring MVC sem iniciar um servidor HTTP real.

Para executar:

~~~bash
mvnw.cmd test
~~~

Os testes de contexto utilizam H2 em memória e não dependem do PostgreSQL ou Docker.

## Docker

~~~bash
docker compose up -d
docker compose ps
docker compose down
~~~

## Boas práticas aplicadas

- Arquitetura em camadas
- DTOs para entrada e saída
- Injeção de dependências por construtor
- Bean Validation
- Tratamento global de exceções
- Status HTTP adequados
- Spring Data JPA
- Testes unitários e Web
- H2 para testes
- OpenAPI / Swagger
- PostgreSQL via Docker Compose

## Objetivo

Projeto desenvolvido como parte do meu processo de aprendizado em **Java Backend**, colocando em prática Java, Spring Boot, APIs REST, Spring Data JPA, PostgreSQL, Docker, testes automatizados, Git e GitHub.

## Autor

**Jefferson Andrade**

Estudante de Análise e Desenvolvimento de Sistemas na UNINTER, com foco em **Java, Spring Boot e desenvolvimento Backend**.
