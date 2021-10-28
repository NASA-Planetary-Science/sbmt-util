package edu.jhuapl.sbmt.util.pipeline.subscriber;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

import edu.jhuapl.sbmt.util.pipeline.IPipelineComponent;
import edu.jhuapl.sbmt.util.pipeline.publisher.IPipelinePublisher;

public interface IPipelineTripleSubscriber<InputType1 extends Object, InputType2 extends Object, InputType3 extends Object> extends IPipelineComponent
{

	public void receive(List<Triple<InputType1, InputType2, InputType3>> items) throws IOException, Exception;


	public void setPublisher(IPipelinePublisher<Triple<InputType1, InputType2, InputType3>> publisher);
}
