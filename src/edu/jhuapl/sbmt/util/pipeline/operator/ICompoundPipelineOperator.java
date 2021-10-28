//package edu.jhuapl.sbmt.image.pipeline.operator;
//
//import java.io.IOException;
//import java.util.List;
//
//import edu.jhuapl.sbmt.image.pipeline.IPipelineComponent;
//import edu.jhuapl.sbmt.image.pipeline.publisher.ICompoundPipelinePublisher;
//import edu.jhuapl.sbmt.image.pipeline.subscriber.ICompoundPipelineSubscriber;
//
//public interface ICompoundPipelineOperator<InputType1 extends Object, InputType2 extends Object, OutputType1 extends Object, OutputType2 extends Object> extends ICompoundPipelinePublisher<InputType1, OutputType1>, ICompoundPipelineSubscriber<InputType1, InputType2>, IPipelineComponent
//{
//	public void processData() throws IOException, Exception;
//
//	public List<InputType1> getMainOutputs();
//
//	public List<OutputType1> getSecondaryOutputs();
//}