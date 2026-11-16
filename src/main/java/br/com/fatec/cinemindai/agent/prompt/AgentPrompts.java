package br.com.fatec.cinemindai.agent.prompt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.io.ClassPathResource;

/**
 * Catálogo dos prompts do agente. Os textos ficam em {@code src/main/resources/prompts/*.st}
 * e são renderizados pelos {@link PromptTemplate} / {@link SystemPromptTemplate} do Spring AI
 * (motor StringTemplate, variáveis no formato {@code {nome}}), em vez de concatenação manual
 * de strings no código Java.
 */
public enum AgentPrompts {

    PLAN_THEN_EXECUTE_SYSTEM("plan-then-execute-system.st", true),
    REACT_SYSTEM("react-system.st", true),
    REACT_USER("react-user.st", false),
    TOOL_CALLING_SYSTEM("tool-calling-system.st", true),
    RESPONDING_SYSTEM("responding-system.st", true),
    RESPONDING_USER("responding-user.st", false);

    private final String template;
    private final boolean system;

    AgentPrompts(String fileName, boolean system) {
        this.template = load("prompts/" + fileName);
        this.system = system;
    }

    /** Cria um novo template do Spring AI para este prompt (system ou user). */
    public PromptTemplate template() {
        return system ? new SystemPromptTemplate(template) : new PromptTemplate(template);
    }

    public String render() {
        return template().render();
    }

    public String render(Map<String, Object> variables) {
        return template().render(variables);
    }

    // Lido explicitamente em UTF-8 para não depender do charset padrão do sistema operacional.
    private static String load(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível carregar o prompt " + path, e);
        }
    }
}
