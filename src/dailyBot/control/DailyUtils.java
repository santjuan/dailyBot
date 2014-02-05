package dailyBot.control;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DailyUtils {

	static class SafeLock extends ReentrantReadWriteLock.WriteLock
	{
	    private static final long serialVersionUID = -2449883455415346160L;
	
	    ReentrantReadWriteLock readWriteLock;
	
	    public SafeLock(ReentrantReadWriteLock readWriteLock)
	    {
	        super(readWriteLock);
	        this.readWriteLock = readWriteLock;
	    }
	
	    @Override
	    public void lock()
	    {
	        int lockNumber = readWriteLock.getReadHoldCount();
	        while(readWriteLock.getReadHoldCount() != 0)
	            readWriteLock.readLock().unlock();
	        super.lock();
	        while(readWriteLock.getReadHoldCount() < lockNumber)
	            readWriteLock.readLock().lock();
	    }
	}

	public static Lock getSafeWriteLock(ReentrantReadWriteLock readWriteLock)
	{
	    return new SafeLock(readWriteLock);
	}

	private static Random random = new Random();
	
	public static void sleep(long millis)
	{
	    try
	    {
	        millis += random.nextInt(101);
	        Thread.sleep(millis);
	    }
	    catch(InterruptedException e)
	    {
	        String stackTrace = "";
	        for(StackTraceElement stackTraceElement : e.getStackTrace())
	            stackTrace += "\n" + stackTraceElement;
	        DailyLog.logError(e.getMessage() + " error de interrupcion en: " + stackTrace);
	    }
	}
}