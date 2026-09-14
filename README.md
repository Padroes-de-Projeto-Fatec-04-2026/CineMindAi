# 🎬 CineMindAi

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![Ollama](https://img.shields.io/badge/Ollama-Local%20LLM-black.svg)](https://ollama.ai/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**CineMindAi** é um agente inteligente de recomendação de filmes construído com **Spring AI** e **Ollama** (utilizando modelos locais e 100% open source). 

O projeto tem um forte foco educacional e arquitetural, demonstrando a aplicação prática de diversos padrões de projeto (Design Patterns) como **State**, **Command**, **Strategy** e **Observer** em um cenário moderno de IA generativa.

---

## 🏗️ Arquitetura

A arquitetura detalhada, juntamente com os diagramas UML, está documentada separadamente. 
📖 **[Veja a explicação detalhada da arquitetura aqui](docs/ARCHITECTURE.md)**.

## 🚀 Pré-requisitos

Para rodar o projeto localmente, você precisará de:

1. **Java 17+** (O Maven Wrapper já está incluso no projeto, não é necessário instalar o Maven separadamente).
2. **[Ollama](https://ollama.com/)** instalado e rodando localmente.

### Configurando o Ollama

Inicie o servidor do Ollama e baixe o modelo `llama3.1` (ou ajuste no `application.properties` para o modelo de sua preferência):

```bash
ollama serve
ollama pull llama3.1
```

## 🛠️ Rodando o Projeto

Com os pré-requisitos atendidos, inicie a aplicação Spring Boot:

```bash
./mvnw spring-boot:run
```

A aplicação estará disponível em `http://localhost:8080`. 
*(Dica: O modelo e a URL do Ollama podem ser customizados no arquivo `src/main/resources/application.properties`)*.

---

## 🎯 Como Testar (Exemplos via cURL)

A aplicação suporta múltiplos modos de planejamento, demonstrando diferentes padrões de interação com agentes de IA.

### 1. Pedido Direto (Plan-then-Execute)
*O agente escolhe a melhor estratégia (selecionada automaticamente) e executa de forma autônoma.*
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Recomende filmes de ficção científica parecidos com Interestelar"}'
```

### 2. Pedido com Confirmação (Human-in-the-loop)
*O agente planeja a ação e aguarda a aprovação humana antes de executar.*
```bash
# Inicia a interação
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"conversationId": "demo-1", "message": "Pode confirmar antes de buscar filmes de terror?"}'

# A resposta indicará "state": "AWAITING_HUMAN_APPROVAL".
# Em seguida, aprove a execução:
curl -X POST http://localhost:8080/api/agent/chat/demo-1/approve
```

### 3. Forçando um Modo Específico de Planejamento
*É possível instruir a API a usar uma estratégia de agente específica.*
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Quero algo pra assistir hoje", "plannerMode": "REACT"}'
```
> **Valores aceitos para `plannerMode`:** `AUTO`, `REACT`, `PLAN_THEN_EXECUTE`, `HUMAN_IN_THE_LOOP`.

### 4. Acompanhamento em Tempo Real (SSE / Observer)
*Utiliza Server-Sent Events (SSE) para emitir eventos de mudança de estado da execução em tempo real.*
```bash
curl -N http://localhost:8080/api/agent/chat/demo-1/events
```

---

## 🧪 Testes Automatizados

O projeto possui uma suíte de testes robusta que verifica os padrões implementados sem depender do Ollama em execução.

```bash
./mvnw test
```

**O que é testado?**
- **Testes Unitários/Integração (`src/test/java/.../agent/`)**: Cobrem o `CommandRegistry`, o `PlannerSelector` e as transições de estado do `AgentExecutionContext` (utilizando *fakes* e sem dependência do Ollama).
- **Teste de Contexto (`CineMindAiApplicationTests`)**: Sobe o contexto Spring completo para garantir que as injeções de dependência resolvam corretamente (também não realiza chamadas reais ao Ollama).
