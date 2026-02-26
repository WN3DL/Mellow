package com.roxiun.mellow.core.async;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncExecutor {

    private static final AsyncExecutor INSTANCE = new AsyncExecutor();

    private final ExecutorService profileIoExecutor = Executors.newFixedThreadPool(
        8,
        namedFactory("Mellow-ProfileIO")
    );
    private final ExecutorService chatExecutor = Executors.newFixedThreadPool(
        4,
        namedFactory("Mellow-Chat")
    );
    private final ExecutorService commandExecutor = Executors.newFixedThreadPool(
        4,
        namedFactory("Mellow-Command")
    );

    private AsyncExecutor() {}

    public static AsyncExecutor getInstance() {
        return INSTANCE;
    }

    public void profileIo(Runnable task) {
        profileIoExecutor.submit(task);
    }

    public void chat(Runnable task) {
        chatExecutor.submit(task);
    }

    public void command(Runnable task) {
        commandExecutor.submit(task);
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(
                runnable,
                prefix + "-" + counter.incrementAndGet()
            );
            thread.setDaemon(true);
            return thread;
        };
    }
}
