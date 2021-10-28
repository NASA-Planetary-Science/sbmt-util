package edu.jhuapl.sbmt.util.pipeline.operator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.jhuapl.sbmt.util.pipeline.publisher.IPipelinePublisher;
import edu.jhuapl.sbmt.util.pipeline.subscriber.IPipelineSubscriber;

public class BasePipelineOperator<InputType, OutputType> implements IPipelineOperator<InputType, OutputType>
{
	protected List<InputType> inputs;
    protected List<OutputType> outputs = new ArrayList<OutputType>();
	protected IPipelinePublisher<InputType> publisher;
	protected IPipelineSubscriber<OutputType> subscriber;


	@Override
	public void processData() throws IOException, Exception
	{

	}

	@Override
	public void publish() throws IOException, Exception
	{
		subscriber.receive(outputs);
	}

	@Override
	public IPipelinePublisher<OutputType> subscribe(IPipelineSubscriber<OutputType> subscriber)
	{
		this.subscriber = subscriber;
		this.subscriber.setPublisher(this);
		return this;
	}

	@Override
	public <T extends Object> IPipelineOperator<OutputType, T> operate(IPipelineOperator<OutputType, T> operator)
	{
		this.subscriber = operator;
		this.subscriber.setPublisher(this);
		return operator;
	}

	@Override
	public void setPublisher(IPipelinePublisher<InputType> publisher)
	{
		this.publisher = publisher;
	}

	@Override
	public List<OutputType> getOutputs()
	{
		return outputs;
	}

	@Override
	public void run() throws IOException, Exception
	{
		publisher.run();
		if (subscriber == null) return;
		publish();
	}

	@Override
	public void receive(List<InputType> items) throws IOException, Exception
	{
		this.inputs = items;
		processData();
	}
}
