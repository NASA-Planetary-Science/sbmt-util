package edu.jhuapl.sbmt.util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.jhuapl.saavtk.util.FileUtil;

import altwg.util.BatchSubmit;
import altwg.util.BatchType;

/**
 * @deprecated Use {@link BatchSubmit} instead.
 * <br>
 * This class contains a static method for submission of jobs to run in parallel. Three types of batch submission
 * methods are supported:
 * <br>
 * <ol>
 * <li>
 * Grid Engine: uses the Grid Engine software to submit jobs (http://gridscheduler.sourceforge.net)
 * <li>
 * GNU Parallel: uses GNU Parallel to run jobs in parallel on multiple machines
 * (http://www.gnu.org/software/parallel/)
 * <li>
 * Make: uses Make (with the -j option) to run jobs in parallel on a single machine
 * <li>
 * Local Sequential: simply runs all jobs serially, one at a time.
 * <li>
 * Local Parallel: simply runs all jobs in parallel, with max number of processes running at any given time equal to
 * number of processor cores.
 * </ol>
 * @author Eli Kahn
 * @version 1.0
 *
 * Note: This is a modified version of BatchSubmission.java from projects/osirisrex/ola/altwg/trunk/java-tools
 *       svn revision 59134, changed to make Bigmap work with the SBMT
 *
 */
@Deprecated
public class DistributedGravityBatchSubmission {


    public static boolean runProgramAndWait(String program) throws IOException, InterruptedException {
        return runProgramAndWait(program, null);
    }

    public static boolean runProgramAndWait(String program, File workingDirectory) throws IOException,
            InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(program.split("\\s+"));
        processBuilder.directory(workingDirectory);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        System.out.printf("Output of running %s is:\n", program);
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

        int exitStatus = process.waitFor();
        System.out.println("Program " + program + " finished with status: " + exitStatus);
        br.close();
        process.destroy();

        if (exitStatus != 0) {
            System.out.println("Terminating since subprogram failed.");
            System.exit(exitStatus);
        }

