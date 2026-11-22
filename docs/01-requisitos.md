# 01 — Análise e Requisitos

**Projeto:** API Back-end da Rede "Raízes do Nordeste"

---

## 1. Escopo da solução

A solução é uma API REST única que atende todos os canais de venda (aplicativo, totem, balcão e retirada) e a gestão da rede (unidades e matriz). As interfaces (app, totem, telas do balcão e painel da matriz) são consumidoras da API e não fazem parte da entrega do Back-end. A API será usada em conjunto.

**Dentro do escopo**
- Catálogo global da franquia e cardápio por unidade (formato da loja, sazonalidade, preço local).
- Pedidos multicanal, ciclo de status e acompanhamento.
- Estoque local por unidade.
- Integração com serviço externo de pagamento (simulado).
- Programa de fidelidade com consentimento LGPD.
- Auditoria de operações sensíveis e de acessos a dados pessoais.
- Relatórios para a matriz (vendas por unidade/região e produtos mais vendidos).

**Fora do escopo**
- Telas (front-end) tratadas como consumidoras da API.
- Processamento do pagamento em si.
- Entrega em domicílio.
- Emissão de documentos fiscais.
- Operação offline das lojas.

## 2. Atores

| Ator | Descrição |
|---|---|
| Cliente | Consulta cardápio, faz pedidos, acompanha status, participa da fidelidade. Pode ser identificado (cadastro) ou anônimo (totem/balcão). |
| Atendente | Registra pedidos no balcão e entrega pedidos prontos. |
| Cozinha | Visualiza a fila de pedidos pagos e atualiza o status de preparo. |
| Gerente da unidade | Administra cardápio local, preços locais e estoque. Autoriza cancelamentos e descontos. Consulta relatórios da sua unidade. |
| Administrador da matriz | Administra catálogo global e unidades. Consulta relatórios consolidados e trilha de auditoria. |
| Serviço de pagamento (externo) | Processa o pagamento e devolve aprovação ou recusa. |

## 3. Requisitos funcionais

PrioridadeS: **O** = obrigatório, **I** = importante, **D** = desejável.
Previsão: **Implementar** = previsto na API entregue; **Modelar** = projetado e documentado, sem implementação nesta versão.

| ID | Requisito | Ator principal | Prioridade | Previsão | Origem no caso |
|---|---|---|---|---|---|
| RF01 | Autenticar usuários internos e clientes, emitindo token de acesso | Todos | O | Implementar | Seção 3, Seção 4 |
| RF02 | Cadastrar cliente com registro de consentimento por finalidade (fidelidade, marketing) | Cliente | O | Implementar | Seção 4 |
| RF03 | Manter unidades (cidade, UF, região, formato: completa ou reduzida) | Matriz | O | Implementar | Seção 2, Seção 3 |
| RF04 | Manter catálogo global de produtos (nome, categoria, preço base, exigência de cozinha completa) | Matriz | O | Implementar | Seção 2, Seção 3 |
| RF05 | Manter cardápio da unidade: disponibilizar produto, preço local e período de vigência (sazonal) | Gerente | O | Implementar | Seção 2 |
| RF06 | Consultar o cardápio de uma unidade exibindo apenas itens disponíveis (RN01) | Cliente, Atendente | O | Implementar | Seção 2 |
| RF07 | Controlar estoque local: entrada, ajuste manual com motivo e baixa automática por pedido | Gerente | O | Implementar | Seção 3 |
| RF08 | Registrar pedido informando unidade, canal de origem (APP, TOTEM, BALCAO) e tipo de atendimento (CONSUMO_LOCAL, RETIRADA) | Cliente, Atendente | O | Implementar | Seção 2 |
| RF09 | Acompanhar o status do pedido | Cliente | O | Implementar | Seção 2 |
| RF10 | Listar a fila de pedidos da unidade e atualizar o status conforme o ciclo válido (RN05) | Cozinha, Atendente | O | Implementar | Seção 2, Seção 3 |
| RF11 | Solicitar pagamento ao serviço externo e registrar o resultado (aprovado, recusado, erro) | Sistema | O | Implementar (serviço simulado) | Seção 5 |
| RF12 | Receber a confirmação assíncrona do serviço de pagamento (webhook) de forma idempotente | Serviço de pagamento | I | Implementar | Seção 5 |
| RF13 | Cancelar pedido com motivo, devolução de estoque e solicitação de estorno quando pago | Gerente, Atendente | O | Implementar | Seção 3 |
| RF14 | Aplicar desconto manual em pedido, com motivo e limite | Gerente | I | Implementar | Seção 3 |
| RF15 | Acumular pontos em pedidos pagos de clientes identificados e com consentimento | Sistema | O | Implementar | Seção 4 |
| RF16 | Resgatar pontos como desconto no pedido | Cliente | I | Implementar | Seção 4 |
| RF17 | Níveis de fidelidade com desconto progressivo | Sistema | I | Modelar | Seção 4 |
| RF18 | Campanhas segmentadas por frequência, faixa etária e região, apenas para clientes com consentimento de marketing | Matriz | D | Modelar | Seção 4 |
| RF19 | Permitir ao cliente consultar e revogar consentimentos e solicitar a eliminação (anonimização) dos seus dados | Cliente | O | Implementar | Seção 4 |
| RF20 | Registrar auditoria de operações sensíveis (cancelamento, desconto, ajuste de estoque, alteração de preço) e de acessos a dados pessoais | Sistema | O | Implementar | Seção 3, Seção 4 |
| RF21 | Consultar a trilha de auditoria com filtros (período, unidade, tipo de ação) | Matriz | I | Implementar | Seção 3 |
| RF22 | Relatório de vendas por unidade e por região em um período | Matriz, Gerente | I | Implementar | Seção 3 |
| RF23 | Relatório de produtos mais vendidos | Matriz, Gerente | I | Implementar | Seção 3 |
| RF24 | Relatórios financeiros consolidados, metas e indicadores por unidade | Matriz | D | Modelar | Seção 3 |
| RF25 | Manter usuários internos vinculados a uma unidade e a um perfil | Matriz | I | Implementar mínimo (dados iniciais) | Seção 3 |

