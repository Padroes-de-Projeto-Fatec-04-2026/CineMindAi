package br.com.fatec.cinemindai.agent.tool;

import br.com.fatec.cinemindai.agent.command.AgentCommand;
import br.com.fatec.cinemindai.agent.command.CommandRegistry;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra todos os {@link AgentCommand} da aplicação como ferramentas do Spring AI,
 * reunidas em um único {@link ToolCallbackProvider}. Novos comandos anotados com
 * {@code @Component} viram ferramentas automaticamente, sem alterar esta classe.
 */
@Configuration
public class AgentToolsConfig {

    @Bean
    public ToolCallbackProvider movieAgentTools(List<AgentCommand> commands, CommandRegistry commandRegistry) {
        List<ToolCallback> callbacks = commands.stream()
                .<ToolCallback>map(command -> new AgentCommandToolCallback(command, commandRegistry))
                .toList();
        return ToolCallbackProvider.from(callbacks);
    }
}
