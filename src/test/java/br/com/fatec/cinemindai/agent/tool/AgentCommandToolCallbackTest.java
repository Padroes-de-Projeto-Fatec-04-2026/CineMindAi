package br.com.fatec.cinemindai.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.cinemindai.agent.command.AgentCommand;
import br.com.fatec.cinemindai.agent.command.CommandContext;
import br.com.fatec.cinemindai.agent.command.CommandRegistry;
import br.com.fatec.cinemindai.agent.command.CommandResult;
import br.com.fatec.cinemindai.agent.core.AgentExecutionContext;
import br.com.fatec.cinemindai.agent.observer.AgentExecutionListener;
import br.com.fatec.cinemindai.agent.observer.ExecutionEvent;
import br.com.fatec.cinemindai.agent.observer.ExecutionEventType;
import br.com.fatec.cinemindai.agent.strategy.PlannerMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/** Verifica o Adapter entre nossos {@link AgentCommand} e o {@link ToolCallback} do Spring AI. */
class AgentCommandToolCallbackTest {

    private final AgentCommand echoCommand = new AgentCommand() {
        @Override
        public String name() {
            return "echo";
        }

        @Override
        public String description() {
            return "Ecoa o argumento 'text'.";
        }

        @Override
        public String inputSchema() {
            return AgentCommand.singleStringArgumentSchema("text", "Texto a ecoar");
        }

        @Override
        public CommandResult execute(CommandContext context) {
            return new CommandResult(true, "eco: " + context.argument("text"), null);
        }
    };

    private final CommandRegistry registry = new CommandRegistry(List.of(echoCommand));
    private final ToolCallback tool = new AgentCommandToolCallback(echoCommand, registry);

    @Test
    void exposesTheCommandAsASpringAiToolDefinition() {
        assertThat(tool.getToolDefinition().name()).isEqualTo("echo");
        assertThat(tool.getToolDefinition().description()).isEqualTo("Ecoa o argumento 'text'.");
        assertThat(tool.getToolDefinition().inputSchema()).contains("\"text\"").contains("\"required\"");
    }

    @Test
    void executesTheCommandThroughTheRegistryAndReturnsJson() {
        String output = tool.call("{\"text\":\"ola\"}");

        assertThat(output).contains("\"success\":true").contains("eco: ola");
        assertThat(registry.history()).hasSize(1);
    }

    @Test
    void recordsTheResultInTheAgentContextAndNotifiesObservers() {
        List<ExecutionEvent> events = new CopyOnWriteArrayList<>();
        AgentExecutionListener listener = events::add;
        AgentExecutionContext agentContext = new AgentExecutionContext(
                "conv-1", "teste", PlannerMode.TOOL_CALLING, null, List.of(listener));

        tool.call("{\"text\":\"ola\"}",
                new ToolContext(Map.of(AgentCommandToolCallback.AGENT_CONTEXT_KEY, agentContext)));

        assertThat(agentContext.commandResults()).hasSize(1);
        assertThat(agentContext.commandResults().get(0).arguments()).containsEntry("text", "ola");
        assertThat(events).extracting(ExecutionEvent::type).containsExactly(ExecutionEventType.COMMAND_EXECUTED);
    }
}
