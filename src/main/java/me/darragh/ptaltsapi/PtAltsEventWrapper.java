package me.darragh.ptaltsapi;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A wrapper class for all SSE events.
 * Must be manually registered to receive events.
 *
 * @see PtAltsRestockSseClient
 * @see PtAltsOrderSseClient
 * @author darraghd493
 * @since 1.0.2
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PtAltsEventWrapper {
    private final Map<Class<?>, List<Consumer<?>>> eventListeners = new HashMap<>();

    @Getter
    private final Consumer<PtAltsRestockSseClient.RestockEvent> onRestockEvent = event -> this.triggerEvent(new RestockEvent(event.stockType(), event.addedCount(), event.newTotal()));
    @Getter
    private final Consumer<PtAltsOrderSseClient.TokenEvent> onTokenEvent = event -> {
        if (event.isToken() && event.token() != null) this.triggerEvent(new TokenEvent(event.token()));
    };

    public <T extends Event> void registerListener(@NotNull Class<T> eventClass, @NotNull Consumer<T> listener) throws IllegalArgumentException {
        if (!eventClass.equals(RestockEvent.class) && !eventClass.equals(TokenEvent.class)) {
            // strict check for supported event types - we want to prevent misusage
            throw new IllegalArgumentException("Unsupported event class: " + eventClass.getName());
        }
        this.eventListeners.computeIfAbsent(eventClass, k -> new ArrayList<>())
                .add(listener);
    }

    public <T extends Event> void unregisterListener(@NotNull Class<T> eventClass, @NotNull Consumer<T> listener) {
        List<Consumer<?>> listeners = this.eventListeners.get(eventClass);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                this.eventListeners.remove(eventClass);
            }
        }
    }

    private void triggerEvent(@NotNull Event event) {
        List<Consumer<?>> listeners = this.eventListeners.get(event.getClass());
        if (listeners != null) {
            for (Consumer<?> listener : listeners) {
                @SuppressWarnings("unchecked")
                Consumer<Event> eventListener = (Consumer<Event>) listener;
                eventListener.accept(event);
            }
        }
    }

    //region Events
    public record RestockEvent(@NotNull String product, int quantity, int total) implements Event {
    }

    public record TokenEvent(@NotNull String token) implements Event {
    }

    public interface Event {
    }
    //endregion
}
