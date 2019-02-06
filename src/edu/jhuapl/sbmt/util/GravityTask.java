package edu.jhuapl.sbmt.util;

import java.util.List;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import edu.jhuapl.saavtk.util.ProgressStatusListener;
import edu.jhuapl.sbmt.util.SBMTDistributedGravity.GravityValues;

public class GravityTask extends SwingWorker<Void, List<GravityValues>>
{
	SubmitLocalJob batchSubmit;
	String batchDir;
	int coresToUse;
	boolean keepGfiles;
	String outfilename;
	ProgressMonitor gravityLoadingProgressMonitor;
	GravityCompletionClosure closure;

	public GravityTask(SubmitLocalJob batchSubmit, String batchDir, int numCores, boolean keepGfiles, String outfilename, ProgressMonitor gravityLoadingProgressMonitor, GravityCompletionClosure closure)
	{
		this.batchSubmit = batchSubmit;
		this.batchDir = batchDir;
		this.coresToUse = numCores;
		this.keepGfiles = keepGfiles;
		this.outfilename = outfilename;
		this.gravityLoadingProgressMonitor = gravityLoadingProgressMonitor;
		this.closure = closure;
	}

	@Override
	protected Void doInBackground() throws Exception
	{
		batchSubmit.runBatchSubmitinDir(batchDir, new ProgressStatusListener()
		{

			@Override
			public void setProgressStatus(String status)
			{
				gravityLoadingProgressMonitor.setNote(status);
			}
		});

		return null;
	}

	@Override
	protected void done()
	{
		// TODO Auto-generated method stub
		super.done();
		closure.complete();

	}

}