package dev.rinchan.letyourfrienddrink.api;

import dev.rinchan.letyourfrienddrink.LetYourFriendDrink;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class FriendDrinkCompletedEvents {
    private static final Map<String, Consumer<FriendDrinkCompletedEvent>> REGISTERED = new LinkedHashMap<>();
    private static volatile List<RegisteredListener> listeners = List.of();

    private FriendDrinkCompletedEvents() {
    }

    public static synchronized void register(String listenerId, Consumer<FriendDrinkCompletedEvent> listener) {
        Objects.requireNonNull(listenerId, "listenerId");
        Objects.requireNonNull(listener, "listener");
        if (listenerId.isBlank()) throw new IllegalArgumentException("listenerId must not be blank");
        if (REGISTERED.putIfAbsent(listenerId, listener) != null) {
            throw new IllegalArgumentException("Duplicate friend-drink listener id: " + listenerId);
        }
        listeners = REGISTERED.entrySet().stream()
            .map(entry -> new RegisteredListener(entry.getKey(), entry.getValue()))
            .toList();
    }

    /** Internal dispatch surface used by the feeding implementation after state is committed. */
    public static void post(FriendDrinkCompletedEvent event) {
        Objects.requireNonNull(event, "event");
        for (RegisteredListener registered : listeners) {
            try {
                registered.listener().accept(event);
            } catch (RuntimeException exception) {
                LetYourFriendDrink.LOGGER.error(
                    "Friend-drink completion listener {} failed", registered.id(), exception);
            }
        }
    }

    private record RegisteredListener(String id, Consumer<FriendDrinkCompletedEvent> listener) {
    }
}
