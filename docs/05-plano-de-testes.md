# 05 — Plano de Testes e Estratégia de Qualidade

**Projeto:** API Back-end da Rede "Raízes do Nordeste"

---

## 1. Objetivo e escopo

Verificar se a API atende aos requisitos funcionais e não funcionais definidos em `01-requisitos.md`, com atenção especial aos pontos de maior risco do estudo de caso:

- disputa de estoque nos horários de pico;
- falhas do serviço externo de pagamento;
- controle de acesso por perfil e por unidade;
- tratamento de dados pessoais conforme a LGPD.

**Dentro do escopo:** todos os endpoints implementados (seção 6 de `03-arquitetura.md`), as regras de negócio RN01–RN18 e os requisitos não funcionais verificáveis em ambiente local.

**Fora do escopo desta entrega:**
- testes de interface (não há front-end na trilha);
- testes no PostgreSQL de produção;
- testes de infraestrutura (balanceador, réplicas).

Os testes de carga e de segurança automatizada estão **planejados**, com ferramenta e critérios definidos, mas não foram executados (seção 7).

## 2. Estratégia

A estratégia combina três níveis de teste, do mais automatizado ao mais exploratório:

| Nível | Ferramenta | O que verifica | Quando roda |
|---|---|---|---|
| **Testes automatizados de integração** | JUnit 5 + Spring MockMvc | Sobem a aplicação inteira (segurança, regras, banco H2) e fazem requisições HTTP simuladas. Verificam o código de status, o conteúdo da resposta e o estado do banco (estoque, pontos, auditoria). | A cada alteração: `./mvnw test` |
| **Testes de API ponta a ponta** | Postman (Collection Runner) | Fluxo completo com a API real em execução: login de cada perfil, pedido, pagamento, cozinha, entrega, cancelamento, LGPD, relatórios. As requisições são encadeadas (uma usa o resultado da anterior). | Antes de cada entrega |
| **Testes não funcionais** | JUnit (concorrência e desempenho indicativo); JMeter e OWASP ZAP (planejados) | Concorrência, tempo de resposta, tolerância a falhas e segurança | Concorrência a cada build; carga e segurança antes de ir para produção |

**Técnicas de projeto de casos de teste**
- **Particionamento em classes de equivalência:** entradas válidas e inválidas, por exemplo pedido com e sem estoque, ou cadastro com e sem declaração de maioridade.
- **Valores-limite:** desconto exatamente acima de 20%, resgate fora do bloco de 100 pontos, centavos ,97/,98/,99 no simulador de pagamento.
- **Transição de estados:** ciclo do pedido (RN05). Cada transição válida e uma inválida são testadas.
- **Testes baseados em perfil:** cada operação protegida é testada com um perfil permitido e com um negado.

Todo requisito testável tem **pelo menos um caso positivo e um negativo**.

## 3. Ambiente de teste

| Item | Configuração |
|---|---|
| Sistema operacional | Windows (máquina do aluno) |
| Java | OpenJDK 17.0.17 (Temurin) |
| Aplicação | Spring Boot 4.2.0, executada com `./mvnw` |
| Banco | H2 em memória, recriado a cada execução com os dados de demonstração (3 unidades, 11 produtos, 5 usuários internos, 1 cliente) |
| Serviço de pagamento | `GatewayPagamentoSimulado`: o resultado depende dos centavos do valor; tempo limite de 2 s |
| Ferramentas | JUnit 5, MockMvc, AssertJ, Postman |

## 4. Critérios

| Critério | Definição |
|---|---|
| **Entrada** | O projeto compila; a API sobe com os dados de demonstração; o health check responde UP. |
| **Aceite de um caso** | O código HTTP e o conteúdo da resposta são os esperados, **e** o efeito colateral esperado ocorreu (ou não ocorreu): saldo de estoque, pontos, registro de auditoria. |
| **Saída (aprovação da entrega)** | 100% dos testes automatizados aprovados; 100% das verificações da coleção Postman aprovadas; nenhum defeito aberto de severidade alta. |
| **Suspensão** | Falha de compilação ou API que não sobe: os testes param até a correção. |

## 5. Cenários de teste principais

Formato: pré-condição, entrada, passos e resultado esperado. **(+)** indica caso positivo e **(−)** caso negativo. A coluna "Automação" aponta o teste JUnit (CT-xxx) e a requisição Postman (Pxx) que executam o cenário.

