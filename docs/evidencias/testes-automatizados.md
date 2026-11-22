# Evidência — Testes automatizados (JUnit 5 + MockMvc)

**Execução:** `./mvnw test` na máquina do aluno (Windows, Java 17 Temurin 17.0.17), em 22/11/2026.
**Fonte:** relatórios do Maven Surefire gerados em `target/surefire-reports` (cópias dos resumos em `docs/evidencias/junit/`).

**Resultado:** 53 testes executados — 53 aprovados, 0 falhas, 0 ignorados. Tempo total das classes: 16.7 s.

Os testes sobem a aplicação completa (Spring Boot + banco H2 em memória + segurança) e fazem requisições HTTP simuladas, verificando o código de status e as regras de negócio. Os identificadores CT-xxx correspondem ao plano de testes (docs/05-plano-de-testes.md).

## AutenticacaoTest (9 testes)

| Caso de teste | Resultado | Tempo (s) |
|---|---|---|
| CT-AUT-01: login válido de usuário interno devolve token e perfil | ✅ Passou | 0.071 |
| CT-AUT-02: senha incorreta devolve 401 sem token | ✅ Passou | 0.071 |
| CT-AUT-03: e-mail em formato inválido devolve 400 | ✅ Passou | 0.035 |
| CT-AUT-04: login de cliente devolve papel CLIENTE | ✅ Passou | 0.096 |
| CT-AUT-05: rota protegida sem token devolve 401 | ✅ Passou | 0.015 |
| CT-AUT-06: token adulterado devolve 401 | ✅ Passou | 0.5 |
| CT-AUT-07: perfil sem permissão (atendente criando unidade) devolve 403 | ✅ Passou | 0.088 |
| CT-AUT-08: administrador da matriz cria unidade (201) | ✅ Passou | 0.439 |
| CT-AUT-09: rotas públicas (unidades e health check) não exigem token | ✅ Passou | 0.047 |

## CardapioEstoqueTest (7 testes)

| Caso de teste | Resultado | Tempo (s) |
|---|---|---|
| CT-CAR-01: produto fora da vigência sazonal não aparece no cardápio | ✅ Passou | 0.017 |
| CT-CAR-02: unidade reduzida não oferece produto que exige cozinha completa e usa preço local | ✅ Passou | 0.15 |
| CT-CAR-03: gerente de outra unidade não altera o cardápio (403) | ✅ Passou | 0.093 |
| CT-CAR-04: alteração de preço local pelo gerente é auditada | ✅ Passou | 0.087 |
| CT-EST-01: ajuste de estoque sem motivo é rejeitado (400) | ✅ Passou | 0.077 |
| CT-EST-02: ajuste com motivo altera o saldo e gera auditoria | ✅ Passou | 0.141 |
| CT-EST-03: atendente não consulta estoque (403) | ✅ Passou | 0.073 |

## PedidoTest (14 testes)

| Caso de teste | Resultado | Tempo (s) |
|---|---|---|
| CT-PED-01: totem anônimo cria pedido, calcula total e reserva estoque | ✅ Passou | 0.022 |
| CT-PED-02: estoque insuficiente devolve 422 e nada é gravado | ✅ Passou | 0.013 |
| CT-PED-03: produto fora da vigência não pode ser pedido (422) | ✅ Passou | 0.012 |
| CT-PED-04: pedido pelo APP sem cliente autenticado é rejeitado (422) | ✅ Passou | 0.01 |
| CT-PED-05: cliente autenticado cria pedido pelo APP e consulta o próprio pedido | ✅ Passou | 0.084 |
| CT-PED-06: dados inválidos (lista de itens vazia) devolvem 400 | ✅ Passou | 0.007 |
| CT-PED-07: transição de status inválida devolve 409 | ✅ Passou | 0.081 |
| CT-PED-08: status PAGO não pode ser definido manualmente (422) | ✅ Passou | 0.078 |
| CT-PED-09: atendente cancela pedido não pago; estoque volta e cancelamento é auditado | ✅ Passou | 0.117 |
| CT-PED-10: cancelamento sem motivo é rejeitado (400) | ✅ Passou | 0.076 |
| CT-PED-11: desconto acima de 20% é rejeitado; desconto válido reduz o total e é auditado | ✅ Passou | 0.093 |
| CT-PED-12: atendente não pode aplicar desconto (403) | ✅ Passou | 0.12 |
| CT-PED-13: gerente de outra unidade não vê a fila de Recife (403) | ✅ Passou | 0.077 |
| CT-PED-14: acompanhamento público pelo código do pedido | ✅ Passou | 0.02 |

