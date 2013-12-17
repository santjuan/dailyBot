package dailyBot.model;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dailyBot.control.DailyLog;
import dailyBot.control.DailyThread;
import dailyBot.control.connection.MySqlConnection;
import dailyBot.control.connection.XMLPersistentObject;
import dailyBot.model.SignalProvider.SignalProviderId;

public class Strategy extends XMLPersistentObject
{
    public enum StrategyId
    {
        BREAKOUT1, BREAKOUT2, RANGE1, RANGE2, MOMENTUM1, MOMENTUM2;

        volatile transient Strategy thisStategy = null;

        public Strategy strategy()
        {
            if(thisStategy == null)
                DailyLog.logError("Estrategia " + this + " fue llamada antes de ser registrada.");
            return thisStategy;
        }

        public void startStrategy()
        {
            thisStategy = Strategy.readPersistency(this);
            if(thisStategy == null)
                thisStategy = new Strategy(this);
            for(StrategySignal signal : thisStategy.signals)
                signal.getPair().addSignal(signal);
        }
    }

    protected StrategyId id;
    protected List <StrategySignal> signals = new LinkedList <StrategySignal>();
    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock read = readWriteLock.readLock();
    protected final Lock write = DailyThread.getSafeWriteLock(readWriteLock);

    public Strategy()
    {
    }

    public Strategy(StrategyId id)
    {
        this.id = id;
    }

    public void processSignalChange(Pair pair, boolean hit, boolean buy, int lotNumber, double entryPrice,
        StrategySignal affected)
    {
        if(hit)
        {
            affected.setLotNumber(affected.getLotNumber() - lotNumber);
            if(affected.getLotNumber() <= 0)
            {
                MySqlConnection.addRecord(id, affected);
                for(SignalProviderId signalProviderId : SignalProviderId.values())
                    signalProviderId.signalProvider().processSignal(affected, hit);
                write.lock();
                try
                {
                    signals.remove(affected);
                }
                finally
                {
                    write.unlock();
                }
                affected.getPair().deleteSignal(affected);
            }
        }
        else
        {
            StrategySignal newSignal = new StrategySignal(id, buy, pair, lotNumber, entryPrice, 0);
            if(hasPair(pair) != null)
            {
                DailyLog.logError("Error, par: " + pair + ", ya existe en esta estrategia " + id.toString());
                return;
            }
            write.lock();
            try
            {
                signals.add(newSignal);
            }
            finally
            {
                write.unlock();
            }
            newSignal.getPair().addSignal(newSignal);
            for(SignalProviderId signalProviderId : SignalProviderId.values())
                signalProviderId.signalProvider().processSignal(newSignal, hit);
        }
    }

    public StrategySignal hasPair(Pair par)
    {
        read.lock();
        try
        {
            for(StrategySignal signal : signals)
                if(signal.getPair().equals(par))
                    return signal;
        }
        finally
        {
            read.unlock();
        }
        return null;
    }

    public boolean checkConsistency()
    {
        read.lock();
        try
        {
            return signals == null || id == null;
        }
        finally
        {
            read.unlock();
        }
    }

    public void writePersistency()
    {
        read.lock();
        try
        {
            writeObject();
        }
        catch(Exception e)
        {
            DailyLog.logError("Error en la escritura en la base de datos: " + id.name() + " " + e.getMessage());
        }
        finally
        {
            read.unlock();
        }
    }

    public static Strategy readPersistency(StrategyId id)
    {
        try
        {
            return XMLPersistentObject.readObject(Strategy.class, id.ordinal());
        }
        catch(Exception e)
        {
            DailyLog.logError("Error de lectura de base de datos: " + id.name());
            DailyLog.tryInmediateReboot();
            return null;
        }
    }

    public List <StrategySignal> duplicateSignals()
    {
        read.lock();
        try
        {
            if(signals == null)
                return null;
            else
                return new LinkedList <StrategySignal>(signals);
        }
        finally
        {
            read.unlock();
        }
    }

    public List <StrategySignal> getSignals()
    {
        read.lock();
        try
        {
            return signals;
        }
        finally
        {
            read.unlock();
        }
    }

    public void setSignals(List <StrategySignal> signals)
    {
        write.lock();
        try
        {
            if(signals != null)
                this.signals = new LinkedList <StrategySignal>(signals);
            else
                this.signals = null;
        }
        finally
        {
            write.unlock();
        }
        for(StrategySignal signal : signals)
            signal.getPair().addSignal(signal);
    }

    public StrategyId getId()
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

    public void setId(StrategyId id)
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

    @Override
    protected int objectId()
    {
        return id.ordinal();
    }
}