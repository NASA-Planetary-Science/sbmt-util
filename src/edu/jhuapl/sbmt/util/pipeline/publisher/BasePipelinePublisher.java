package edu.jhuapl.sbmt.util.pipeline.publisher;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.internal.Lists;

import edu.jhuapl.sbmt.util.pipeline.operator.IPipelineOperator;
import edu.jhuapl.sbmt.util.pipeline.subscriber.IPipelineSubscriber;

public class BasePipelinePublisher<O extends Object> implements IPipelinePublisher<O>
{

	protected List<O> outputs;
	protected IPipelineSubscriber<O> subscriber;

	public BasePipelinePublisher()
	{
		outputs = Lists.newArrayList();
	}


	@Override
	public void run() throws IOException, Exception
	{
		publish();
	}

	@Override
	public void publish() throws IOException, Exception
	{
		subscriber.receive(outputs);
	}

	@Override
	public List<O> getOutputs()
	{
		return outputs;
	}

	@Override
	public IPipelinePublisher<O> subscribe(IPipelineSubscriber<O> subscriber)
	{
		this.subscriber = subscriber;
		this.subscriber.setPublisher(this);
		return this;
	}

	@Override
	public <T extends Object> IPipelineOperator<O, T> operate(IPipelineOperator<O, T> operator)
	{
		this.subscriber = operator;
		this.subscriber.setPublisher(this);
		return operator;
	}
}
