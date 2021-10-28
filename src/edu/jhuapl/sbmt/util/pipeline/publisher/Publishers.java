package edu.jhuapl.sbmt.util.pipeline.publisher;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class Publishers<O extends Object> extends BasePipelinePublisher<O>
{

	public Publishers(List<O> inputs)
	{
		outputs = new ArrayList<O>(inputs);
	}

	@SafeVarargs
	public static Publishers<Object> zip(Object... publishers)
	{
		List<Object> outputs = new ArrayList<Object>();
		for (Object publisher : publishers)
		{
			outputs.add(((IPipelinePublisher<Object>)publisher).getOutputs().get(0));
		}
		return new Publishers<Object>(outputs);
	}

//	@SafeVarargs
//	public static <O extends Object> Publishers<O> zip(IPipelinePublisher<O>... publishers)
//	{
//		List<O> outputs = new ArrayList<O>();
//		for (IPipelinePublisher<O> publisher : publishers)
//		{
//			outputs.add(publisher.getOutputs().get(0));
//		}
//		return new Publishers<O>(outputs);
//	}

	@SafeVarargs
	public static <O extends Object> Publishers<O> merge(IPipelinePublisher<? extends Object>... list)
	{
		List<O> outputs = new ArrayList<O>();
		for (IPipelinePublisher<? extends Object> publisher : list)
		{
			outputs.add((O) publisher.getOutputs().get(0));
		}
		return new Publishers<O>(outputs);
	}

	@SafeVarargs
	public static <O extends Object> Publishers<List<O>> mergeLists(IPipelinePublisher<? extends Object>... list)
	{
		List<List<O>> outputs = new ArrayList<List<O>>();
		for (IPipelinePublisher<? extends Object> publisher : list)
		{
			outputs.add((List<O>) publisher.getOutputs());
		}
		return new Publishers<List<O>>(outputs);
	}

	public static <InputType1 extends Object, InputType2 extends Object> IPipelinePublisher<Pair<InputType1, InputType2>> formPair(IPipelinePublisher<InputType1> input1, IPipelinePublisher<InputType2> input2)
	{
		BasePipelinePairPublisher<InputType1, InputType2> pub = new BasePipelinePairPublisher<InputType1, InputType2>() {


			@Override
			public List<Pair<InputType1, InputType2>> getOutputs()
			{
				outputs.clear();
				for (int i=0; i < input1.getOutputs().size(); i++)
				{
					outputs.add(Pair.of(input1.getOutputs().get(i), input2.getOutputs().get(i)));
				}
				return outputs;
			}
		};
		pub.getOutputs();
		return pub;
	}

	public static <InputType1 extends Object, InputType2 extends Object, InputType3 extends Object> IPipelinePublisher<Triple<InputType1, InputType2, InputType3>> formTriple(IPipelinePublisher<InputType1> input1, IPipelinePublisher<InputType2> input2, IPipelinePublisher<InputType3> input3)
	{
		BasePipelineTriplePublisher<InputType1, InputType2, InputType3> pub = new BasePipelineTriplePublisher<InputType1, InputType2, InputType3>() {


			@Override
			public List<Triple<InputType1, InputType2, InputType3>> getOutputs()
			{
				outputs.clear();
				for (int i=0; i < input1.getOutputs().size(); i++)
				{
					outputs.add(Triple.of(input1.getOutputs().get(i), input2.getOutputs().get(i), input3.getOutputs().get(i)));
				}
				return outputs;
			}
		};
		pub.getOutputs();
		return pub;
	}

}