        return exitStatus == 0;
    }

    private static boolean runBatchSubmitProgramGridEngine(ArrayList<String> commandList, String workingDir) throws InterruptedException,
            IOException {
        // Create a text file for input to qsub making use of qsub's job array
        // option
        File temp = File.createTempFile("altwg-batch-list", ".bash", null);

        FileWriter ofs = new FileWriter(temp);
        BufferedWriter out = new BufferedWriter(ofs);
        for (int i = 1; i <= commandList.size(); ++i)
            out.write("if [ $SGE_TASK_ID -eq " + i + " ]; then \n " + commandList.get(i - 1) + "\n exit $?\nfi\n");
        out.close();

        String batchSubmitCommand;
        if (workingDir != null) {

            //user specified working directory
             batchSubmitCommand = "qsub -S /bin/bash -V -wd " + workingDir + " -sync y -t 1-" + commandList.size() + " "
                    + temp.getAbsolutePath();

        } else {
            //null working directory. Assume want to work in current working directory
             batchSubmitCommand = "qsub -S /bin/bash -V -cwd -sync y -t 1-" + commandList.size() + " "
                    + temp.getAbsolutePath();

        }

        boolean success = runProgramAndWait(batchSubmitCommand);

        if (success) {
            // If no error, delete qsub output and error files
            File[] filelist = new File(".").listFiles();
            for (File f : filelist) {
                if (f.getName().startsWith(temp.getName()))
                    f.delete();
            }
        }

        return success;
    }

    /**
     * Call the grid engine using the parallel environment that has been configured for it.
     * The batchType enumeration should contain a number specifying the number of CPUs needed per job.
     * Useful when trying to run memory intensive programs such as DistributedGravity.java.
     * @param commandList
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private static boolean runBatchSubmitProgramGridEngineLimitSlots(ArrayList<String> commandList, String workingDir, BatchType batchType)
            throws InterruptedException,
            IOException {
        // Create a text file for input to qsub making use of qsub's job array
        // option
        File temp = File.createTempFile("altwg-batch-list", ".bash", null);

        FileWriter ofs = new FileWriter(temp);
        BufferedWriter out = new BufferedWriter(ofs);
        for (int i = 1; i <= commandList.size(); ++i)
            out.write("if [ $SGE_TASK_ID -eq " + i + " ]; then \n " + commandList.get(i - 1) + "\n exit $?\nfi\n");
        out.close();

        String batchString = batchType.toString();
        String[] tempS = batchString.split("_");
        //default to using 6 cpus per job.
        String cpus = "6";
        if (tempS.length == 3) {
            cpus = tempS[2];
        }

        String batchSubmitCommand;
        if (workingDir != null) {
            batchSubmitCommand = "qsub -pe ocmp " + cpus + " -S /bin/bash -V -wd " + workingDir + " -sync y -t 1-" + commandList.size() + " "
                    + temp.getAbsolutePath();
        } else {
            batchSubmitCommand = "qsub -pe ocmp " + cpus + " -S /bin/bash -V -cwd -sync y -t 1-" + commandList.size() + " "
                    + temp.getAbsolutePath();
        }

        boolean success = runProgramAndWait(batchSubmitCommand);

        if (success) {
            // If no error, delete qsub output and error files
            File[] filelist = new File(".").listFiles();
            for (File f : filelist) {
                if (f.getName().startsWith(temp.getName()))
                    f.delete();
            }
        }

        return success;
    }

    private static boolean runBatchSubmitProgramParallel(ArrayList<String> commandList) throws InterruptedException,
            IOException {
        // Create a text file with all the commands that should be run, one per
        // line
        File temp = File.createTempFile("altwg-batch-list", ".tmp", null);
        FileUtil.saveList(commandList, temp.getAbsolutePath());

        // Now submit all these batches GNU Parallel
        String batchSubmitCommand = "parallel -v -a " + temp.getAbsolutePath();

        return runProgramAndWait(batchSubmitCommand);
    }

    private static boolean runBatchSubmitProgramLocalMake(ArrayList<String> commandList) throws InterruptedException,
            IOException {
        // Create a Makefile and run the tasks in parallel with the -j option
        File temp = File.createTempFile("altwg-batch-list", ".tmp", null);

        FileWriter ofs = new FileWriter(temp);
        BufferedWriter out = new BufferedWriter(ofs);
        out.write("all : ");
        for (int i = 0; i < commandList.size(); ++i)
            out.write("job" + i + " ");
        out.write("\n");
        for (int i = 0; i < commandList.size(); ++i) {
            out.write("job" + i + " :\n");
            out.write("\t" + commandList.get(i));
            out.write("\n");
        }
        out.close();

        int cores = Runtime.getRuntime().availableProcessors();
        String batchSubmitCommand = "make -k -j " + cores + " -f " + temp.getAbsolutePath() + " all";

        return runProgramAndWait(batchSubmitCommand);
    }

    private static boolean runBatchSubmitProgramLocalParallel(ArrayList<String> commandList, final String workingDir) throws IOException,
            InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        final AtomicBoolean successful = new AtomicBoolean(true);
        Collection<Future<?>> futures = new LinkedList<Future<?>>();
        for (final String command : commandList) {
            Future<?> future = executor.submit(new Runnable() {
                public void run() {
                    try {
                        File workingFile = new File(workingDir); // twupy1
                        if (!runProgramAndWait(command, workingFile))
                            successful.set(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            futures.add(future);
        }

        try {
            // Wait for all tasks to end (get blocks if necessary)
            for (Future<?> future : futures)
                future.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        executor.shutdown();

        return successful.get();
    }

    private static boolean runBatchSubmitProgramLocalSequential(ArrayList<String> commandList, String workingDir) throws IOException,
            InterruptedException {
        boolean successful = true;
        for (String command : commandList) {
            if (workingDir != null) {
                File workingFile = new File(workingDir);
                if (!runProgramAndWait(command, workingFile))
                    successful = false;
            } else {
                if (!runProgramAndWait(command))
                    successful = false;
            }
        }

        return successful;
    }

    public static boolean runBatchSubmitinDir(ArrayList<String> commandList, String workingDir, BatchType batchType) throws InterruptedException, IOException {

        switch (batchType) {
        case GRID_ENGINE:
            if (System.getenv("SGE_ROOT") == null) {
                if (workingDir != null) {
                    System.out.println("Cannot run in grid engine. Need to run in local make. Cannot run in specified working Dir");
                }
                return runBatchSubmitProgramLocalMake(commandList);
            }
            else
                return runBatchSubmitProgramGridEngine(commandList, workingDir);

        case GRID_ENGINE_6:
        case GRID_ENGINE_4:
        case GRID_ENGINE_3:
        case GRID_ENGINE_2:
            if (System.getenv("SGE_ROOT") == null) {
                if (workingDir != null) {
                    System.out.println("Cannot run in grid engine.Cannot run in specified working Dir using localmake");
                }
                return runBatchSubmitProgramLocalMake(commandList);
            }
            else
                return runBatchSubmitProgramGridEngineLimitSlots(commandList, workingDir, batchType);

        case GNU_PARALLEL:
            return runBatchSubmitProgramParallel(commandList);

        case LOCAL_PARALLEL_MAKE:
            if (workingDir != null) {
                System.out.println("Cannot run in specified working Dir using " + batchType.toString());
            }
            return runBatchSubmitProgramLocalMake(commandList);

        case LOCAL_PARALLEL:
            return runBatchSubmitProgramLocalParallel(commandList, workingDir);
//          if (workingDir != null) {
//              System.out.println("Cannot run in specified working Dir using " + batchType.toString());
//          }
//          return runBatchSubmitProgramLocalMake(commandList);

        case LOCAL_SEQUENTIAL:
            return runBatchSubmitProgramLocalSequential(commandList, workingDir);

        default:
            return false;

        }
    }

    public static boolean runBatchSubmitPrograms(ArrayList<String> commandList, BatchType batchType)
            throws InterruptedException, IOException {

        boolean result = runBatchSubmitinDir(commandList, null, batchType);

        // if (batchType == BatchType.GRID_ENGINE) {
        // // If Grid Engine is not present on machine, use parallel make
        // // instead
        // if (System.getenv("SGE_ROOT") == null)
        // return runBatchSubmitProgramLocalMake(commandList);
        // else
        // return runBatchSubmitProgramGridEngine(commandList);
        // }
        // else if (batchType == BatchType.GNU_PARALLEL)
        // return runBatchSubmitProgramParallel(commandList);
        // else if (batchType == BatchType.LOCAL_PARALLEL_MAKE)
        // return runBatchSubmitProgramLocalMake(commandList);
        // else if (batchType == BatchType.LOCAL_PARALLEL)
        // return runBatchSubmitProgramLocalParallel(commandList);
        // else if (batchType == BatchType.LOCAL_SEQUENTIAL)
        // return runBatchSubmitProgramLocalSequential(commandList);
        // return false;

        return result;
    }

    // twupy1: Added for integration with SBMT
    public static boolean runBatchSubmitPrograms(ArrayList<String> commandList, String rootDir, BatchType batchType)
            throws InterruptedException, IOException {

        boolean result = runBatchSubmitinDir(commandList, rootDir, batchType);

        return result;
    }
}
