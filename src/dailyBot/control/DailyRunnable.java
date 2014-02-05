package dailyBot.control;

import java.util.concurrent.atomic.AtomicLong;

public abstract class DailyRunnable implements Runnable
{
    private AtomicLong lastUpdate = new AtomicLong(System.currentTimeMillis());
    private final String name;
    private final long updateInterval;
    private final boolean runWhileMarketClosed;
    private AtomicLong lastRun = new AtomicLong();
    
    public DailyRunnable(String name, long updateInterval, boolean runWhileMarketClosed)
    {
    	this.name = name;
    	this.updateInterval = updateInterval;
    	this.runWhileMarketClosed = runWhileMarketClosed;
    }
    
    public abstract void runOnce();
    
    public final void run()
    {
    	lastRun.set(System.currentTimeMillis());
    	if(runWhileMarketClosed || (!DailyBotMain.marketClosed()))
    		runOnce();
    	setLastUpdate(System.currentTimeMillis());
    }
    
    private void setLastUpdate(long time)
    {
        lastUpdate.getAndSet(time);
    }

    public long getLastUpdate()
    {
        return lastUpdate.getAndAdd(0);
    }

    public long getUpdateInterval()
    {
        return updateInterval;
    }
    
    public long getLastRun()
    {
    	return lastRun.get();
    }

    public String getName()
    {
    	return name;
    }
}