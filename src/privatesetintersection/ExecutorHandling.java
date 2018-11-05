package privatesetintersection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import protocol.Log;

public class ExecutorHandling {
	
	public interface WaitForThreads <T>
	{
		public void function(Future<T> future);
	}
	
	public interface CalculateParallelForListElemenets<T,U>
	{
		public U function(T t);
	}
	
	/**
	 * Waits for Callables to be finished and executes a function on their futures.
	 * @param futures
	 * @param function lambda function of what to do with the results of given futures
	 */
	public static <T> void waitForThreads(List<Future<T>> futures, WaitForThreads function)
	{
		//-------------- Wait until all tasks are finished then end the executor and with that the threads ------------
		// i and k used to keep track of how many statements have been blinded
		Logger logger = Log.getLogger();
		logger.info("Total Number of Elements to process: " + futures.size());
		int i = 0;
		int k = 10000;
		// For every task wait until it is done
		for(Future<T> future : futures)
		{
			// Waits until a task is done and the future is returned
			function.function(future);

			
			// Every 10k statements output how many statements have been processed
			// Keep track of progress
			i++;
			if(i >= k)
			{
				logger.info("Number of Elements processed: " + i);
				k = k + 10000;
			}
		}
	}

	/**
	 * Parallel executes a function of all elements of a given list. 
	 * Creates a threadpool for the parallel execution. The number of threads equals double the amount of available processors.
	 * @param list
	 * @param function to be executed on elements of the list
	 * @param waitThreads function to be executed on results of first function (futures of callabels)
	 */
	public static <T,U> void calculateParallelForListElements(List<T> list, CalculateParallelForListElemenets<T,U> function, WaitForThreads waitThreads)
	{
		// Create a fixed number of threads handled by executor 
		// Number of threads equals the number of available processors
		ExecutorService executor = Executors.newFixedThreadPool(2*Runtime.getRuntime().availableProcessors());
		// A list of results from tasks given to threads to compute in parallel
		// A future is filled once this task is completed as it is the return value
		// Allows to detect when a task has finished
		List<Future<U>> futures = new ArrayList<Future<U>>();
		// For every statement generate a task which calculates the signature for it
		// Then puts the signature into the BF
		for(T statement : list)
		{	
			// Define a task which can be given to the executor which handles the threads
			// Callables have a return (future)
			Callable<U> task = new Callable<U>()
			{
				@Override
				public U call()
				{
					return function.function(statement);
				}
			};
			// Run the above defined task and read out its result
			Future<U> f = executor.submit(task);
			// Add the return to the list of futures
			futures.add(f);
		}
		
		waitForThreads(futures, waitThreads);
		
		// Threads are no longer needed
		// End the executor and therefore also the threads
		executor.shutdown();
	}
}
