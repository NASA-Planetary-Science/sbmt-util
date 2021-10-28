package edu.jhuapl.sbmt.util.pipeline.subscriber;

import java.io.IOException;
import java.util.List;

import edu.jhuapl.sbmt.util.pipeline.IPipelineComponent;
import edu.jhuapl.sbmt.util.pipeline.publisher.IPipelinePublisher;

public interface IPipelineSubscriber<InputType extends Object> extends IPipelineComponent
{

	public void receive(List<InputType> items) throws IOException, Exception;


	public void setPublisher(IPipelinePublisher<InputType> publisher);
}
