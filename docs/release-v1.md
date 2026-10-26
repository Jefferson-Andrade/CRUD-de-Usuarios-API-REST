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

A master anterior a esta entrega, commit cbc03aa, teve CI e Docker aprovados.
A publicação e preservação dos três registros de demonstração foram verificadas
em 26/10/2026, conforme [evidências](demo-online.md).
Essas evidências históricas não substituem a validação da nova versão.

## Operação e recuperação

Configure APP_JWT_SECRET também no Render antes do deploy. Nunca altere as migrações
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
