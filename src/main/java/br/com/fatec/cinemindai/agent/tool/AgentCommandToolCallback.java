package br.com.fatec.cinemindai.agent.tool;

import br.com.fatec.cinemindai.agent.command.AgentCommand;
import br.com.fatec.cinemindai.agent.command.CommandRegistry;
import br.com.fatec.cinemindai.agent.command.CommandResult;
import br.com.fatec.cinemindai.agent.core.AgentExecutionContext;
import br.com.fatec.cinemindai.agent.observer.ExecutionEventType;
import br.com.fatec.cinemindai.agent.strategy.PlannedStep;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.JsonParser;

/**
 * Padrão ADAPTER: adapta um {@link AgentCommand} (nosso padrão Command) à interface
 * {@link ToolCallback} do Spring AI. Assim o próprio modelo pode decidir chamar o comando
 * via <i>tool calling</i> nativo, sem que os comandos precisem conhecer o Spring AI.
 *
 * <p>A execução continua passando pelo {@link CommandRegistry} (Invoker), mantendo o
 * histórico. Quando o {@link ToolContext} traz o {@link AgentExecutionContext} da interação,
 * o resultado também é registrado nele e um evento é emitido aos observadores (Observer).
 */
public class AgentCommandToolCallback implements ToolCallback {

    /** Chave usada no {@link ToolContext} para transportar o contexto de execução do agente. */
    public static final String AGENT_CONTEXT_KEY = "agentExecutionContext";

    private final AgentCommand command;
    private final CommandRegistry commandRegistry;
    private final ToolDefinition toolDefinition;

    public AgentCommandToolCallback(AgentCommand command, CommandRegistry commandRegistry) {
        this.command = command;
        this.commandRegistry = commandRegistry;
        this.toolDefinition = ToolDefinition.builder()
                .name(command.name())
                .description(command.description())
                .inputSchema(command.inputSchema())
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        PlannedStep step = new PlannedStep(command.name(), parseArguments(toolInput),
                "Chamado pelo modelo via tool calling do Spring AI");
        CommandResult result = commandRegistry.execute(step);

        AgentExecutionContext agentContext = agentContextFrom(toolContext);
        if (agentContext != null) {
            agentContext.recordCommandResult(step, result);
            agentContext.emit(ExecutionEventType.COMMAND_EXECUTED,
                    step.commandName() + " (tool calling) -> " + result.summary());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.success());
        response.put("summary", result.summary());
        response.put("data", result.data());
        return JsonParser.toJson(response);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonParser.fromJson(toolInput, Map.class);
        return parsed == null ? Map.of() : parsed;
    }

    private AgentExecutionContext agentContextFrom(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object value = toolContext.getContext().get(AGENT_CONTEXT_KEY);
        return value instanceof AgentExecutionContext context ? context : null;
    }
}