## PagamentoTest (11 testes)

| Caso de teste | Resultado | Tempo (s) |
|---|---|---|
| CT-PAG-01: cartão aprovado muda o pedido para PAGO e credita 1 ponto por real | ✅ Passou | 0.118 |
| CT-PAG-02: mesma Idempotency-Key repetida não gera nova cobrança (200, mesmo pagamento) | ✅ Passou | 0.104 |
| CT-PAG-03: pagamento recusado (valor terminado em ,97) mantém o pedido aguardando | ✅ Passou | 0.026 |
| CT-PAG-04: serviço de pagamento indisponível (,98) devolve 503 e preserva o pedido | ✅ Passou | 0.1 |
| CT-PAG-05: demora acima do tempo limite (,99) devolve 503 sem travar a API | ✅ Passou | 2.033 |
| CT-PAG-06: Pix fica pendente e é confirmado pelo webhook; webhook repetido não altera nada | ✅ Passou | 0.052 |
| CT-PAG-07: pedido já pago não aceita novo pagamento (409) | ✅ Passou | 0.033 |
| CT-PAG-08: sem Idempotency-Key a solicitação é rejeitada (400) | ✅ Passou | 0.022 |
| CT-PAG-09: gerente cancela pedido pago; estorno solicitado e pontos retirados; atendente não pode (403) | ✅ Passou | 0.245 |
| CT-PAG-10: pedido sem pagamento expira em 15 minutos e devolve o estoque (RN04) | ✅ Passou | 0.068 |
| CT-PAG-11: resgate de 100 pontos dá R$ 5,00 de desconto; resgate fora de bloco é rejeitado (422) | ✅ Passou | 0.091 |

## ClienteLgpdTest (6 testes)

| Caso de teste | Resultado | Tempo (s) |
|---|---|---|
| CT-CLI-01: cadastro sem declaração de maioridade é rejeitado (400) | ✅ Passou | 0.026 |
| CT-CLI-02: data de nascimento de menor de 18 anos é rejeitada (422) | ✅ Passou | 0.007 |
| CT-CLI-03: cadastro válido (201) e e-mail repetido (409); senha não é devolvida | ✅ Passou | 0.075 |
| CT-CLI-04: revogação do consentimento é auditada e interrompe o acúmulo de pontos | ✅ Passou | 0.241 |
| CT-CLI-05: titular acessa os próprios dados (acesso auditado); equipe não acessa (403) | ✅ Passou | 0.2 |
| CT-CLI-06: eliminação anonimiza os dados (204) e impede novo login (401) | ✅ Passou | 0.16 |

## RelatorioAuditoriaTest (5 testes)

| Caso de teste | Resultado | Tempo (s) |
|---|---|---|
| CT-AUD-01: matriz consulta a auditoria filtrando por ação; atendente não acessa (403) | ✅ Passou | 0.243 |
| CT-REL-01: matriz consulta vendas por unidade | ✅ Passou | 0.176 |
| CT-REL-02: matriz consulta vendas por região | ✅ Passou | 0.17 |
| CT-REL-03: gerente só vê a própria unidade; pedir outra unidade devolve 403 | ✅ Passou | 0.206 |
| CT-REL-04: ranking de produtos mais vendidos | ✅ Passou | 0.17 |

## RaizesDoNordesteApiApplicationTests (1 testes)

| Caso de teste | Resultado | Tempo (s) |
|---|---|---|
| Contexto da aplicação sobe corretamente | ✅ Passou | 0.003 |
