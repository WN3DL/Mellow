package com.roxiun.mellow.api.seraph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

final class RequestFailureBackoff {

    private static final long BASE_DELAY_MS = 5_000L;
    private static final long MAX_DELAY_MS = 120_000L;

    private final Map<String, FailureState> failures = new ConcurrentHashMap<>();

    long getRetryAfterMillis(String key) {
        FailureState state = failures.get(key);
        if (state == null) {
            return 0L;
        }

        long remaining = state.retryAtMs - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    void recordFailure(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }

        failures.compute(key, (ignored, previous) -> {
            int count = previous == null ? 1 : Math.min(previous.count + 1, 6);
            long delay = Math.min(
                MAX_DELAY_MS,
                BASE_DELAY_MS * (1L << Math.min(count - 1, 5))
            );
            return new FailureState(count, System.currentTimeMillis() + delay);
        });
    }

    void recordSuccess(String key) {
        if (key != null) {
            failures.remove(key);
        }
    }

    void removeMatching(Predicate<String> matcher) {
        if (matcher != null) {
            failures.keySet().removeIf(matcher);
        }
    }

    void clear() {
        failures.clear();
    }

    private static final class FailureState {

        private final int count;
        private final long retryAtMs;

        private FailureState(int count, long retryAtMs) {
            this.count = count;
            this.retryAtMs = retryAtMs;
        }
    }
}
