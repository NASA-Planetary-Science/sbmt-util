package edu.jhuapl.sbmt.util.pipeline.subscriber;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import edu.jhuapl.sbmt.util.pipeline.IPipelineComponent;
import edu.jhuapl.sbmt.util.pipeline.publisher.IPipelinePublisher;

public interface IPipelinePairSubscriber<InputType1 extends Object, InputType2 extends Object> extends IPipelineComponent
{

	public void receive(List<Pair<InputType1, InputType2>> items) throws IOException, Exception;


	public void setPublisher(IPipelinePublisher<Pair<InputType1, InputType2>> publisher);
}
