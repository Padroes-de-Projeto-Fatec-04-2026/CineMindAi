package br.com.fatec.cinemindai.agent.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

/** Garante que os templates .st carregam e são renderizados pelo PromptTemplate do Spring AI. */
class AgentPromptsTest {

    @Test
    void rendersTemplatesWithVariables() {
        String rendered = AgentPrompts.PLAN_THEN_EXECUTE_SYSTEM.render(
                Map.of("commands", "- echo: Argumentos: {\"text\": string}."));

        assertThat(rendered).contains("- echo: Argumentos: {\"text\": string}.").doesNotContain("{commands}");
        assertThat(AgentPrompts.REACT_USER.render(Map.of("request", "terror", "observations", "(nenhuma ainda)")))
                .contains("Pedido original: terror").contains("(nenhuma ainda)");
        assertThat(AgentPrompts.RESPONDING_USER.render(Map.of("request", "terror", "results", "- ok")))
                .contains("- ok");
    }

    @Test
    void rendersTemplatesWithoutVariables() {
        assertThat(AgentPrompts.TOOL_CALLING_SYSTEM.render()).contains("ferramentas");
        assertThat(AgentPrompts.RESPONDING_SYSTEM.render()).contains("português");
        assertThat(AgentPrompts.TOOL_CALLING_SYSTEM.template()).isInstanceOf(SystemPromptTemplate.class);
    }
}
