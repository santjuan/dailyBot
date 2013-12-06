package dailyBot.control;

import java.util.concurrent.atomic.AtomicLong;

public abstract class DailyRunnable implements Runnable
{
    private AtomicLong lastUpdate = new AtomicLong(System.currentTimeMillis());
    private AtomicLong updateInterval = new AtomicLong();

    public void setLastUpdate(long time)
    {
        lastUpdate.getAndSet(time);
    }

    public long getLastUpdate()
    {
        return lastUpdate.getAndAdd(0);
    }

    public void setUpdateInterval(long time)
    {
        updateInterval.getAndSet(time);
    }

    public long getUpdateInterval()
    {
        return updateInterval.getAndAdd(0);
    }
}