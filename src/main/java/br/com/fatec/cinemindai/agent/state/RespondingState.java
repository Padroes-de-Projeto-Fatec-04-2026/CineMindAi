package br.com.fatec.cinemindai.agent.state;

import br.com.fatec.cinemindai.agent.command.CommandExecutionRecord;
import br.com.fatec.cinemindai.agent.core.AgentExecutionContext;
import br.com.fatec.cinemindai.agent.prompt.AgentPrompts;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.memory.ChatMemory;

public class RespondingState implements AgentState {

    @Override
    public String name() {
        return "RESPONDING";
    }

    @Override
    public AgentState process(AgentExecutionContext context) {
        if (context.finalAnswer() == null || context.finalAnswer().isBlank()) {
            String observations = context.commandResults().stream()
                    .map(this::describe)
                    .collect(Collectors.joining("\n"));

            String reply = context.services().chatClient().prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, context.conversationId()))
                    .system(AgentPrompts.RESPONDING_SYSTEM.render())
                    .user(AgentPrompts.RESPONDING_USER.render(Map.of(
                            "request", context.userMessage(),
                            "results", observations)))
                    .call()
                    .content();

            context.setFinalAnswer(reply);
        }
        return new CompletedState();
    }

    private String describe(CommandExecutionRecord record) {
        return "- " + record.commandName() + " -> " + record.result().summary();
    }
}
