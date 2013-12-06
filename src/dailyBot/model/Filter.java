package dailyBot.model;

import java.io.Serializable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.control.DailyThread;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;

public abstract class Filter implements Serializable
{
    private static final long serialVersionUID = 7582836081548863756L;

    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock read = readWriteLock.readLock();
    protected final Lock write = DailyThread.getSafeWriteLock(readWriteLock);
    private boolean active = false;
    private boolean[][] activeArray = new boolean[StrategyId.values().length][Pair.values().length];

    public boolean isActive(StrategyId startegyId, Pair pair)
    {
        read.lock();
        try
        {
            return activeArray[startegyId.ordinal()][pair.ordinal()] && active;
        }
        finally
        {
            read.unlock();
        }
    }

    public void changeActive(StrategyId startegyId, Pair pair, boolean active)
    {
        write.lock();
        try
        {
            activeArray[startegyId.ordinal()][pair.ordinal()] = active;
        }
        finally
        {
            write.unlock();
        }
    }

    public abstract void startFilter(SignalProviderId id);

    public abstract boolean filter(SignalHistoryRecord record, Object... parameters);

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
}