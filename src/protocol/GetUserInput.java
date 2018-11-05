package protocol;

import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import knowledgegraphpartitioning.Strategies;

/**
 * Class containing methods used to get input from the user
 * 
 * runAllSteps can be set to true to skip asking any user input and use default values.
 * 
 * @author ---
 *
 */
public class GetUserInput 
{
	private static Scanner scanner;
	private static boolean runAllSteps = false;
	
	/**
	 * Asks the user if the next step should be run or skipped.
	 * User has to enter "yes" or "no" otherwise he is asked for input an other time
	 * 
	 * @param message informing the user about the step that he can choose to skip
	 * @return boolean true if user wants to run step
	 */
	public static boolean runStep(String message)
	{
		Logger logger = Log.getLogger();
		boolean runStep;
		if(runAllSteps)
		{
			runStep = true;
		} else
		{
			logger.info(message);
			String input = scanner.next();
			if(input.equals("yes"))
			{
				runStep = true;
				logger.info("Step is run.");
			} else if(input.equals("no"))
			{
				runStep = false;
				logger.info("Step is skipped.");
			} else
			{
				logger.info("Not a valid input.");
				runStep = runStep(message);
			}
		}
		
		return runStep;
	}
	
	/**
	 * Asks the user if he wants to continue running the program.
	 * Terminates if the user inputs "no". Continues if input is "yes".
	 * Any other input leads to the user being asked again.
	 */
	public static void continueProtocol()
	{
		Logger logger = Log.getLogger();
		if(runAllSteps)
		{
			return;
		} else
		{
			logger.info("Would you like to keep going with the protocol? [yes, no]");
			String input = scanner.next();
			if(input.equals("yes"))
			{
				return;
			} else if(input.equals("no"))
			{
				logger.info("The protocol is not being continued.");
				System.exit(0);
			} else
			{
				logger.info("Not a valid input.");
				continueProtocol();
			}
		}
	}
	
	/**
	 * Asks the user to enter a IP.
	 * IP needs to be of the form X.X.X.X where X can be 1 to 3 digits each
	 * @return IP as String
	 */
	public static String askIP()
	{
		Logger logger = Log.getLogger();
		
		String ip = "127.0.0.1";
		if(runAllSteps)
		{
			return ip;
		} else
		{
			boolean ipEntered = false;
			while(!ipEntered)
			{
				logger.info("Enter IP address to connect to.");
				ip = scanner.next();
				
				Pattern p = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
				Matcher m = p.matcher(ip);
				if(m.find())
				{
					ipEntered = true;
				}
			}
		}
		logger.info("Entered IP: " + ip);
		return ip;
	}
	
	/**
	 * Asks the user to enter a false positive probability for a Bloom filter.
	 * Needs to be a number bigger than 0 and smaller then 1.
	 * @return double false positive probability
	 */
	public static double getBloomFilterFPP()
	{
		Logger logger = Log.getLogger();
		
		double fpp = 1;
		if(runAllSteps)
		{
			fpp = 0.03;
		} else
		{
			boolean fppEntered = false;
			while(!fppEntered)
			{
				logger.info("Enter wished false positive probability for Bloom filter. Must be 0<fpp<=1.");
				String fppString = scanner.next();
				try
				{
					fpp = Double.valueOf(fppString);
					if(fpp > 0 && fpp <= 1)
					{
						fppEntered = true;
					}
				} catch(NumberFormatException e)
				{
					
				}
			}
		}
		logger.info("Entered fpp: " + fpp);
		return fpp;
	}
	
	/**
	 * Asks the user to enter which entropies should be computed.
	 * @return unfilterd LinkedList<String> with all user inputs.
	 */
	public static LinkedList<String> entropiesToCompute()
	{
		Logger logger = Log.getLogger();
		
		LinkedList<String> input = new LinkedList<String>();
		
		if(runAllSteps)
		{
			input.add("all");
			return input;
		}
		
		logger.info("Which Entropies should be calculated? \n"
				+ "'all' to compute all entropies, \n"
				+ "'1' to compute desc, \n"
				+ "'2' to compute classif, \n"
				+ "'3' to comupte descm, \n"
				+ "'4' to compute descmp, \n"
				+ "'5' to compute econn, \n"
				+ "'6' to compute resource, \n"
				+ "'7' to compute subject, \n"
				+ "'8' to compute predicate, \n"
				+ "'9' to compute literal, \n"
				+ "'done' to end entering numbers");
		
		while(true)
		{
			String i = scanner.next();
			if(i.equals("all") || i.equals("done"))
			{
				input.add(i);
				break;
			} else
			{
				input.add(i);
			}
		}
		
		return input;
	}
	