## 4. Requisitos não funcionais

| ID | Categoria | Requisito | Verificação | Previsão |
|---|---|---|---|---|
| RNF01 | Segurança | Autenticação por token (JWT), senhas armazenadas com hash BCrypt, autorização por perfil e por unidade (o gerente acessa apenas a sua unidade) | Testes de acesso negado (401/403) | Implementar |
| RNF02 | Privacidade (LGPD) | Consentimento explícito e separado por finalidade, com data e versão do termo, minimização de dados (data de nascimento opcional, uso apenas como faixa etária), anonimização na eliminação e nos relatórios, dados pessoais nunca registrados em log | Testes de consentimento, revogação e anonimização | Implementar |
| RNF03 | Auditabilidade | Registros de auditoria imutáveis (somente inserção) com usuário, data/hora, ação, entidade, valores anterior e novo e motivo | Teste de geração do registro em cada operação sensível | Implementar |
| RNF04 | Consistência | Operações de pedido e estoque em transação única, controle de concorrência otimista para impedir venda acima do estoque em acessos simultâneos | Teste de concorrência | Implementar |
| RNF05 | Tolerância a falhas | Chamada ao serviço de pagamento com tempo limite, chave de idempotência e tratamento de indisponibilidade sem derrubar a API, pedido permanece aguardando pagamento até resposta definitiva | Testes com o serviço simulado retornando recusa, erro e demora | Implementar |
| RNF06 | Desempenho | Meta (premissa): p95 ≤ 300 ms na consulta de cardápio e ≤ 800 ms na criação de pedido com 50 usuários simultâneos | Teste de carga planejado (JMeter) | Planejar teste |
| RNF07 | Disponibilidade | Meta (premissa) de 99,5% mensal em produção; API sem estado de sessão, permitindo várias instâncias atrás de balanceador de carga; verificação de saúde (health check) | Endpoint de health check; diagrama de implantação | Implementar health check / Modelar infraestrutura |
| RNF08 | Escalabilidade | Inclusão de novas unidades, regiões e produtos apenas por cadastro, sem alteração de código; consultas indexadas por unidade | Revisão do modelo de dados | Modelar |
| RNF09 | Manutenibilidade | Arquitetura em camadas (controller, service, repository), DTOs separados das entidades, validação de entrada e formato único de erro | Revisão de código; testes automatizados das regras | Implementar |
| RNF10 | Interoperabilidade | API REST com JSON, versionada (`/api/v1`), documentada em OpenAPI (Swagger) | Swagger acessível | Implementar |
| RNF11 | Padronização da franquia | Catálogo e receitas base definidos pela matriz; unidades apenas disponibilizam itens e ajustam preço local | Regras de autorização | Implementar |

## 5. Regras de negócio

