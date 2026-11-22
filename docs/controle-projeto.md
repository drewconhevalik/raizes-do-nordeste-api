# Controle do Projeto — Rede "Raízes do Nordeste" (Trilha Back-end)

## Identificação (capa)
- Aluno: Andrew Santos — RU (consta no documento final)
- Curso: CST Análise e Desenvolvimento de Sistemas
- Disciplina: Projeto Multidisciplinar
- Polo: Uninter Pirituba
- Semestre: C Fase II 2026 Regular
- Professores: Prof. Giuliano Lanes de Almeida, Esp.; Profa. Luciane Yanase Hibara Kanashiro, Me.; Prof. Rodrigo da Silva do Nascimento, Me.; Prof. Winston Sen Lun Fung, Me.
- Trilha: Back-end

## Prazo e tempo
- Início: 21/11/2026 03:54 (horário de Brasília)
- Prazo final (informado pelo aluno): 23/11/2026 23:59
- Meta interna de envio: 23/11/2026 20:00 (margem de ~4h)
- Horas por dia: sem limite definido (aluno: "vamos fazendo")

### Registro de sessões (tempo de relógio, não estimativa)
| Data | Início | Fim | Etapa | Observação |
|---|---|---|---|---|
| 21/11 | 03:54 | 04:41 | E0 | Análise do PDF, escolha da trilha (inclui tempo de leitura do aluno) |
| 22/11 | 01:57 | 04:22 | E1–E6 | Retomada (sábado não trabalhado; cronograma replanejado). E1–E5 concluídas e validadas; E6 com 31 testes passando. Pausa combinada para descanso. |

## Etapas
| Etapa | Descrição | Estimativa | Status |
|---|---|---|---|
| E0 | Análise do PDF e escolha da trilha | 1h | Concluída (21/11 04:40) |
| E1 | Stack, ambiente, repositório GitHub público | 1,5h | Concluída |
| E2 | Requisitos (RF/RNF), regras de negócio, premissas | 2,5h | Concluída |
| E3 | Casos de uso, DER, diagrama de classes | 3h | Concluída |
| E4 | Arquitetura e contrato dos endpoints | 1h | Concluída |
| E5 | Base: banco, autenticação, perfis | 2,5h | Concluída |
| E6 | Cardápio por unidade, estoque, pedidos, status | 3h | Concluída |
| E7 | Pagamento simulado, fidelização + consentimento, auditoria | 2,5h | Pendente |
| E8 | Testes automatizados + coleção Postman | 2h | Pendente |
| E9 | Plano de testes completo + evidências | 2,5h | Pendente |
| E10 | Introdução, conclusão, referências, anexos | 2,5h | Pendente |
| E11 | Montagem do PDF, auditoria, links, envio | 2h + margem | Pendente |

## Decisões técnicas
| ID | Data | Decisão | Justificativa | Status |
|---|---|---|---|---|
| D01 | 21/11 04:40 | Trilha Back-end | Caso centrado em regras de negócio, estoque, auditoria, LGPD e pagamento desacoplado; evidências objetivas (repositório, Swagger, Postman, testes) que também alimentam o critério 4; maior aceleração com IA | Aprovada pelo aluno |
| D02 | 22/11 01:57 | Java 17 + Spring Boot (Maven Wrapper), H2 para demo/testes, PostgreSQL como alvo de produção documentado, JUnit + Postman, VS Code | Aluno está aprendendo Java e já usou Spring Boot (defensabilidade); diagrama de classes ↔ entidades JPA; transações/concorrência para estoque; Spring Security para perfis; Swagger automático; nada extra a instalar | Aprovada pelo aluno |
| D04 | 22/11 03:21 | Chaves como `int` no DER e `id` sem tipo no diagrama de classes; casos de uso sem include/extend | Pedido do aluno (simplificação visual). Para coerência DER × código, entidades usarão `Integer` como tipo de id | Aprovada pelo aluno |
| D05 | 22/11 04:05 | E-mails de demonstração em @rnordeste.com; "Suco de cajá" → "Suco de graviola" | Comparação com repositório público de outro aluno (salguento/projeto-back-end-uninter) para evitar semelhança; demais coincidências são de domínio/convenção | Aprovada pelo aluno |
| D03 | 22/11 | Sem Lombok | Menos configuração de IDE e código mais explícito para o aluno explicar; DTOs com records do Java 17 | Proposta |

## Premissas
| ID | Premissa | Origem |
|---|---|---|
| P01 | O "roteiro da trilha" corresponde aos itens específicos de cada trilha descritos no PDF (Estrutura, itens 4, 5 e 6; Orientações Gerais, item 5 — Repositório). O PDF menciona uma "Tabela consolidada — critérios oficiais" no roteiro; o aluno informou que não há documento adicional. | Aluno, 21/11 |
| P02 | Pesos dos critérios (10/30/30/20/10) usados como referência de prioridade. | PDF p. 11–13 |

