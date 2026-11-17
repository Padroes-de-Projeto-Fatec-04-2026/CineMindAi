package br.com.fatec.cinemindai.agent.command;

/**
 * Padrão COMMAND: cada ação concreta que o agente pode executar (buscar filme,
 * recomendar por gênero, etc.) é encapsulada como um comando autocontido, permitindo
 * despacho uniforme, logging e histórico via {@link CommandRegistry}.
 *
 * <p>Cada comando também descreve seus argumentos em JSON Schema ({@link #inputSchema()}),
 * o que permite expô-lo ao LLM como uma ferramenta nativa do Spring AI
 * (ver {@code br.com.fatec.cinemindai.agent.tool.AgentCommandToolCallback}).
 */
public interface AgentCommand {

    String name();

    String description();

    CommandResult execute(CommandContext context);

    /**
     * JSON Schema dos argumentos do comando, usado na {@code ToolDefinition} do Spring AI.
     * O padrão é "sem argumentos"; os comandos concretos sobrescrevem.
     */
    default String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    /** Monta o JSON Schema de um comando que recebe um único argumento do tipo string. */
    static String singleStringArgumentSchema(String argumentName, String argumentDescription) {
        return """
                {"type":"object","properties":{"%s":{"type":"string","description":"%s"}},"required":["%s"]}"""
                .formatted(argumentName, argumentDescription, argumentName);
    }
}
