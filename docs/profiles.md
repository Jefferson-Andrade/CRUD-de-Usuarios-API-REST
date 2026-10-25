# Perfis de desenvolvimento e producao

## O que muda

| Configuracao | dev (padrao local) | prod |
|---|---|---|
| Conexao JDBC | DB_URL ou localhost:5432/crud | DB_URL obrigatoria |
| Usuario do banco | DB_USERNAME ou postgres | DB_USERNAME obrigatoria |
| Senha do banco | DB_PASSWORD ou postgres | DB_PASSWORD obrigatoria e nao vazia |
| SQL no console | Habilitado | Desabilitado; log SQL e bind em OFF |
| Bootstrap ADMIN | Opcional, sem credenciais padrao | Opcional, sem credenciais padrao |
| Schema | Flyway e Hibernate validate | Flyway e Hibernate validate |

`application.properties` contem as configuracoes comuns.
`application-dev.properties` oferece os padroes locais.
`application-prod.properties` exige configuracao externa e omite detalhes internos
das respostas de erro do servidor. A autenticacao Basic/CSRF permanece em ambos.

Sem perfil explicito, `spring.profiles.default=dev`. Para producao, selecione
`SPRING_PROFILES_ACTIVE=prod` explicitamente. Nao combine dev e prod: a inicializacao
recusa a combinacao para evitar precedencia de configuracoes locais.

## Desenvolvimento

Com Docker Compose, copie `.env.example` para `.env`, preencha o bootstrap
se precisar criar o primeiro ADMIN e execute:

```powershell
docker compose up --build -d
```

O Compose carrega .env e passa `SPRING_PROFILES_ACTIVE` para a API; o padrao e dev.
Para executar Maven no host, inicie somente o banco:

```powershell
docker compose stop api
docker compose up -d db
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Maven e `java -jar` nao carregam .env automaticamente. Configure DB_URL,
DB_USERNAME e DB_PASSWORD na sessao se o banco nao usar os padroes locais.

## Producao

No ambiente de hospedagem, configure pelo painel de variaveis/segredos:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL=jdbc:postgresql://HOST:5432/BANCO` (adicione os parametros TLS exigidos pelo provedor)
- `DB_USERNAME`
- `DB_PASSWORD`

Nenhuma senha real deve entrar no Git ou nos comandos versionados.
Se necessario, informe APP_ADMIN_USERNAME e APP_ADMIN_PASSWORD para o bootstrap
conforme o [guia de autenticacao](authentication.md); remova-os depois da criacao.
Sem bootstrap, contas ja provisionadas continuam funcionando. Nao existe senha padrao da API.

Com Java 21, depois de configurar as variaveis no ambiente:

```powershell
.\mvnw.cmd -DskipTests package
java -jar target/crud-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Antes de criar a conexao, a aplicacao verifica DB_URL, DB_USERNAME e DB_PASSWORD.
Valores ausentes, vazios ou so com espacos interrompem a inicializacao com
`Perfil prod exige variaveis nao vazias: <nomes>`, sem imprimir valores.
As tres variaveis devem ser fornecidas mesmo que outras propriedades
spring.datasource.* sejam configuradas diretamente.

A configuracao e um padrao de operacao, nao impede overrides explicitos do Spring.
Por exemplo, nao reative SQL por argumento de linha de comando em producao.
Flyway permanece habilitado e baseline automatica desabilitada. Nao edite
migracoes aplicadas e siga o procedimento de adocao do README para bancos legados.

O Compose deste repositorio continua local: usa portas em loopback e credenciais
locais de desenvolvimento no servico db. Selecionar prod nele permite testar
o perfil, mas nao o transforma em hospedagem publica. HTTPS, credenciais reais,
acesso de demonstracao e publicacao serao configurados em uma etapa posterior.

## Testes e independencia das credenciais locais

`ProfileConfigurationTest` carrega os arquivos reais de src/main/resources
num contexto sem banco. Verifica dev, overrides externos, prod, cada variavel
ausente/vazia e a combinacao proibida dev+prod. Remove as fontes de propriedades
do host nesse contexto para nao depender das credenciais da maquina.

Os testes existentes continuam com H2 ou PostgreSQL descartavel do Testcontainers.
A configuracao de teste define bootstrap vazio e perfil padrao test.
O workflow Docker executa a mesma verificacao de Basic, CSRF, CRUD, migracoes e
persistencia em uma matriz dev/prod, com credenciais ficticias de um banco temporario.
