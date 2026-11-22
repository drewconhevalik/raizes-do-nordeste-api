# API Raízes do Nordeste

API REST do back-end da rede de lanchonetes **Raízes do Nordeste**, desenvolvida para o **Projeto Multidisciplinar** do curso de **CST em Análise e Desenvolvimento de Sistemas — UNINTER** (trilha **Back-end**), 2026.

**Aluno:** Andrew Santos

A API atende, com um único contrato, os canais de venda da rede (aplicativo, totem, balcão e retirada) e a gestão das unidades e da matriz: cardápio por unidade, pedidos com controle de estoque, pagamento por serviço externo, fidelidade com consentimento (LGPD), auditoria e relatórios.

---

## Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 17 | Linguagem |
| Spring Boot 4.2 (Web MVC, Data JPA, Validation, Security, Actuator) | Framework da aplicação |
| Spring Security + OAuth2 Resource Server (JWT HS256) | Autenticação sem sessão e autorização por perfil |
| H2 (em memória) | Banco da demonstração e dos testes; o alvo de produção é PostgreSQL |
| springdoc-openapi 3.2 | Documentação OpenAPI / Swagger UI |
| JUnit 5 + MockMvc | Testes automatizados (55 testes) |
| Postman | Coleção de testes de API (56 requisições encadeadas) |
| Maven Wrapper | Build sem instalar o Maven |

## Como executar

Pré-requisito: **JDK 17 ou superior**. Não é preciso instalar o Maven nem um banco de dados.

```bash
git clone https://github.com/drewconhevalik/raizes-do-nordeste-api.git
cd raizes-do-nordeste-api

# Windows
.\mvnw.cmd spring-boot:run
# Linux/macOS
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`, já com os dados de demonstração carregados.

| Recurso | Endereço |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Contrato OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Health check | http://localhost:8080/actuator/health |

### Contas de demonstração

Todas usam a senha **`Senha@123`**. São dados fictícios, apenas para demonstração.

| Perfil | E-mail | Unidade |
|---|---|---|
| Administrador da matriz | admin@rnordeste.com | todas |
| Gerente | gerente.recife@rnordeste.com | Raízes Boa Viagem (Recife) |
| Atendente | atendente.recife@rnordeste.com | Raízes Boa Viagem (Recife) |
| Cozinha | cozinha.recife@rnordeste.com | Raízes Boa Viagem (Recife) |
| Gerente | gerente.sp@rnordeste.com | Raízes Paulista (São Paulo) |
| Cliente (fidelidade, 250 pontos) | maria@email.com | — |

Login de equipe: `POST /api/v1/auth/login`. Login de cliente: `POST /api/v1/auth/clientes/login`. No Swagger, use o botão **Authorize** com o token recebido.

### Pagamento simulado

O serviço externo de pagamento é simulado pela classe `GatewayPagamentoSimulado`. O resultado depende dos **centavos** do valor do pedido, o que torna os testes reproduzíveis:

| Centavos | Resultado |
|---|---|
| ,97 (ex.: Água de coco, R$ 6,97) | Recusado |
| ,98 (ex.: Cocada, R$ 4,98) | Serviço indisponível → a API responde 503 e preserva o pedido |
| ,99 (ex.: Queijadinha, R$ 3,99) | Demora acima do tempo limite (2 s) → 503 |
| demais | Cartão: aprovado. Pix: pendente até o webhook `POST /api/v1/pagamentos/webhook` (cabeçalho `X-Gateway-Token: dev-token-webhook`) |

Toda solicitação de pagamento exige o cabeçalho `Idempotency-Key`. Repetir a mesma chave devolve o pagamento já registrado, sem gerar nova cobrança.

## Testes

```bash
.\mvnw.cmd test        # Windows
./mvnw test            # Linux/macOS
```

- **55 testes automatizados**, que sobem a aplicação completa e verificam regras de negócio, segurança, LGPD e falhas de integração. Resultado da última execução: [docs/evidencias/testes-automatizados.md](docs/evidencias/testes-automatizados.md).
- **Coleção Postman:** [postman/raizes-do-nordeste.postman_collection.json](postman/raizes-do-nordeste.postman_collection.json), com 56 requisições (P01 a P56) e testes em cada uma. Importe no Postman e execute pelo *Collection Runner* com a API recém-iniciada.

## Documentação do projeto

| Documento | Conteúdo |
|---|---|
| [docs/01-requisitos.md](docs/01-requisitos.md) | Escopo, atores, requisitos funcionais e não funcionais, regras de negócio, ambiguidades e premissas |
| [docs/02-modelagem.md](docs/02-modelagem.md) | Diagrama de casos de uso, diagrama de classes e DER |
| [docs/03-arquitetura.md](docs/03-arquitetura.md) | Arquitetura, fluxo de pedido e pagamento, estados do pedido, implantação e endpoints |
| [docs/diagramas/](docs/diagramas/) | Imagens dos diagramas e o código-fonte que as gera (Mermaid) |
| [docs/evidencias/](docs/evidencias/) | Evidências de execução dos testes |

## Estrutura do código

O código é organizado **por módulo de negócio**. Cada módulo segue as camadas controller → service → repository.

```
src/main/java/br/com/raizesdonordeste_api/
├── auditoria/    registro e consulta de operações sensíveis
├── catalogo/     catálogo global e cardápio por unidade
├── cliente/      cadastro, consentimentos e direitos do titular (LGPD)
├── comum/        exceções e tratamento de erros (Problem Details, RFC 9457)
├── config/       Swagger, agendamento e dados de demonstração
├── estoque/      saldo e movimentações por unidade
├── fidelidade/   pontos e regras do programa
├── pagamento/    integração com o serviço de pagamento (interface + simulador)
├── pedido/       pedidos, fila, ciclo de status e expiração
├── relatorio/    relatórios gerenciais
├── seguranca/    JWT, perfis e escopo por unidade
├── unidade/      unidades da rede
└── usuario/      usuários internos e perfis
```

## Limitações conhecidas

- O banco H2 em memória é recriado a cada inicialização. A configuração para PostgreSQL está descrita na documentação, mas não foi provisionada.
- Balanceador de carga, réplicas e autoescalonamento são a proposta de implantação (docs/03-arquitetura.md), não fazem parte desta entrega.
- Campanhas segmentadas, níveis de fidelidade e metas por unidade foram apenas modelados.
- Em produção, o Swagger deve ser desativado e os segredos (`JWT_SEGREDO`, `GATEWAY_TOKEN`) devem vir de variáveis de ambiente.
