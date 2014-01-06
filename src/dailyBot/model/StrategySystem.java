package dailyBot.model;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import dailyBot.control.DailyLog;
import dailyBot.model.Strategy.StrategyId;

public abstract class StrategySystem
{
    protected StrategyId[] strategies;

    protected ReentrantLock systemLock = new ReentrantLock(true);

    protected abstract void checkConsistency();

    protected abstract ArrayList<StrategySignal> read(String[] input);

    protected abstract void process(String[] read);

    public void lockSystem()
    {
        systemLock.lock();
    }

    public void unlockSystem()
    {
        systemLock.unlock();
    }

    public void startProcessing(String[] read)
    {
        try
        {
            process(read);
        }
        catch(Exception e)
        {
            DailyLog.logError(e.getMessage() + ", error en Iniciar procesamiento al procesar en: "
                + getClass().getCanonicalName());
        }
    }

    public abstract void startThread();

    public abstract void writePersistence();
}
