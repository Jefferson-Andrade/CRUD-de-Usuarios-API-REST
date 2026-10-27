# Conclusão da versão 1.0

## Escopo entregue

- CRUD de usuários: criação, consulta paginada/filtrada, consulta por ID, atualização e exclusão.
- DTOs, Bean Validation, e-mail único, auditoria UTC e erros HTTP padronizados.
- PostgreSQL, Flyway V1–V4 preservadas e Hibernate em validate.
- Contas BCrypt com ADMIN/READER, HTTP Basic e JWT HS256 de curta duração.
- CSRF obrigatório nas escritas; limite de requisições em POST /auth/login.
- Swagger/OpenAPI, health, logs de falhas com identificador e perfis dev/prod.
- Docker Compose, CI com Testcontainers e teste de persistência dos containers.
- Demonstração HTTPS em Render/Neon e instruções reproduzíveis de configuração.

## Verificação da entrega

A suíte prevista reúne **200 testes Java**: 174 convencionais e 26 com PostgreSQL,
além de **4 testes Python**. A CI executa a suíte completa; o workflow Docker
verifica os perfis dev/prod e a persistência. Consulte as execuções do commit
correspondente antes de implantar.

A entrega foi integrada à `master` no commit `ee566dc206ad19f58f13ecaf0706b8c20027081f`.
A [CI final](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/runs/40422226402)
e o [workflow Docker dev/prod](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/actions/runs/40422226471)
foram aprovados para esse commit: 174 testes convencionais, 26 PostgreSQL e 4 testes Python,
além da verificação Docker de CRUD e persistência nos dois perfis.

A publicação da v1.0.0 e as consultas HTTPS foram registradas no
[PR #12](https://github.com/Jefferson-Andrade/CRUD-de-Usuarios-API-REST/pull/12).
O [guia de demonstração](demo-online.md) separa essa verificação final das evidências
históricas de preservação dos três registros entre deploys e das tarefas administrativas pendentes.

## Operação e recuperação

Mantenha APP_JWT_SECRET configurado no Render em todos os deploys. Nunca altere as migrações
já aplicadas. Mantenha as credenciais do banco e ADMIN privadas.
O [guia de demonstração](demo-online.md) descreve backup, restauração, bootstrap
e verificação de persistência; o README contém um exemplo local de pg_dump/pg_restore.
Não execute restaurações sobre o banco de demonstração para validar esta entrega.

Após atualizar, confira health, Swagger, login e consultas READER. A manutenção ADMIN
no ambiente hospedado e a definição/validação do backup do banco gerenciado ainda dependem
de acesso administrativo privado; os testes CI usam somente bancos descartáveis.

## Limites conhecidos

Esta é uma API de portfólio, com demonstração somente para leitura.
Não inclui cadastro público de contas, frontend, refresh token, revogação individual,
limite distribuído ou garantia de disponibilidade dos planos gratuitos.
O limite em memória cobre o login JWT; proteja HTTP Basic e o proxy no ingresso.
Esses itens não são requisitos adicionais para concluir o escopo funcional da versão 1.0.