| ID | Cenário | Pré-condição | Entrada / passos | Resultado esperado | Automação |
|---|---|---|---|---|---|
| CEN-01 (+) | Pedido no totem sem identificação | Tapioca de queijo disponível em Recife | `POST /pedidos` canal TOTEM, 2 tapiocas, sem token | 201; status AGUARDANDO_PAGAMENTO; total R$ 29,00; estoque reduzido em 2; código de acompanhamento gerado | CT-PED-01, P17 |
| CEN-02 (−) | Pedido acima do estoque | Bolo de macaxeira com saldo 2 | `POST /pedidos` com 3 bolos | 422 "Regra de negócio violada"; **nenhum** dado gravado; saldo continua 2 | CT-PED-02, P18 |
| CEN-03 (−) | Produto sazonal fora de época | Canjica com vigência de junho a julho | Consultar cardápio e pedir canjica em novembro | Canjica ausente do cardápio; pedido devolve 422 | CT-CAR-01, CT-PED-03, P12, P19 |
| CEN-04 (+) | Diferença entre unidades | São Paulo é loja reduzida e tem preço local | `GET /unidades/{SP}/cardapio` | Sem cuscuz e sem café completo (exigem cozinha completa); tapioca de carne de sol a R$ 21,90 | CT-CAR-02, P13 |
| CEN-05 (−) | Último item disputado no pico | Saldo 1 de um produto | 10 pedidos simultâneos do mesmo produto | Exatamente 1 pedido aceito (201); os demais recebem 409 ou 422; saldo final 0, nunca negativo | CT-CON-01 |
| CEN-06 (+) | Pagamento aprovado com pontos | Cliente com consentimento de fidelidade | Pedido pelo app de R$ 18,90; pagamento com cartão | Pagamento APROVADO; pedido PAGO; +18 pontos no saldo | CT-PAG-01, P25 |
| CEN-07 (−) | Requisição de pagamento repetida | Pagamento já processado com a chave K | Reenviar o mesmo pagamento com a mesma `Idempotency-Key` K | 200 com o **mesmo** pagamento; nenhuma nova cobrança; 1 tentativa registrada | CT-PAG-02, P26 |
| CEN-08 (−) | Serviço de pagamento fora do ar | Pedido de R$ 4,98 | Solicitar pagamento | 503 "Serviço indisponível"; pagamento ERRO registrado; pedido continua AGUARDANDO_PAGAMENTO | CT-PAG-04, P40 |
| CEN-09 (−) | Serviço de pagamento lento | Pedido de R$ 3,99 (simulador demora 3 s) | Solicitar pagamento | 503 em cerca de 2 s (tempo limite); a API não fica travada | CT-PAG-05 |
| CEN-10 (+) | Pix confirmado por webhook | Pagamento Pix PENDENTE | Webhook com token correto, repetido duas vezes | 1ª chamada: APROVADO e pedido PAGO. 2ª chamada: mesmo resultado, sem efeito novo. Sem token: 401 | CT-PAG-06, P34–P36 |
| CEN-11 (+) | Ciclo operacional completo | Pedido pago | Cozinha: EM_PREPARO → PRONTO; atendente: ENTREGUE | Cada transição 200; histórico com 5 registros; cozinha não pode marcar ENTREGUE (403) | P28–P33 |
| CEN-12 (−) | Transição inválida | Pedido aguardando pagamento | Cozinha marca PRONTO | 409 "Transição de status inválida" | CT-PED-07 |
| CEN-13 (+) | Cancelamento de pedido pago | Pedido pago pelo app com pontos ganhos | Gerente cancela com motivo | Pedido CANCELADO; estoque devolvido; estorno solicitado; pontos retirados; cancelamento auditado. O atendente recebe 403. | CT-PAG-09, P41–P42 |
| CEN-14 (−) | Desconto acima do limite | Pedido de R$ 18,90 (limite R$ 3,78) | Gerente aplica R$ 5,00 e depois R$ 2,00 | R$ 5,00: 422. R$ 2,00: 200, total R$ 16,90 e auditoria registrada | CT-PED-11, P23 |
| CEN-15 (+) | Resgate de pontos | Cliente com 250 pontos | Pedido de R$ 18,90 com 100 pontos (e outro com 150) | Desconto de R$ 5,00, total R$ 13,90, saldo −100. Com 150 pontos (fora do bloco): 422 | CT-PAG-11, P21 |
| CEN-16 (−) | Acesso fora da unidade | Gerente de São Paulo autenticado | Consultar fila e relatório de Recife | 403 nas duas consultas | CT-PED-13, CT-REL-03, P24, P54 |
| CEN-17 (+) | Revogação de consentimento | Cliente com fidelidade concedida | Revogar FIDELIDADE e depois pagar um pedido | Situação atual = revogado; histórico preservado (3 registros); auditoria registrada; **nenhum** ponto creditado | CT-CLI-04, P46 |
| CEN-18 (+) | Eliminação de dados (LGPD) | Cliente cadastrado | `DELETE /clientes/me` e depois tentar login | 204; dados anonimizados; login devolve 401; anonimização auditada | CT-CLI-06, P49–P50 |
| CEN-19 (−) | Cadastro de menor / sem declaração | — | Cadastro sem `declaraMaioridade` e cadastro com nascimento há 15 anos | 400 e 422, respectivamente | CT-CLI-01, CT-CLI-02, P43 |
| CEN-20 (−) | Token ausente ou adulterado | — | Chamar rota protegida sem token e com token inválido | 401 com corpo Problem Details | CT-AUT-05, CT-AUT-06, P09 |
| CEN-21 (+) | Pedido sem pagamento expira | Pedido aguardando pagamento há mais de 15 min | Executar a rotina de expiração | Pedido CANCELADO; estoque devolvido; cancelamento auditado sem autor (sistema) | CT-PAG-10 |

