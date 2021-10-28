//package edu.jhuapl.sbmt.image.pipeline.publisher;
//
//import java.io.IOException;
//import java.util.List;
//
//import edu.jhuapl.sbmt.image.pipeline.IPipelineComponent;
//import edu.jhuapl.sbmt.image.pipeline.operator.ICompoundPipelineOperator;
//import edu.jhuapl.sbmt.image.pipeline.subscriber.ICompoundPipelineSubscriber;
//
//public interface ICompoundPipelinePublisher<OutputType1 extends Object, OutputType2 extends Object> extends IPipelineComponent
//{
//	public void publish() throws IOException, Exception;
//
//	public ICompoundPipelinePublisher acceptSubscription(ICompoundPipelineSubscriber<OutputType1, OutputType2> subscriber);
//
//	public ICompoundPipelineOperator accept(ICompoundPipelineOperator operator);
//
//	public List<OutputType1> getMainOutputs();
//
//	public List<OutputType2> getSecondaryOutputs();
//}
