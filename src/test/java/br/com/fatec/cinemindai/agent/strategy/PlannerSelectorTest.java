package br.com.fatec.cinemindai.agent.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fatec.cinemindai.agent.command.CommandRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;

class PlannerSelectorTest {

    private final CommandRegistry commandRegistry = new CommandRegistry(List.of());
    private final PlanThenExecuteStrategy planThenExecute = new PlanThenExecuteStrategy(commandRegistry);
    private final ReActPlanningStrategy reAct = new ReActPlanningStrategy(commandRegistry);
    private final HumanInTheLoopStrategy humanInTheLoop = new HumanInTheLoopStrategy(planThenExecute);
    private final NativeToolCallingStrategy toolCalling = new NativeToolCallingStrategy(ToolCallbackProvider.from(List.of()));
    private final PlannerSelector selector = new PlannerSelector(reAct, planThenExecute, humanInTheLoop, toolCalling);

    @Test
    void respectsAnExplicitlyRequestedMode() {
        assertThat(selector.select(PlannerMode.REACT, "qualquer coisa")).isSameAs(reAct);
        assertThat(selector.select(PlannerMode.HUMAN_IN_THE_LOOP, "qualquer coisa")).isSameAs(humanInTheLoop);
        assertThat(selector.select(PlannerMode.PLAN_THEN_EXECUTE, "qualquer coisa")).isSameAs(planThenExecute);
        assertThat(selector.select(PlannerMode.TOOL_CALLING, "qualquer coisa")).isSameAs(toolCalling);
    }

    @Test
    void picksHumanInTheLoopWhenUserAsksForConfirmation() {
        assertThat(selector.select(PlannerMode.AUTO, "pode confirmar antes de buscar?")).isSameAs(humanInTheLoop);
    }

    @Test
    void picksReActForLongOpenEndedMessages() {
        String longMessage = "Eu gostaria de assistir algo hoje a noite mas nao sei bem o que, "
                + "talvez algo de ficcao cientifica ou suspense, dependendo do clima da noite";

        assertThat(selector.select(PlannerMode.AUTO, longMessage)).isSameAs(reAct);
    }

    @Test
    void defaultsToSpringAiToolCallingForShortDirectRequests() {
        assertThat(selector.select(PlannerMode.AUTO, "recomende filmes de terror")).isSameAs(toolCalling);
    }
}
