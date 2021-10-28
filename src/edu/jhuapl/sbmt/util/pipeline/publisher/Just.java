package edu.jhuapl.sbmt.util.pipeline.publisher;

import java.util.ArrayList;
import java.util.List;

public class Just<OutputType extends Object> extends BasePipelinePublisher<OutputType>
{
	public Just(OutputType input)
	{
		this.outputs = new ArrayList<OutputType>();
		outputs.add(input);
	}

	public Just(List<OutputType> inputs)
	{
		this.outputs = new ArrayList<OutputType>();
		outputs.addAll(inputs);
	}

	public static <OutputType extends Object> Just<OutputType> of(OutputType outputs)
	{
		return new Just<OutputType>(outputs);
	}

	public static <OutputType extends Object> Just<OutputType> of(List<OutputType> outputs)
	{
		return new Just<OutputType>(outputs);
	}
}