## 6. Matriz de rastreabilidade

| Requisito | Casos de teste automatizados (JUnit) | Requisições Postman |
|---|---|---|
| RF01 Autenticação | CT-AUT-01 a 06 | P02–P09 |
| RF02 Cadastro com consentimento | CT-CLI-01 a 03 | P43–P44 |
| RF03 Unidades | CT-AUT-08, CT-AUT-09 | P10 |
| RF04 Catálogo global | — (listagem verificada no Postman) | P11 |
| RF05 Cardápio da unidade | CT-CAR-03, CT-CAR-04 | — |
| RF06 Cardápio disponível (RN01) | CT-CAR-01, CT-CAR-02 | P12, P13 |
| RF07 Estoque | CT-EST-01 a 03, CT-PED-01 | P14–P16 |
| RF08 Pedido multicanal | CT-PED-01, 04, 05, 06 | P17, P20, P21 |
| RF09 Acompanhamento | CT-PED-05, CT-PED-14 | P33 |
| RF10 Fila e status | CT-PED-07, 08, 13 | P24, P28–P32 |
| RF11 Pagamento | CT-PAG-01 a 05, 07, 08 | P25–P27, P37–P40 |
| RF12 Webhook | CT-PAG-06 | P34–P36 |
| RF13 Cancelamento | CT-PED-09, 10, CT-PAG-09 | P41, P42 |
| RF14 Desconto | CT-PED-11, 12 | P22, P23 |
| RF15 Acúmulo de pontos | CT-PAG-01, CT-CLI-04 | P36 |
| RF16 Resgate de pontos | CT-PAG-11 | P21 |
| RF17, RF18, RF24 | Apenas modelados (sem implementação) | — |
| RF19 Direitos do titular | CT-CLI-04 a 06 | P46–P50 |
| RF20 Registro de auditoria | CT-CAR-04, CT-EST-02, CT-PED-09, CT-PED-11, CT-CLI-04 a 06 | P15, P41 |
| RF21 Consulta da auditoria | CT-AUD-01 | P55, P56 |
| RF22 Relatório de vendas | CT-REL-01 a 03 | P51, P52, P54 |
| RF23 Mais vendidos | CT-REL-04 | P53 |
| RF25 Usuários e perfis | CT-AUT-01, CT-AUT-07 (dados iniciais) | P02–P06 |
| RNF01 Segurança | CT-AUT-05 a 07, CT-PED-12, 13, CT-REL-03, CT-CLI-05 | P09, P16, P22, P24, P30, P35, P48, P54 |
| RNF02 LGPD | CT-CLI-01 a 06, CT-PAG-11 | P43–P50 |
| RNF03 Auditabilidade | CT-CAR-04, CT-EST-02, CT-PED-09, CT-PED-11, CT-AUD-01 | P55, P56 |
| RNF04 Consistência e concorrência | CT-CON-01, CT-PED-02 | P18 |
| RNF05 Tolerância a falhas do pagamento | CT-PAG-02 a 06 | P26, P38, P40 |
| RNF06 Desempenho | CT-DES-01 (indicativo); teste de carga planejado (7.1) | — |
| RNF07 Disponibilidade | CT-AUT-09 (health check) | P01 |
| RNF09 Manutenibilidade | Toda a suíte (regressão automática) | — |
| RNF10 Documentação OpenAPI | — (evidência: print do Swagger) | — |

## 7. Testes não funcionais