## Checklist Back-end (fechado na E0)
### Documento principal
- [ ] Capa (curso, disciplina, nome, RU, polo, semestre, professor) e sumário
- [ ] Introdução: contexto, objetivos, usuários, relevância
- [ ] Trilha Back-end declarada explicitamente
- [ ] RF e RNF em tabelas
- [ ] Cobertura dos 5 aspectos do critério 2: multicanal; diferenças entre unidades; estoque + relatórios da matriz; fidelização + LGPD; pagamento externo
- [ ] Ambiguidades e premissas documentadas
- [ ] Decisões técnicas justificadas
- [ ] Diagrama de casos de uso
- [ ] Diagrama de classes
- [ ] DER
- [ ] Descrição dos principais endpoints da API
- [ ] Tecnologias de persistência justificadas
- [ ] Escalabilidade, integração, tolerância a falhas, crescimento
- [ ] Entrega técnica: link do repositório com código-fonte
- [ ] Plano de testes: funcionais, não funcionais, segurança/LGPD, integração, pico
- [ ] Cenários com entradas, passos, saída esperada; positivos e negativos
- [ ] Evidências: coleção Postman, relatório de testes automatizados, registros de execução
- [ ] Conclusão: lições, desafios, evoluções futuras
- [ ] Referências verificáveis
### Anexos
- [ ] UML complementares (sequência do pagamento, estados do pedido, implantação) — se agregarem
- [ ] Prints de execução e testes
### Entrega
- [ ] Repositório público testado em janela anônima
- [ ] Coerência documento × código
- [ ] PDF único
- [ ] Envio na área "Trabalhos" do AVA-Univirtus até 23/11 23:59

## Referências (registro contínuo)
| ID | Referência | Link | Consulta | Fundamenta |
|---|---|---|---|---|
| R01 | ALMEIDA, G. L.; KANASHIRO, L. Y. H.; NASCIMENTO, R. S.; FUNG, W. S. L. Projeto Multidisciplinar — Orientações e Estudo de Caso: Rede "Raízes do Nordeste". UNINTER, 2026, rev. 0.3. | (documento do AVA) | 21/11/2026 | Todo o projeto |

## Registro de uso de IA
| ID | Data | Tarefa | Como a IA ajudou | O que o aluno fez/decidiu |
|---|---|---|---|---|
| IA02 | 22/11 | Ambiente e Git | Diagnóstico da pasta duplicada e da URL errada do remoto; verificação do acesso público ao repositório | Gerou o projeto no Initializr, executou comandos, criou o repositório |
| IA03 | 22/11 | Rascunho de requisitos (docs/01-requisitos.md) | Rascunho de 25 RF, 11 RNF, 18 RN e 14 ambiguidades com premissas | (pendente: revisão e aprovação das premissas) |
| IA06 | 22/11 | Código da E6 (cardápio, estoque, pedidos) | Escrita dos serviços, controllers e 21 testes | Executou os testes (31/31) e fez o commit |
| IA05 | 22/11 | Código da base (E5) | Escrita das 14 entidades, segurança JWT, login, tratamento de erros, dados de demonstração e 9 testes; consulta de compatibilidade springdoc × Spring Boot 4.2 | Executou os testes (10/10), comparou com repositório público de outro aluno e pediu ajustes de semelhança |
| IA04 | 22/11 | Diagramas (E3) | Geração dos diagramas como código (Mermaid + script SVG), layout e renderização | (pendente: revisão) |
| IA01 | 21/11 | Leitura do PDF de orientações | Extração de requisitos, critérios, ambiguidades e riscos; proposta de cronograma | Escolheu a trilha, definiu prazo, forneceu dados e contexto da máquina |

## Pendências de documentação (incluir na E10)
- Nova regra implementada na E6: canal APP exige cliente autenticado; BALCAO exige equipe da unidade; TOTEM aceita anônimo → registrar como RN19 em docs/01-requisitos.md.
- Resgate de pontos (RF16) implementado junto com a criação do pedido na E6 (antecipado da E7).
- Status manual: PAGO só via pagamento; CANCELADO só via endpoint de cancelamento; cozinha define EM_PREPARO/PRONTO, atendente ENTREGUE → registrar em RN05.
- Endpoint extra: GET /unidades/{id}/cardapio/gestao (visão completa para gerente) → acrescentar em docs/03-arquitetura.md.
- Evolução futura: desligar Swagger em produção (aviso do springdoc nos testes).

## Notas para o documento final
- Análise: 11 ambiguidades levantadas na E0 (idade/perfil × LGPD e menores; regras de pontos; momento de baixa de estoque e concorrência; catálogo global × por unidade; falhas de pagamento; ciclo de status; permissão de cancelamento/desconto; pontuação sem cliente identificado; delivery fora do escopo; metas mensuráveis de disponibilidade; contingência sem internet).
- Estratégia: 60% do peso em análise + modelagem → implementação parcial (fatia vertical) que comprove o desenho.