| ID | Regra |
|---|---|
| RN01 | Um produto aparece no cardápio da unidade somente se: estiver ativo no catálogo, estiver disponibilizado pela unidade, a data atual estiver dentro do período de vigência (quando houver), a unidade atender à exigência de cozinha completa (quando houver) e houver estoque disponível. |
| RN02 | O preço de venda é o preço local da unidade ou, na ausência dele, o preço base do catálogo. O preço é gravado no item do pedido no momento da compra. |
| RN03 | O estoque é reservado (baixado) na criação do pedido e devolvido em caso de cancelamento ou recusa do pagamento. |
| RN04 | Pedido sem confirmação de pagamento em 15 minutos expira e é cancelado automaticamente, devolvendo o estoque. |
| RN05 | Ciclo de status: AGUARDANDO_PAGAMENTO → PAGO → EM_PREPARO → PRONTO → ENTREGUE. CANCELADO é permitido a partir de AGUARDANDO_PAGAMENTO, PAGO ou EM_PREPARO. Qualquer outra transição é rejeitada. |
| RN06 | Cancelamento exige motivo. Pedidos já pagos ou em preparo só podem ser cancelados pelo gerente e geram solicitação de estorno. Todo cancelamento é auditado. |
| RN07 | Desconto manual só pode ser aplicado pelo gerente, com motivo obrigatório e limite de 20% do valor do pedido. Todo desconto é auditado. |
| RN08 | Cliente identificado e com consentimento de fidelidade acumula 1 ponto por R$ 1,00 pago (valor inteiro). Os pontos são creditados quando o pedido é pago e estornados se o pedido for cancelado. |
| RN09 | Resgate: cada 100 pontos valem R$ 5,00 de desconto, limitado a 50% do valor do pedido. |
| RN10 | Participação na fidelidade exige consentimento da finalidade "fidelidade", campanhas segmentadas exigem consentimento separado da finalidade "marketing". A revogação interrompe o uso imediatamente. |
| RN11 | O cadastro no programa exige declaração de maioridade (18 anos). Cadastro de menores, que dependeria de consentimento do responsável, fica fora do escopo. |
| RN12 | A data de nascimento é opcional e, quando informada, é usada apenas como faixa etária em segmentações e relatórios. |
| RN13 | Pedido sem cliente identificado (totem ou balcão anônimo) não acumula pontos. |
| RN14 | O processamento da resposta do pagamento pode ocorrer mais de uma vez: a mesma transação recebida mais de uma vez não altera o resultado. Aprovação recebida para pedido já cancelado é registrada e gera solicitação de estorno. |
| RN15 | Usuários de unidade (atendente, cozinha, gerente) operam apenas dados da própria unidade e a matriz acessa todas. |
| RN16 | Registros de auditoria não podem ser alterados nem excluídos. |
| RN17 | Todos os pagamentos, inclusive os presenciais no balcão, passam pelo serviço externo de pagamento. |
| RN18 | A eliminação de dados a pedido do titular anonimiza os dados pessoais do cliente e preserva os pedidos para fins financeiros e estatísticos. |

## 6. Ambiguidades identificadas e premissas adotadas

| ID | Ambiguidade no estudo de caso | Premissa adotada | Regra |
|---|---|---|---|
| A01 | Campanhas consideram "idade e perfil", mas a LGPD exige minimização e trata dados de crianças e adolescentes de forma especial | Data de nascimento opcional, usada só como faixa etária, programa restrito a maiores de 18 anos e segmentação apenas com consentimento de marketing | RN10, RN11, RN12 |
| A02 | Regras de pontos e descontos progressivos não definidas | Conversão de 1 ponto por R$ 1,00 / resgate de 100 pontos por R$ 5,00, níveis progressivos apenas modelados | RN08, RN09 |
| A03 | Não define quando o estoque é baixado nem como tratar disputa pelo último item no pico | Reserva na criação do pedido, devolução em cancelamento/recusa, controle de concorrência | RN03, RN04, RNF04 |
| A04 | Catálogo único ou por unidade? Como tratar variação regional e sazonalidade | Catálogo global da matriz + cardápio por unidade com preço local e vigência, variação regional de receita registrada como observação do item da unidade | RN01, RN02, RNF11 |
| A05 | Falhas de pagamento não detalhadas (demora, duplicidade, confirmação tardia, estorno) | Tempo limite, pedido aguarda resposta definitiva, expiração em 15 min, estorno em aprovação tardia | RN04, RN14, RNF05 |
| A06 | Ciclo de status do pedido não definido | Ciclo de 5 estados + cancelado, com transições validadas | RN05 |
| A07 | Não define quem pode cancelar ou dar desconto | Gerente autoriza, motivo obrigatório, limite de 20%, auditoria | RN06, RN07 |
| A08 | Pedido de balcão/totem pode não ter cliente identificado | Pedido anônimo permitido, sem pontuação | RN13 |
| A09 | "Pick-up" aparece como canal, mas também pode ser modalidade de pedidos do app | Separação entre canal de origem (APP, TOTEM, BALCAO) e tipo de atendimento (CONSUMO_LOCAL, RETIRADA) | RF08 |
| A10 | "Alta disponibilidade" sem metas numéricas | Metas assumidas de disponibilidade e tempo de resposta, verificáveis por teste | RNF06, RNF07 |
| A11 | Operação da loja sem internet não é tratada | Fora do escopo, registrada como evolução futura | — |
| A12 | Delivery não é mencionado | Fora do escopo, apenas consumo no local e retirada | — |
| A13 | Pagamento presencial no balcão pode ou não passar pelo serviço externo | Todos os pagamentos passam pelo serviço externo | RN17 |
| A14 | Eliminação de dados do titular × necessidade de manter registros de vendas | Anonimização do cliente preservando pedidos | RN18 |