### 7.1 Desempenho em horário de pico (RNF06)

**Executado (indicativo): CT-DES-01**
- 50 acessos simultâneos à consulta de cardápio, 20 requisições cada (1.000 no total).
- Meta: p95 menor que 300 ms.
- **Resultado medido** na máquina do aluno: p50 = 4 ms, **p95 = 189 ms**, máximo = 812 ms. A meta foi atendida.
- O teste roda dentro do processo, sem rede. Ele serve para pegar uma regressão grave de desempenho, por exemplo uma consulta que deixou de usar índice, mas **não substitui** um teste de carga.

**Planejado: teste de carga com Apache JMeter**

| Parâmetro | Valor |
|---|---|
| Cenário | Simulação de horário de almoço: 70% consultas de cardápio, 25% criação de pedido, 5% pagamento |
| Carga | 50 usuários virtuais, subida em 60 s, 10 min de duração; depois um pico de 150 usuários por 2 min |
| Ambiente | Cópia do ambiente de produção (PostgreSQL, 2 instâncias atrás do balanceador) |
| Critérios de aceite | p95 ≤ 300 ms no cardápio e ≤ 800 ms na criação de pedido; taxa de erro < 1% (desconsiderando 422 de estoque esgotado) |
| Métricas observadas | Tempo de resposta (p50, p95, p99), vazão, erros, uso de CPU e de conexões do banco |

### 7.2 Concorrência (RNF04)

**Executado e aprovado: CT-CON-01.** Dez pedidos simultâneos disputam o último item. O controle otimista (campo `version` no estoque), junto com a transação única do pedido, garante que só um é aceito e que o saldo nunca fica negativo.

### 7.3 Tolerância a falhas da integração (RNF05)

**Executado: CT-PAG-02 a 06 e P26, P38, P40.** Os testes cobrem:
- recusa do pagamento;
- indisponibilidade do serviço;
- demora acima do tempo limite;
- repetição da mesma solicitação;
- webhook repetido.

Em todos os casos, o pedido é preservado e não há cobrança duplicada.

### 7.4 Segurança

**Executado (testes funcionais de segurança):**
- acesso sem token e com token adulterado (401);
- perfil sem permissão (403);
- acesso a outra unidade (403);
- webhook sem segredo (401);
- respostas sem senha nem dados pessoais para a equipe;
- senhas armazenadas com BCrypt.

**Planejado: varredura automatizada com OWASP ZAP**, em modo *baseline* e depois *API scan* usando o contrato OpenAPI (`/v3/api-docs`), contra o ambiente de homologação. Critério: nenhum alerta de risco alto. Os alertas médios devem ser analisados e justificados.

### 7.5 LGPD

**Executado: CT-CLI-01 a 06 e P43–P50.** Os testes cobrem:
- consentimento separado por finalidade e nada pré-marcado;
- maioridade;
- revogação com efeito imediato;
- direito de acesso (auditado);
- eliminação por anonimização;
- ausência de dados pessoais nas respostas para perfis internos.

## 8. Resultados e evidências

| Execução | Resultado | Evidência |
|---|---|---|
| Testes automatizados (`./mvnw test`) | 55 testes, 55 aprovados, 0 falhas; CT-DES-01 com p95 = 189 ms | `docs/evidencias/testes-automatizados.md` e `docs/evidencias/junit/` |
| Coleção Postman (56 requisições, 95 verificações) | 95 de 95 aprovadas, 0 erros, 6,2 s | `docs/evidencias/prints/postman-runner.png` |
| Documentação OpenAPI | Swagger UI acessível com todos os endpoints | `docs/evidencias/prints/swagger.png` |

**Defeitos:** nenhum defeito aberto. Durante o desenvolvimento, os testes foram executados ao fim de cada módulo implementado. Todas as rodadas terminaram sem falhas antes do commit.

## 9. Riscos e limitações dos testes

| Risco | Impacto | Mitigação |
|---|---|---|
| Testes executados com H2, não com PostgreSQL | Diferenças de SQL ou de bloqueio podem passar despercebidas | Executar a suíte contra PostgreSQL (por exemplo, com Testcontainers) antes de produção |
| Desempenho medido só dentro do processo | Não reflete rede, balanceador nem banco real | Teste de carga JMeter planejado (7.1) |
| Serviço de pagamento simulado | O comportamento real do provedor pode diferir | Testes de contrato com o ambiente de testes (*sandbox*) do provedor escolhido |
| Testes de interface inexistentes | Integração front-end × API não verificada | Fora do escopo da trilha; o contrato OpenAPI serve de base para os times de front-end |
