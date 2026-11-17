package br.com.fatec.cinemindai.agent.strategy;

import br.com.fatec.cinemindai.agent.command.CommandRegistry;
import br.com.fatec.cinemindai.agent.core.AgentExecutionContext;
import br.com.fatec.cinemindai.agent.prompt.AgentPrompts;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

/**
 * Pede ao modelo, em uma única chamada, um plano completo de comandos a executar
 * antes de qualquer execução. Bom para pedidos diretos ("recomende filmes de terror").
 */
@Component
public class PlanThenExecuteStrategy implements PlanningStrategy {

    private final CommandRegistry commandRegistry;

    public PlanThenExecuteStrategy(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public PlannerMode mode() {
        return PlannerMode.PLAN_THEN_EXECUTE;
    }

    @Override
    public Plan plan(AgentExecutionContext context) {
        ChatClient chatClient = context.services().chatClient();
        String systemPrompt = AgentPrompts.PLAN_THEN_EXECUTE_SYSTEM.render(
                Map.of("commands", commandRegistry.describeAvailableCommands()));

        LlmPlan llmPlan = chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, context.conversationId()))
                .system(systemPrompt)
                .user(context.userMessage())
                .call()
                .entity(LlmPlan.class);

        if (llmPlan == null) {
            return new Plan(List.of(), "Não consegui montar um plano para esse pedido. Pode reformular?", false);
        }
        return new Plan(llmPlan.steps(), llmPlan.finalAnswer(), false);
    }
}
