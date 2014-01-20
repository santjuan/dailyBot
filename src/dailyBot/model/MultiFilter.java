package dailyBot.model;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyThread;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;

public class MultiFilter implements Serializable
{
	private static final long serialVersionUID = -2010581534111176537L;

	protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock read = readWriteLock.readLock();
    protected final Lock write = DailyThread.getSafeWriteLock(readWriteLock);
    private boolean active = false;
    private boolean[][] activeArray = new boolean[StrategyId.values().length][Pair.values().length];
    private int[][] activeFilters = new int[StrategyId.values().length][Pair.values().length];
    private Filter[] filters = new Filter[0];
    private SignalProviderId id;
    
    public MultiFilter()
    {
    }
    
    public MultiFilter(SignalProviderId id)
    {
    	this.id = id;
    }
    
    public void startFilters(Filter[] filters)
    {
    	write.lock();
    	try
    	{
    		this.filters = filters;
    	}
    	finally
    	{
    		write.unlock();
    	}
    	for(Filter filter : this.filters)
    		filter.startFilter(id);
    }
    
    public boolean hasActive(StrategyId strategyId, Pair pair)
    {
        read.lock();
        try
        {
            return activeArray[strategyId.ordinal()][pair.ordinal()] && active;
        }
        finally
        {
            read.unlock();
        }
    }

    public void changeActive(StrategyId strategyId, Pair pair, boolean active)
    {
        write.lock();
        try
        {
            activeArray[strategyId.ordinal()][pair.ordinal()] = active;
        }
        finally
        {
            write.unlock();
        }
    }
    
    public void changeActiveFilter(StrategyId strategyId, Pair pair, int newValue)
    {
    	write.lock();
        try
        {
            activeFilters[strategyId.ordinal()][pair.ordinal()] = newValue;
        }
        finally
        {
            write.unlock();
        }
    }
    
    public boolean[][] getActiveArray()
    {
        read.lock();
        try
        {
            return activeArray;
        }
        finally
        {
            read.unlock();
        }
    }

    private boolean[][] fixSize(boolean[][] array)
    {
        int sizeX = StrategyId.values().length;
        int sizeY = Pair.values().length;
        if(array.length != sizeX || array.length == 0 || array[0].length != sizeY)
        {
            boolean[][] newArray = new boolean[sizeX][sizeY];
            for(int i = 0; i < Math.min(array.length, newArray.length); i++)
                if(newArray.length != 0 && array.length != 0)
                    for(int j = 0; j < Math.min(array[0].length, newArray[0].length); j++)
                        newArray[i][j] = array[i][j];
            return newArray;
        }
        else
            return array;
    }

    public void setActiveArray(boolean[][] activeArray)
    {
        write.lock();
        try
        {
            this.activeArray = fixSize(activeArray);
        }
        finally
        {
            write.unlock();
        }
    }
    
    public int[][] getActiveFilters()
    {
        read.lock();
        try
        {
            return activeFilters;
        }
        finally
        {
            read.unlock();
        }
    }

    private int[][] fixSize(int[][] array)
    {
        int sizeX = StrategyId.values().length;
        int sizeY = Pair.values().length;
        if(array.length != sizeX || array.length == 0 || array[0].length != sizeY)
        {
            int[][] newArray = new int[sizeX][sizeY];
            for(int i = 0; i < Math.min(array.length, newArray.length); i++)
                if(newArray.length != 0 && array.length != 0)
                    for(int j = 0; j < Math.min(array[0].length, newArray[0].length); j++)
                        newArray[i][j] = array[i][j];
            return newArray;
        }
        else
            return array;
    }

    public void setActiveFilters(int[][] activeFilters)
    {
        write.lock();
        try
        {
            this.activeFilters = fixSize(activeFilters);
        }
        finally
        {
            write.unlock();
        }
    }

    public boolean isActive()
    {
        read.lock();
        try
        {
            return active;
        }
        finally
        {
            read.unlock();
        }
    }

    public void setActive(boolean active)
    {
        write.lock();
        try
        {
            this.active = active;
        }
        finally
        {
            write.unlock();
        }
    }
    
    public SignalProviderId getId()
    {
    	read.lock();
    	try
    	{
    		return id;
    	}
    	finally
    	{
    		read.unlock();
    	}
    }

    public synchronized void setId(SignalProviderId id)
    {
    	write.lock();
    	try
    	{
    		this.id = id;
    	}
    	finally
    	{
    		write.unlock();
    	}
    }
    
    public boolean filter(SignalHistoryRecord record, boolean sendMessage)
    {
    	read.lock();
    	try
    	{
	    	if((!active) || (!activeArray[record.id.ordinal()][record.pair.ordinal()]))
	    		return false;
	    	int activeInt = activeFilters[record.id.ordinal()][record.pair.ordinal()];
	    	boolean or = (activeInt & 1) == 1;
	    	activeInt >>= 1;
	        if(filters.length == 0 || activeInt == 0)
	        	return false;
	        boolean ok = or ? false : true;
	        boolean any = false;
	        String message = "Intentando abrir " + id.toString() + ", " + record.id.toString() + ", "
	                + record.pair.toString() + ", " + record.pair.getCurrentPrice(record.buy) + ", "
	                + (record.buy ? "BUY" : "SELL");
	        message += "precio actual: " + record.pair.getCurrentPrice(record.buy) + "\n";
	        for(int i = 0; i < filters.length; i++)
	        {
	        	if((activeInt & 1) == 1)
	        	{
	        		any = true;
	        		boolean current = filters[i].filter(record);
	        		if(or)
	        			ok = ok || current;
	        		else
	        			ok = ok && current;
	        		message += filters[i] + ": " + current + "\n";
	        	}
	        	activeInt >>= 1;
	        }
	        if(sendMessage)
	        	DailyLog.logInfoWithTitle("dailybot-rangos", message);
	    	return any && ok;
    	}
    	finally
    	{
    		read.unlock();
    	}
    }
}