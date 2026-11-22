package dev.server404.deepdarkexpeditions; 

import java.util.EnumSet;
import java.util.Set;

public final class ExpeditionManager {

    public enum State {
        PREPARING,
        ACTIVE,
        EXTRACTION,
        EVACUATING,
        RESETTING
    }

    private final DeepDarkExpeditions plugin;

    private State currentState;

    public ExpeditionManager(DeepDarkExpeditions plugin) {
        this.plugin = plugin;
        this.currentState = State.PREPARING;
    }

    /**
     * Obtener el estado actual de la expedición.
     */
    public State getState() {
        return currentState;
    }

    /**
     * Comprobar si la expedición está en un estado específico.
     */
    public boolean isState(State state) {
        return currentState == state;
    }

    /**
     * Intentar cambiar al siguiente estado.
     */
    public boolean transitionTo(State newState) {

        if (newState == null) {
            plugin.getLogger().warning(
                    "No se puede cambiar a un estado nulo."
            );
            return false;
        }

        if (!isTransitionAllowed(newState)) {
            plugin.getLogger().warning(
                    "Transición no permitida: "
                            + currentState + " -> " + newState
            );
            return false;
        }

        State previousState = currentState;
        currentState = newState;

        plugin.getLogger().info(
                "[EXPEDICION] Estado cambiado: "
                        + previousState + " -> " + currentState
        );

        return true;
    }

    /**
     * Validar las transiciones permitidas.
     */
    private boolean isTransitionAllowed(State newState) {

        Set<State> allowedStates = switch (currentState) {

            case PREPARING ->
                    EnumSet.of(State.ACTIVE);

            case ACTIVE ->
                    EnumSet.of(State.EXTRACTION);

            case EXTRACTION ->
                    EnumSet.of(State.EVACUATING);

            case EVACUATING ->
                    EnumSet.of(State.RESETTING);

            case RESETTING ->
                    EnumSet.of(State.PREPARING);
        };

        return allowedStates.contains(newState);
    }

    /**
     * Obtener el nombre del estado actual.
     */
    public String getStateName() {
        return currentState.name();
    }
}
