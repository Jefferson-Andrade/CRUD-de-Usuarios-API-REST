# Migração planejada: HTTP Basic para JWT

> Documento de implementação da branch `feature/jwt-authentication`. O suporte a JWT **ainda não está ativo**.

## Contrato proposto

- `POST /auth/login` recebe `username` e `password` e devolve `accessToken`, `tokenType: Bearer` e `expiresIn`.
- `Authorization: Bearer <token>` autentica chamadas à API.
- Assinatura JWT com chave configurada por variável de ambiente, nunca armazenada no Git.
- Claims mínimas: `sub` (identificador da conta), `iat`, `exp`, `iss` e perfis autorizados.
- Validação obrigatória de assinatura, emissor, validade temporal e conta habilitada, com política explícita para alteração de perfis.
- `READER` continua restrito a leitura; `ADMIN` continua autorizado a escrita.
- Respostas de autenticação: `401` para credenciais/token inválidos e `403` para falta de autorização.
- Não incluir senha, hash, dados pessoais do CRUD ou segredo de assinatura no JWT.

## Etapas de implementação

1. Adicionar dependências de suporte JWT compatíveis com Spring Boot 4.1.1.
2. Configurar emissão/validação de tokens, com validação de configuração e falha segura sem chave.
3. Criar DTOs de login/resposta e endpoint de autenticação com rate limiting ou outra mitigação de força bruta.
4. Integrar autenticação Bearer à `SecurityFilterChain`, mantendo regras de perfis.
5. Definir migração de HTTP Basic e CSRF: não desabilitar CSRF globalmente enquanto clientes Basic/cookies forem suportados.
6. Adicionar testes de login válido/inválido, expiração, assinatura adulterada, conta desabilitada, perfis e CSRF.
7. Atualizar Swagger, exemplos de execução, variáveis de ambiente e README.
8. Rodar `./mvnw -B -ntp -Pintegration verify` e revisar o PR antes de mesclar.

## Compatibilidade e segurança

A aplicação atual usa HTTP Basic e token CSRF em escritas. A migração precisa distinguir requisições autenticadas exclusivamente por Bearer das que usam credenciais automáticas do navegador. Durante a transição, manter os testes atuais passando e não retirar mecanismos de proteção prematuramente.

Não criar credenciais padrão. Não registrar tokens ou senhas nos logs. Não fazer commit de chaves ou arquivos `.env`. Preferir tokens de curta duração, rotação de chaves e procedimento documentado de revogação.

## Critérios de aceite

- Suíte de testes e integração aprovada.
- Sem alterações destrutivas de dados ou migrações antigas.
- Regras de autorização preservadas.
- Swagger e README descrevem exatamente o comportamento implementado.
- Revisão do PR antes do merge na `master`.
