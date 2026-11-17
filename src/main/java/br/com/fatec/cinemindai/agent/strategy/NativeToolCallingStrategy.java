package br.com.fatec.cinemindai.agent.strategy;

import br.com.fatec.cinemindai.agent.core.AgentExecutionContext;
import br.com.fatec.cinemindai.agent.prompt.AgentPrompts;
import br.com.fatec.cinemindai.agent.tool.AgentCommandToolCallback;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * Estratégia que delega o planejamento ao <i>tool calling</i> nativo do Spring AI: os comandos
 * são oferecidos ao modelo como ferramentas ({@link ToolCallbackProvider}) e o próprio
 * framework conduz o loop "modelo pede ferramenta → executa → devolve resultado ao modelo"
 * até chegar a uma resposta final.
 *
 * <p>O {@link AgentExecutionContext} vai no {@code toolContext} (não é enviado ao modelo), para
 * que cada ferramenta executada seja registrada na trace e notificada aos observadores.
 */
@Component
public class NativeToolCallingStrategy implements PlanningStrategy {

    private final ToolCallbackProvider movieAgentTools;

    public NativeToolCallingStrategy(ToolCallbackProvider movieAgentTools) {
        this.movieAgentTools = movieAgentTools;
    }

    @Override
    public PlannerMode mode() {
        return PlannerMode.TOOL_CALLING;
    }

    @Override
    public Plan plan(AgentExecutionContext context) {
        String answer = context.services().chatClient().prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, context.conversationId()))
                .system(AgentPrompts.TOOL_CALLING_SYSTEM.render())
                .user(context.userMessage())
                .toolCallbacks(movieAgentTools)
                .toolContext(Map.of(AgentCommandToolCallback.AGENT_CONTEXT_KEY, context))
                .call()
                .content();

        if (answer == null || answer.isBlank()) {
            answer = "Não consegui encontrar uma resposta para esse pedido. Pode reformular?";
        }
        // As ferramentas já foram executadas pelo Spring AI: o plano chega com a resposta final pronta.
        return new Plan(List.of(), answer, false);
    }
}
