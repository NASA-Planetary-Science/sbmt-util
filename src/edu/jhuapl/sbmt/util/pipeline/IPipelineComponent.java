package edu.jhuapl.sbmt.util.pipeline;

import java.io.IOException;

public interface IPipelineComponent
{
	public void run() throws IOException, Exception;
}
