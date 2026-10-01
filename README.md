# CRUD de Usuários — API REST

API REST desenvolvida com **Java e Spring Boot** para gerenciamento de usuários, utilizando **Spring Data JPA** e **PostgreSQL** como banco de dados.

O projeto foi desenvolvido com foco na prática de conceitos fundamentais de desenvolvimento Backend, como criação de APIs REST, operações CRUD, persistência de dados, arquitetura em camadas e integração com banco de dados relacional.

## Tecnologias

* Java 21
* Spring Boot 4.1.1
* Spring Web MVC
* Spring Data JPA
* PostgreSQL
* Maven
* Docker
* Docker Compose
* Git/GitHub

## Funcionalidades

A API disponibiliza operações para gerenciamento de usuários:

* Criar usuário
* Consultar usuário por ID
* Listar usuários
* Atualizar usuário
* Excluir usuário

## Arquitetura

O projeto utiliza uma organização em camadas:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL
```

### Estrutura principal

```text
src
├── main
│   ├── java
│   │   └── com.example.crud
│   │       ├── controller
│   │       │   └── UserController.java
│   │       ├── model
│   │       │   └── User.java
│   │       ├── repository
│   │       │   └── UserRepository.java
│   │       ├── service
│   │       │   └── UserService.java
│   │       └── CrudApplication.java
│   │
│   └── resources
│       └── application.properties
│
└── test
    └── java
        └── com.example.crud
            └── CrudApplicationTests.java
```

## Modelo de dados

A entidade `User` possui os seguintes atributos:

| Campo  | Tipo   | Descrição           |
| ------ | ------ | ------------------- |
| `id`   | Long   | Identificador único |
| `name` | String | Nome do usuário     |

## Endpoints

### Criar usuário

**POST** `/user`

Exemplo de requisição:

```json
{
  "name": "Jefferson Andrade"
}
```

Resposta:

```json
{
  "id": 1,
  "name": "Jefferson Andrade"
}
```

---

### Buscar usuário por ID

**GET** `/user/{id}`

Exemplo:

```text
GET /user/1
```

Resposta:

```json
{
  "id": 1,
  "name": "Jefferson Andrade"
}
```

---

### Listar usuários

**GET** `/user`

Resposta:

```json
[
  {
    "id": 1,
    "name": "Jefferson Andrade"
  },
  {
    "id": 2,
    "name": "Maria Silva"
  }
]
```

---

### Atualizar usuário

**PUT** `/user`

Exemplo:

```json
{
  "id": 1,
  "name": "Jefferson Andrade Atualizado"
}
```

---

### Excluir usuário

**DELETE** `/user/{id}`

Exemplo:

```text
DELETE /user/1
```

Resposta:

```text
204 No Content
```

## Como executar o projeto

### Pré-requisitos

Antes de executar a aplicação, tenha instalado:

* Java 21
* Maven
* Docker e Docker Compose
* Git

### 1. Clonar o projeto

```bash
git clone <URL_DO_REPOSITORIO>
```

Acesse a pasta:

```bash
cd CRUD-de-Usuarios-API-REST
```

### 2. Iniciar o PostgreSQL

Execute:

```bash
docker compose up -d
```

O Docker Compose iniciará um container PostgreSQL utilizando:

```text
Database: crud
Username: postgres
Password: postgres
Port: 5432
```

### 3. Executar a aplicação

No Windows:

```bash
mvnw.cmd spring-boot:run
```

Ou, caso o Maven esteja instalado:

```bash
mvn spring-boot:run
```

A aplicação será executada localmente na porta padrão:

```text
http://localhost:8080
```

## Executando os testes

Para executar os testes:

```bash
mvnw.cmd test
```

O projeto atualmente possui teste de carregamento do contexto da aplicação com Spring Boot.

## Docker

Para iniciar o banco de dados:

```bash
docker compose up -d
```

Para verificar os containers:

```bash
docker compose ps
```

Para interromper os containers:

```bash
docker compose down
```

## Objetivo do projeto

Este projeto faz parte do meu processo de desenvolvimento de competências em **Java Backend**, colocando em prática conceitos de:

* Desenvolvimento de APIs REST
* Spring Boot
* Programação orientada a objetos
* Persistência de dados com JPA
* Integração com PostgreSQL
* Arquitetura em camadas
* Maven
* Docker
* Git e GitHub

## Próximas evoluções

Como evolução do projeto, pretendo implementar:

* Bean Validation
* Tratamento global de exceções
* DTOs
* Padronização das respostas da API
* Documentação com Swagger/OpenAPI
* Testes unitários e de integração
* Variáveis de ambiente para configuração do banco
* Pipeline de CI/CD

## Autor

**Jefferson Andrade**

Estudante de Análise e Desenvolvimento de Sistemas com foco em **Java, Spring Boot e desenvolvimento Backend**.
