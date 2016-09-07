package edu.jhuapl.sbmt.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.jhuapl.saavtk.util.FileUtil;



/**
 * This class contains a static method for submission of jobs to run in
 * parallel. Three types of batch submission methods are supported:
 *
 * 1. Grid Engine: uses the Grid Engine software to submit jobs
 * (http://gridscheduler.sourceforge.net)
 *
 * 2. GNU Parallel: uses GNU Parallel to run jobs in parallel on multiple
 * machines (http://www.gnu.org/software/parallel/)
 *
 * 3. Local Sequential: simply runs all jobs serially, one at a time.
 *
 * 4. Local Parallel: simply runs all jobs in parallel, with max number of processes
 * running at any given time equal to number of processor cores.
 *
 */
public class BatchSubmission
{

    public enum BatchType
    {
        GRID_ENGINE, GNU_PARALLEL, LOCAL_PARALLEL, LOCAL_SEQUENTIAL
    }

    public static boolean runProgramAndWait(String program) throws IOException, InterruptedException
    {
        ProcessBuilder processBuilder = new ProcessBuilder(program.split("\\s+"));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        System.out.printf("Output of running %s is:\n", program);
        while ((line = br.readLine()) != null)
        {
            System.out.println(line);
        }

        int exitStatus = process.waitFor();
        System.out.println("Exit status is: " + exitStatus);
        return exitStatus == 0;
    }

    private static boolean runBatchSubmitProgramGridEngine(List<String> commandList) throws InterruptedException, IOException
    {
        // Create a text file for input to qsub making use of qsub's job array option
        File temp = File.createTempFile("altwg-batch-list", ".bash", null);

        FileWriter ofs = new FileWriter(temp);
        BufferedWriter out = new BufferedWriter(ofs);
        for (int i=1; i<=commandList.size(); ++i)
            out.write("if [ $SGE_TASK_ID -eq " + i + " ]; then \n "+ commandList.get(i-1) + "\n exit $?\nfi\n");
        out.close();

        String batchSubmitCommand = "qsub -S /bin/bash -V -sync y -t 1-" + commandList.size() + " " + temp.getAbsolutePath();

        return runProgramAndWait(batchSubmitCommand);
    }

    private static boolean runBatchSubmitProgramParallel(List<String> commandList) throws InterruptedException, IOException
    {
        // Create a text file with all the commands that should be run, one per
        // line
        File temp = File.createTempFile("altwg-batch-list", ".tmp", null);
        FileUtil.saveList(commandList, temp.getAbsolutePath());

        // Now submit all these batches GNU Parallel
        String batchSubmitCommand = "parallel -v -a " + temp.getAbsolutePath();

        return runProgramAndWait(batchSubmitCommand);
    }

    private static boolean runBatchSubmitProgramLocalParallel(List<String> commandList) throws IOException, InterruptedException
    {
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        final AtomicBoolean successful = new AtomicBoolean(true);
        Collection<Future<?>> futures = new LinkedList<Future<?>>();
        for (final String command : commandList)
        {
            Future<?> future = executor.submit(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        if (!runProgramAndWait(command))
                            successful.set(false);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
            futures.add(future);
        }

        try
        {
            // Wait for all tasks to end (get blocks if necessary)
            for (Future<?> future : futures)
                future.get();
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }

        executor.shutdown();

        return successful.get();
    }

    private static boolean runBatchSubmitProgramLocalSequential(List<String> commandList) throws IOException, InterruptedException
    {
        boolean successful = true;
        for (String command : commandList)
        {
            if (!runProgramAndWait(command))
                successful = false;
        }

        return successful;
    }

    public static boolean runBatchSubmitPrograms(List<String> commandList, BatchType batchType) throws InterruptedException, IOException
    {
        if (batchType == BatchType.GRID_ENGINE)
            return runBatchSubmitProgramGridEngine(commandList);
        else if (batchType == BatchType.GNU_PARALLEL)
            return runBatchSubmitProgramParallel(commandList);
        else if (batchType == BatchType.LOCAL_PARALLEL)
            return runBatchSubmitProgramLocalParallel(commandList);
        else if (batchType == BatchType.LOCAL_SEQUENTIAL)
            return runBatchSubmitProgramLocalSequential(commandList);
        return false;
    }
}
