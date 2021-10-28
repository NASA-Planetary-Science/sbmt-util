package edu.jhuapl.sbmt.util.pipeline.subscriber;

import java.io.IOException;
import java.util.List;

import edu.jhuapl.sbmt.util.pipeline.publisher.IPipelinePublisher;

public class Sink<O extends Object> implements IPipelineSubscriber<O>
{
	private IPipelinePublisher<O> publisher;
	private List<O> outputs;

	public static <O extends Object> Sink<O> of(List<O> outputs)
	{
		return new Sink<O>(outputs);
	}

	public Sink(List<O> outputs)
	{
		this.outputs = outputs;
	}

	@Override
	public void receive(List<O> items)
	{
		this.outputs.addAll(items);
	}

	@Override
	public void setPublisher(IPipelinePublisher<O> publisher)
	{
		this.publisher = publisher;
	}

	@Override
	public void run() throws IOException, Exception
	{
		publisher.run();
	}

}
