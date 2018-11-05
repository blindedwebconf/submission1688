package protocol;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Log 
{

	private static String path;
	private static Logger logger;
	
	public static Logger getLogger()
	{
		if(logger == null)
		{
			setupLogger();
		}
		return logger;
	}
	
	/**
	 * Sets up a logger
	 */
	private static void setupLogger()
	{
		logger = Logger.getLogger("LogFile");
		FileHandler fileHandler;
		
		try 
		{  
			fileHandler = new FileHandler(path);  
	        logger.addHandler(fileHandler);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fileHandler.setFormatter(formatter);    

	    } catch (SecurityException e) 
		{  
	        e.printStackTrace();  
	    } catch (IOException e) 
		{  
	        e.printStackTrace();  
	    }  

	    logger.info("Logger is set up."); 
	}
	
	public static void setPath(String logpath)
	{
		path = logpath;
	}
}
