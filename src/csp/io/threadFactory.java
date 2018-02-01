package csp.io;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class threadFactory {
    public static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    public static ForkJoinPool forkJoinPool = new ForkJoinPool(32);
    public static void submit(Runnable task){
        forkJoinPool.submit(task);
        try {
            forkJoinPool.shutdown();
            forkJoinPool.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void execute(Runnable task){
        cachedThreadPool.execute(task);
    }
}
