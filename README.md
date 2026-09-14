# CineMindAi

FindMoviesAI é um Knowledge Assistant especializado na pesquisa e síntese de informações sobre filmes e séries.

## Arquitetura do Agente

O sistema é construído utilizando os seguintes padrões de design voltados para a criação de Agentes de IA escaláveis e modulares:

- **Strategy Pattern (Planning Strategies)**:
  A interface de planejamento permite múltiplas implementações (`ReActPlanner`, `PlanThenExecutePlanner`, `HumanInTheLoopPlanner`).
  *Benefícios*: O agente pode alternar modos de planejamento baseando-se na complexidade da tarefa sem alterar o código em tempo de execução.

- **Chain of Responsibility (Advisor Chains)**:
  "Advisors" processam requisições em sequência, adicionando comportamentos. O ambiente de execução do agente configura a cadeia.
  *Benefícios*: Separação de responsabilidades; facilidade para adicionar logs, regras de conformidade (compliance), etc.

- **Observer Concepts (Execution Monitoring)**:
  O loop de observação do agente implementa o padrão observer: o executor notifica o contexto (e os ouvintes) sobre os resultados das ferramentas.
  *Benefícios*: Permite a criação de dashboards em tempo real, alertas e loops de feedback.

- **Dependency Injection (Spring Integration)**:
  Todos os componentes são beans do Spring (ferramentas, gerenciadores de memória e planejadores são injetados).
  *Benefícios*: Recursos empresariais (transações, segurança, métricas) são herdados de forma automática.

## Fontes de Dados e Coleta de Informações

As informações sobre filmes e séries são consultadas a partir do serviço **OMDb API**, utilizando a integração ou wrapper:
- [api-omdb (Omertron)](https://github.com/Omertron/api-omdb.git)

Além da comunicação com serviços externos, também utiliza-se o **Ollama** de forma local para processamento de inferência do agente e síntese de resultados.

## Integração

A configuração do projeto suporta o isolamento das chaves da API de serviços locais e externos por meio de variáveis de ambiente.
