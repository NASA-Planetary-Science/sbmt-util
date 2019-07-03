package edu.jhuapl.sbmt.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import edu.jhuapl.saavtk.util.ProgressStatusListener;

import altwg.util.BatchType;
import altwg.util.FileUtil;

/**
 * Contains concrete methods for submitting batch jobs to local machine.
 * So even if batchType says "GRID*" will default to local.
 *
 * @author espirrc1
 *
 */
public class SubmitLocalGravityJob { //implements BatchSubmitI {


	private ArrayList<String> commandList;
	private BatchType batchType;
	private int cores;
	private boolean showOutput = true;
	private String gridQueue = null;


	public SubmitLocalGravityJob(ArrayList<String> commandList, BatchType batchType) {
		this.commandList = commandList;
		this.batchType = batchType;
//		System.out.println("BatchSubmitLocal: BatchSubmitLocal: batch type is " + batchType);
		cores = Runtime.getRuntime().availableProcessors();
	}

	/**
	 * STDOUT from commandlist will not be printed.
	 */
	public void noScreenOutput() {
		showOutput = false;
	}

	/**
	 * Set the grid queue to use when calling the grid engine.
	 * @param gridQueue
	 */
	public void setGQ(String gridQueue) {
		this.gridQueue = gridQueue;
	}

	public void limitCores(int limit)
	{
		cores = Math.min(limit, Runtime.getRuntime().availableProcessors());
//		System.out.println("limiting number of local cores to:" + cores);
	}

	/**
	 * Evaluate working directory string. Set to null if empty.
	 * @param workingDir
	 * @return
	 */
	private String emptyToNull(String workingDir) {
		if (workingDir != null) {
			if (workingDir.length() < 1) {
				return null;
			} else {
			}
		}
		return workingDir;
	}

	/**
	 * Run batch submission in the specified working directory. Uses current working directory if workingDir is
	 * null.
	 * @param workingDir
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public boolean runBatchSubmitinDir(String workingDir, ProgressStatusListener listener, int maxPoints) throws InterruptedException, IOException {

//		System.out.println("BatchSubmitLocal: runBatchSubmitinDir:");
		//evaluate workingDir. If empty string then set to null;
		workingDir = emptyToNull(workingDir);

		switch (batchType) {
		case GRID_ENGINE:
		case GRID_ENGINE_8:
		case GRID_ENGINE_6:
		case GRID_ENGINE_4:
		case GRID_ENGINE_3:
		case GRID_ENGINE_2:
				System.out.println("Can't submit grid engine batch type in "
						+ "BatchSubmitLocal class. Defaulting to use local make");
				return runBatchSubmitProgramLocalMake(commandList, null, maxPoints);

		case GNU_PARALLEL:
			return runBatchSubmitProgramParallel(commandList);

		case LOCAL_PARALLEL_MAKE:
			if (workingDir != null) {
				System.out.println("Cannot run in specified working Dir using " + batchType.toString());
			}
			return runBatchSubmitProgramLocalMake(commandList, listener, maxPoints);

		case LOCAL_PARALLEL:
			if (workingDir != null) {
				System.out.println("Cannot run in specified working Dir using " + batchType.toString());
			}
			return runBatchSubmitProgramLocalMake(commandList, null, maxPoints);

		case LOCAL_SEQUENTIAL:
			return runBatchSubmitProgramLocalSequential(commandList, workingDir);

		default:
			return false;

		}
	}

	public boolean runProgramAndWait(String program, ProgressStatusListener listener, int maxPoints) throws IOException, InterruptedException {
		return runProgramAndWait(program, null, listener, maxPoints);
	}


	public boolean runProgramAndWait(String program, File workingDirectory, ProgressStatusListener listener, int maxPoints) throws IOException,
	InterruptedException {

		System.out.println("SubmitLocalGravityJob: runProgramAndWait: program is " + program + " and working directory " + workingDirectory);
//		return runAndWait(program, workingDirectory, showOutput);
		ProcessBuilder processBuilder = new ProcessBuilder(program.split("\\s+"));
		processBuilder.directory(workingDirectory);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();

		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		if (showOutput) {
//			System.out.printf("Output of running %s is:\n", program);
			while ((line = br.readLine()) != null) {
//				System.out.println(line);

				if (listener != null)
				{
					if (line.startsWith("Initialization"))
					{
//						System.out.println("SubmitLocalGravityJob: runProgramAndWait: starting");
						listener.setProgressStatus("Starting Gravity Generation....", 1);
					}
					if (line.startsWith("Time to evaluate total"))
					{
//						System.out.println("SubmitLocalGravityJob: runProgramAndWait: breaking");
//						listener.setProgressStatus("Done!", 100);
						break;
					}
					if (line.startsWith("Time"))
					{
						int progress = Integer.parseInt(line.split(" ")[4]);
						int percentage = (int)((float)progress*100.0/(float)maxPoints);
//						System.out.println("SubmitLocalGravityJob: runProgramAndWait: progress is " + progress + " and max points " + maxPoints);
//						System.out.println("SubmitLocalGravityJob: runProgramAndWait: setting percentage to " + (int)((float)progress*100.0/(float)maxPoints));
						listener.setProgressStatus(line, percentage);
					}

				}
			}
		} else {
			System.out.printf("Output of running %s disabled.\n",program);
		}

		int exitStatus = process.waitFor();
		System.out.println("Program " + program + " finished with status: " + exitStatus);
		br.close();
		process.destroy();

		if (exitStatus != 0) {
			System.out.println("Terminating since subprogram failed.");
//			System.exit(exitStatus);
		}

		return exitStatus == 0;
	}

	/**
	 * Static method that can also be used when one does not need to specify batchType. It will be run on machine
	 * that is executing the code. User takes responsibility for errors if the program itself is expecting
	 * a distributed process to exist.
	 * @param program
	 * @param workingDirectory
	 * @param showOutput
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean runAndWait(String program, File workingDirectory, boolean showOutput) throws IOException,
	InterruptedException {

		return false;
	}

	private boolean runBatchSubmitProgramLocalMake(ArrayList<String> commandList, ProgressStatusListener  listener, int maxPoints) throws InterruptedException,
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

		String batchSubmitCommand = "make -k -j " + cores + " -f " + temp.getAbsolutePath() + " all";

		return runProgramAndWait(batchSubmitCommand, listener, maxPoints);
	}

	private boolean runBatchSubmitProgramParallel(ArrayList<String> commandList) throws InterruptedException,
	IOException {
		// Create a text file with all the commands that should be run, one per
		// line
		File temp = File.createTempFile("altwg-batch-list", ".tmp", null);
		FileUtil.saveList(commandList, temp.getAbsolutePath());

		// Now submit all these batches GNU Parallel
		String batchSubmitCommand = "parallel -v -a " + temp.getAbsolutePath();

		return runProgramAndWait(batchSubmitCommand, null, 0);
	}

	private boolean runBatchSubmitProgramLocalSequential(ArrayList<String> commandList, String workingDir) throws IOException,
	InterruptedException {

		//evaluate workingDir. If empty string then set to null;
		workingDir = emptyToNull(workingDir);

		boolean successful = true;
		for (String command : commandList) {
			if (workingDir != null) {
				File workingFile = new File(workingDir);
				if (!runProgramAndWait(command, workingFile, null, 0))
					successful = false;
			} else {
				if (!runProgramAndWait(command, null, 0))
					successful = false;
			}
		}

		return successful;
	}

}
