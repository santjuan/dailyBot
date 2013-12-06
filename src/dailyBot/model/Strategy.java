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
        BREAKOUT1, BREAKOUT2, RANGE1, RANGE2, MOMENTUM1, MOMENTUM2, TECHNICAL, JOEL;

        volatile Strategy thisStategy = null;

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
    protected List<StrategySignal> signals = new LinkedList<StrategySignal>();
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
        write.lock();
        try
        {
            if(hit)
            {
                affected.setLotNumber(affected.getLotNumber() - lotNumber);
                if(affected.getLotNumber() <= 0)
                {
                    MySqlConnection.addRecord(id, affected);
                    for(SignalProviderId signalProviderId : SignalProviderId.values())
                        signalProviderId.signalProvider().processSignal(affected, hit);
                    signals.remove(affected);
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
                signals.add(newSignal);
                newSignal.getPair().addSignal(newSignal);
                for(SignalProviderId signalProviderId : SignalProviderId.values())
                    signalProviderId.signalProvider().processSignal(newSignal, hit);
            }
        }
        finally
        {
            write.unlock();
        }
    }

    public void checkStops()
    {
        read.lock();
        try
        {
            for(StrategySignal affected : signals)
            {
                if(Math.abs(affected.getStop()) < 10e-4d)
                    continue;
                if(affected.isBuy())
                {
                    if((affected.getPair().getCurrentPrice(true) < affected.getStop())
                            && (affected.getPair().getCurrentPrice(false) < affected.getStop())
                            && ((affected.getLotNumber() < 4) || (affected.getStop() > affected.stopDaily())))
                    {
                        if(!affected.isStopTouched())
                        {
                            for(SignalProviderId signalProviderId : SignalProviderId.values())
                                if(!affected.isStopTouched())
                                    signalProviderId.signalProvider().stopped(affected);
                            affected.setStopTouched(true);
                        }
                    }
                }
                else
                {
                    if((affected.getPair().getCurrentPrice(false) > affected.getStop())
                            && (affected.getPair().getCurrentPrice(true) > affected.getStop())
                            && ((affected.getLotNumber() < 4) || (affected.getStop() < affected.stopDaily())))
                    {
                        if(!affected.isStopTouched())
                        {
                            for(SignalProviderId signalProviderId : SignalProviderId.values())
                                if(!affected.isStopTouched())
                                    signalProviderId.signalProvider().stopped(affected);
                            affected.setStopTouched(true);
                        }
                    }
                }
            }
        }
        finally
        {
            read.unlock();
        }
    }

    public StrategySignal hasPair(Pair par)
    {
        read.lock();
        try
        {
            for(StrategySignal signal : signals)
            {
                if(signal.getPair().equals(par))
                    return signal;
            }
            return null;
        }
        finally
        {
            read.unlock();
        }
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

    public List<StrategySignal> duplicateSignals()
    {
        read.lock();
        try
        {
            LinkedList<StrategySignal> copiedSignals = new LinkedList<StrategySignal>();
            copiedSignals.clear();
            if(signals == null)
                return null;
            for(StrategySignal signal : signals)
                copiedSignals.add(signal);
            return copiedSignals;
        }
        finally
        {
            read.unlock();
        }
    }

    public List<StrategySignal> getSignals()
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

    public void setSignals(List<StrategySignal> signals)
    {
        write.lock();
        try
        {
            this.signals = new LinkedList<StrategySignal>();
            for(StrategySignal signal : signals)
            {
                this.signals.add(signal);
                signal.getPair().addSignal(signal);
            }
        }
        finally
        {
            write.unlock();
        }
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