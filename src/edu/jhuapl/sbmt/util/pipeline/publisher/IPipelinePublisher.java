package edu.jhuapl.sbmt.util.pipeline.publisher;

import java.io.IOException;
import java.util.List;

import edu.jhuapl.sbmt.util.pipeline.IPipelineComponent;
import edu.jhuapl.sbmt.util.pipeline.operator.IPipelineOperator;
import edu.jhuapl.sbmt.util.pipeline.subscriber.IPipelineSubscriber;

public interface IPipelinePublisher<OutputType extends Object> extends IPipelineComponent
{
	public void publish() throws IOException, Exception;

	public IPipelinePublisher<OutputType> subscribe(IPipelineSubscriber<OutputType> subscriber);

	public <T extends Object> IPipelineOperator<OutputType, T> operate(IPipelineOperator<OutputType, T> operator);

	public List<OutputType> getOutputs();
}
