package edu.jhuapl.sbmt.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadService
{
	private static ExecutorService executorService;

	public static void initialize(int poolSize) {
		ThreadService.executorService = Executors.newFixedThreadPool(poolSize);
	}

	public static <O extends Object>Future<O> submitTask(Callable<O> task) {
		Future<O> result = ThreadService.executorService.submit(task);
		return result;
	}

	public static <O extends Object>List<Future<O>> submitAll(List<Callable<O>> tasks){
		List<Future<O>> results = new ArrayList<Future<O>>();
		try
		{
			results = ThreadService.executorService.invokeAll(tasks);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	public static <O extends Object> List<Future<List<O>>> submitAllLists(List<Callable<List<O>>> tasks){
		List<Future<List<O>>> results = new ArrayList<Future<List<O>>>();
		try
		{
			results = ThreadService.executorService.invokeAll(tasks);
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	public static void close() {
		ThreadService.executorService.shutdown();
	}

	public static <O extends Object> List<O> getFutureResult(List<Future<O>> resultList)
	{
		List<O> outputs = new ArrayList<O>();
		for (int i = 0; i < resultList.size(); i++)
		{
			Future<O> future = resultList.get(i);

			try
			{
				O item = future.get();
				outputs.add(item);
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (ExecutionException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return outputs;
	}

	public static <O extends Object> List<O> getFutureResults(List<Future<List<O>>> resultList)
	{
		List<O> outputs = new ArrayList<O>();
		for (int i = 0; i < resultList.size(); i++)
		{
			Future<List<O>> future = resultList.get(i);

			try
			{
				List<O> items = future.get();
				Iterator<O> it = items.iterator();
				while (it.hasNext())
				{
					O item = (O) it.next();
					outputs.add(item);
				}
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (ExecutionException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		return outputs;
	}

	public static  <O extends Object> List<O> getCallableResult(Callable<O> task) throws Exception
	{
		List<O> outputs = new ArrayList<O>();
		O item = task.call();
		outputs.add(item);
		return outputs;
	}

	public static  <O extends Object> List<O> getCallableResults(Callable<List<O>> task) throws Exception
	{
		List<O> outputs = new ArrayList<O>();

		List<O> items = task.call();
		Iterator<O> it = items.iterator();
		while (it.hasNext())
		{
			O injury = (O) it.next();
			outputs.add(injury);
		}

		return outputs;
	}

}
