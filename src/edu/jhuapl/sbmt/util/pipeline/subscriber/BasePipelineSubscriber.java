package edu.jhuapl.sbmt.util.pipeline.subscriber;

import java.io.IOException;
import java.util.List;

import edu.jhuapl.sbmt.util.pipeline.publisher.IPipelinePublisher;

public class BasePipelineSubscriber<O extends Object> implements IPipelineSubscriber<O>
{
	protected IPipelinePublisher<O> publisher;


	public BasePipelineSubscriber()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws IOException, Exception
	{
		publisher.run();
	}

	@Override
	public void receive(List<O> items) throws IOException, Exception
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setPublisher(IPipelinePublisher<O> publisher)
	{
		this.publisher = publisher;
	}

}
