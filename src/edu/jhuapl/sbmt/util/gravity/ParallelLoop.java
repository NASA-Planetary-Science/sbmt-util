package edu.jhuapl.sbmt.util.gravity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class contains a static method, runParallelLoop, used by the gravity code for
 * computing gravity over an data array in parallel. It splits up the computation
 * into threads with tthe number of threads equal to the number of processors.
 *
 * @param <T>
 */
public class ParallelLoop<T> {

    public interface LoopFunction {
        public <T> List<T> func(int startId, int stopId);
    }

    public static <T> List<T> runParallelLoop(int size, final LoopFunction loopFunction) throws InterruptedException,
            ExecutionException {

        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<List<T>>> tasks = new ArrayList<Callable<List<T>>>();

        int processors = Runtime.getRuntime().availableProcessors();
        int chunk = size / processors;
        for (int i = 0; i < processors; i++) {
            final int startId = i * chunk;
            final int stopId = i < processors - 1 ? (i + 1) * chunk : size;

            tasks.add(new Callable<List<T>>() {
                @Override
                public List<T> call() throws Exception {
                    return loopFunction.func(startId, stopId);
                }
            });
        }

        List<Future<List<T>>> futures = executor.invokeAll(tasks);

        List<T> results = new ArrayList<T>();
        for (Future<List<T>> f : futures) {
            results.addAll(f.get());
        }

        executor.shutdown();

        return results;
    }
}