	/**
	 * Asks the user to enter how many secrets are to be shared in K out of N Oblivious Transfer.
	 * @return Integer k number of secrets to be shared
	 */
	public static int askObliviousTransferK()
	{
		Logger logger = Log.getLogger();
		
		int k = 1;
		if(runAllSteps)
		{
			k = 5;
		} else
		{
			boolean entered = false;
			while(!entered)
			{
				logger.info("Enter number of model parts to be shared via oblivious transfer. Must be an integer <= 1.");
				String kString = scanner.next();
				try
				{
					k = Integer.valueOf(kString);
					if(k > 0)
					{
						entered = true;
					}
				} catch(NumberFormatException e)
				{
					
				}
			}
		}
		logger.info("Entered k: " + k);
		return k;
	}
	
	/**
	 * Asks the user to enter a strategy with which a model is to be partitioned.
	 * 
	 * @return Strategies (Enum) chosen strategy
	 */
	public static Strategies askPartitioningStrategy()
	{
		Logger logger = Log.getLogger();
		
		if(runAllSteps)
		{
			return Strategies.BALANCEDDBSCAN;
		} else
		{
			Strategies strategy = null;
			boolean entered = false;
			while(!entered)
			{
				logger.info("Enter a partitioning strategy. 'balancedDBSCAN', 'DBSCAN', 'range', 'resource'");
				String strategyString = scanner.next();
				switch (strategyString)
				{
					case "balancedDBSCAN":
						strategy = Strategies.BALANCEDDBSCAN;
						entered = true;
						break;
					case "DBSCAN":
						strategy = Strategies.DBSCAN;
						entered = true;
						break;
					case "range":
						strategy = Strategies.RANGE;
						entered = true;
						break;
					case "resource":
						strategy = Strategies.RESOURCE;
						entered = true;
						break;
					default:
						
				}
			}
			return strategy;
		}
	}
	
	/**
	 * Asks the user to enter a number for minimum statements used in DBSCAN partitioning
	 * @return Integer minstatements
	 */
	public static int askDBSCANminStatements()
	{
		Logger logger = Log.getLogger();
		
		int k = 1;
		if(runAllSteps)
		{
			k = 10;
		} else
		{
			boolean entered = false;
			while(!entered)
			{
				logger.info("Enter number of minimum statements for DBSCAN. Must be an integer <= 1.");
				String string = scanner.next();
				try
				{
					k = Integer.valueOf(string);
					if(k > 0)
					{
						entered = true;
					}
				} catch(NumberFormatException e)
				{
					
				}
			}
		}
		logger.info("Entered minimum Statements: " + k);
		return k;
	}
	
	/**
	 * Asks the user to enter a range to be used in range based partitioning.
	 * Must be an integer bigger or equal to 1
	 * @return Intger range
	 */
	public static int askRange()
	{
		Logger logger = Log.getLogger();
		
		int range = 1;
		if(runAllSteps)
		{
			range = 5;
		} else
		{
			boolean entered = false;
			while(!entered)
			{
				logger.info("Enter a range for neighbor partitioning. Must be an integer <= 1.");
				String rangeString = scanner.next();
				try
				{
					range = Integer.valueOf(rangeString);
					if(range > 0)
					{
						entered = true;
					}
				} catch(NumberFormatException e)
				{
					
				}
			}
		}
		logger.info("Entered range: " + range);
		return range;
	}
	
	/**
	 * Asks the User to enter a path to a folder.
	 * @return path to folder
	 */
	public static String askFolder()
	{
		Logger logger = Log.getLogger();
		
		if(runAllSteps)
		{
			return "D:\\Uni_6_3_2017\\Masterarbeit\\Datasets\\Buyer\\Obtained\\";
		} else
		{
			logger.info("Enter the path to a folder where Models should be written to.");
			String folder = scanner.next();
			return folder;
		}
	}
	
	
	public static void setRunAllSteps(boolean runAll)
	{
		runAllSteps = runAll;
	}
	
	public static void setScanner(Scanner s)
	{
		scanner = s;
	}

}
