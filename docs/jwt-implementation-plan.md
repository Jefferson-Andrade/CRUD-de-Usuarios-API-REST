# Registro da implementação JWT

O JWT foi integrado pelo PR #11, em paralelo ao HTTP Basic.
O [guia atual](jwt-authentication.md) documenta configuração, uso, CSRF e limitações.

A conclusão da versão 1.0 acrescenta limite de tentativas por endereço remoto,
respostas de login sem cache, validação dos limites de entrada e testes de segurança.
Permissões são consultadas no banco em cada requisição; os papéis não são copiados ao JWT.
CSRF permanece obrigatório nas escritas Basic e Bearer.

As migrações V1–V4 foram preservadas. Refresh token, revogação individual e contadores
distribuídos não fazem parte da versão 1.0. Consulte os [critérios de entrega](release-v1.md).
