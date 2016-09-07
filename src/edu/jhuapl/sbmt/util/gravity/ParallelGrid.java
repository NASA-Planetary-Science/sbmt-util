package edu.jhuapl.sbmt.util.gravity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This class contains a static method, runParallelGrid, used by the gravity code for
 * computing gravity over a grid in parallel. It splits up the computation
 * into threads with tthe number of threads equal to the number of processors.
 *
 * @param <T>
 */
public class ParallelGrid<T> {

    public interface GridFunction {
        public <T> T[][] func(int startRow, int stopRow);
    }

    public static <T> T[][] runParallelGrid(int numRows, final GridFunction loopFunction) throws InterruptedException,
            ExecutionException {

        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<T[][]>> tasks = new ArrayList<Callable<T[][]>>();

        int processors = Runtime.getRuntime().availableProcessors();
        int chunk = numRows / processors;
        for (int i = 0; i < processors; i++) {
            final int startRow = i * chunk;
            final int stopRow = i < processors - 1 ? (i + 1) * chunk : numRows;

            tasks.add(new Callable<T[][]>() {
                @Override
                public T[][] call() throws Exception {
                    return loopFunction.func(startRow, stopRow);
                }
            });
        }

        // Merge all the 3D arrays into a single array
        List<Future<T[][]>> futures = executor.invokeAll(tasks);

        T[][] results = futures.get(0).get().clone();
        int numColumns = results[0].length; // TODO check this

        for (int i = 0; i < processors; i++) {
            final int startRow = i * chunk;
            final int stopRow = i < processors - 1 ? (i + 1) * chunk : numRows;

            Future<T[][]> f = futures.get(i);

            T[][] grid = f.get();
            for (int m = startRow; m < stopRow; ++m)
                for (int n = 0; n < numColumns; ++n) {
                    results[m][n] = grid[m][n];
                }
        }

        executor.shutdown();

        return results;
    }
}
