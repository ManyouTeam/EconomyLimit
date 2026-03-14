package cn.superiormc.economylimit.database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DatabaseExecutor {

    public static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            runnable -> new Thread(runnable, "EconomyLimit-DB")
    );

    private DatabaseExecutor() {
    }
}
