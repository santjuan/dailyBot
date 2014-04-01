package dailyBot.model.dailyFx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

import dailyBot.control.DailyExecutor;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyLoopInfo;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyRunnable;
import dailyBot.control.connection.dailyFx.DailyFxServerConnection;
import dailyBot.model.Strategy;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySignal;
import dailyBot.model.StrategySystem;

public class DailyFXStrategySystem extends StrategySystem
{
    private boolean changed = false;
    protected Condition systemChanged = systemLock.newCondition();

    public DailyFXStrategySystem()
    {
        strategies = new StrategyId[] { StrategyId.BREAKOUT1, StrategyId.BREAKOUT2, StrategyId.RANGE1,
            StrategyId.RANGE2, StrategyId.MOMENTUM1, StrategyId.MOMENTUM2 };
    }

    @Override
    protected void checkConsistency()
    {
        for(StrategyId strategyId : strategies)
            if(strategyId.strategy() == null || strategyId.strategy().checkConsistency())
            {
                DailyLog.logError("Error de consistencia en " + strategyId);
                strategyId.startStrategy();
            }
    }

    @Override
    public void startThreads()
    {
        DailyRunnable monitorRunnable = new DailyRunnable("Monitor " + getClass().getCanonicalName(), 900000L, false)
        {
            public void runOnce()
            {
            	try
            	{
            		DailyLoopInfo.registerLoop("DailyFX updater");
            		checkConsistency();
            		DailyLoopInfo.registerUpdate("DailyFX updater", "State", "reading from DailyFX server");
            		String[] read = DailyFxServerConnection.readDailyFxServer();
            		DailyLoopInfo.registerLoop("DailyFX updater last read string");
            		DailyLoopInfo.registerUpdate("DailyFX updater last read string", "Last read",
            				Arrays.toString(read));
            		DailyLoopInfo.closeLoop("DailyFX updater last read string");
            		DailyLoopInfo.registerUpdate("DailyFX updater", "State", "readed without errors");
            		systemLock.lock();
            		try
            		{
            			DailyLoopInfo.registerUpdate("DailyFX updater", "State", "processing read data");
            			startProcessing(read);
            			checkConsistency();
            			DailyLoopInfo.registerUpdate("DailyFX updater", "State", "data processed");
            			if(changed)
            				systemChanged.signalAll();
            		}
            		finally
            		{
            			systemLock.unlock();
            		}
            	}
            	catch(Exception e)
            	{
            		try
            		{
            			DailyLoopInfo.registerUpdate("DailyFX updater", "State", "error processing " + e + " "
            					+ e.getMessage());
            	       	if((errorCount.incrementAndGet() % 300) <= 1)
            	       		DailyLog.logError(e.getMessage() + " Error en el ciclo dailyFX");
            		}
            		catch(Exception e1)
            		{
            			DailyLog.logError(e.getMessage() + " Error en el ciclo de error DailyFX");
            		}
            	}
            	finally
            	{
            		DailyLoopInfo.closeLoop("DailyFX updater");
            	}
            }
        };
        DailyExecutor.addRunnable(monitorRunnable, DailyProperties.isTesting() ? 10000 : 500, TimeUnit.MILLISECONDS, 3, TimeUnit.MINUTES);
        DailyRunnable persistenceRunnable = new DailyRunnable("Presistence " + getClass().getCanonicalName(), 600000L, false) 
        {
            public void runOnce()
            {
            	DailyLoopInfo.registerLoop("DailyFX persistence");
            	DailyLoopInfo.registerUpdate("DailyFX persistence", "State", "waiting for changes");
            	try
            	{
            		waitForChange();
            		DailyLoopInfo.registerUpdate("DailyFX persistence", "State", "stored in db");
            	}
            	catch(RuntimeException e)
            	{
            		DailyLoopInfo.registerUpdate("DailyFX persistence", "State",
            				"error storing " + e + " " + e.getMessage());
            		throw e;
            	}
            	finally
            	{
            		DailyLoopInfo.closeLoop("DailyFX persistence");
            	}
            }
        };
        DailyExecutor.addRunnable(persistenceRunnable, 3, TimeUnit.MINUTES, 3, TimeUnit.MINUTES);
    }

    public static final AtomicReference <Vector <StrategySignal>> lastRead = new AtomicReference <Vector <StrategySignal>>();
    public static final AtomicLong lastReadTime = new AtomicLong();
    public static final AtomicLong lastChangeTime = new AtomicLong();
    private static final AtomicLong errorCount = new AtomicLong(-1);

    @Override
    protected ArrayList <StrategySignal> read(String[] input)
    {
        try
        {
            DailyLoopInfo.registerUpdate("DailyFX updater", "Read state", "reading from DailyFX server");
            ArrayList <StrategySignal> read = dailyJSON.leer(input[0]);
            DailyLoopInfo.registerUpdate("DailyFX updater", "Read state", "readed JSON without errors");
            lastRead.set(new Vector <StrategySignal>(read));
            lastReadTime.set(System.currentTimeMillis());
            return read;
        }
        catch(Exception e)
        {
            DailyLoopInfo.registerUpdate("DailyFX updater", "Read state", "error reading JSON");
            DailyLog.logError("Error leyendo las senales de DailyFX " + e.getMessage());
            throw(new RuntimeException("Error de lectura"));
        }
    }

    @Override
    protected void process(String[] read)
    {
        try
        {
            ArrayList <StrategySignal> readSignals = read(read);
            DailyLoopInfo.registerUpdate("DailyFX updater", "Processing state", "processing readed signals");
            DailyLoopInfo.registerLoop("DailyFX updater last read objects");
            String readLog = "";            
            final int trailingStopHit = Integer.parseInt(DailyProperties
                    .getProperty("dailyBot.model.dailyFx.DailyFXStrategySystem.trailingStopHit"));
            final int trailingStop = Integer.parseInt(DailyProperties
                .getProperty("dailyBot.model.dailyFx.DailyFXStrategySystem.trailingStop"));
            for(StrategySignal signal : readSignals)
            {
                DailyLoopInfo.registerUpdate("DailyFX updater", "Processing state current signal",
                    "processing signal: " + signal);
                Strategy current = signal.getStrategyId().strategy();
                StrategySignal affected = null;
                if((affected = current.hasPair(signal.getPair())) != null)
                {
                    boolean changedInternal = false;
                    if(signal.isBuy() != affected.isBuy())
                    {
                        current
                            .processSignalChange(signal.getPair(), true, false, affected.getLotNumber(), 0, affected);
                        current.processSignalChange(signal.getPair(), false, signal.isBuy(), signal.getLotNumber(),
                            affected.getEntryPrice(), affected);
                        changed = true;
                        changedInternal = true;
                    	lastChangeTime.set(System.currentTimeMillis());
                        if(current.getId() != StrategyId.BREAKOUT1)
                        {
                        	DailyLog.addRangeInfo("cambios", "Cambio: " + affected + " por: " + signal);
                        }
                    }
                    if(affected.getLotNumber() > signal.getLotNumber())
                    {
                        current.processSignalChange(signal.getPair(), true, false,
                            affected.getLotNumber() - signal.getLotNumber(), affected.getEntryPrice(), affected);
                        if(affected.isBuy())
                        {
                            if(affected.getStop() < affected.getEntryPrice())
                                affected.setStop(0d);
                        }
                        else
                        {
                            if(affected.getStop() > affected.getEntryPrice())
                                affected.setStop(0d);
                        }
                        changed = true;
                        changedInternal = true;
                        lastChangeTime.set(System.currentTimeMillis());
                        if(current.getId() != StrategyId.BREAKOUT1)
                        {
                        	DailyLog.addRangeInfo("cambios", "Cambio: " + affected + " por: " + signal);
                        }
                    }
                    else
                    {
                        int profit = affected.getPair().differenceInPips(affected.getEntryPrice(), affected.isBuy());
                        if(Math.abs(affected.getStop()) < 1e-4d)
                        {
                            affected.setStop(signal.getStop());
                            changed = true;
                            changedInternal = true;
                        }
                        else
                        {
                            if(profit >= trailingStopHit)
                            {
                                double stop = affected.getPair().getCurrentPriceMinus(trailingStop, affected.isBuy());
                                if(affected.getPair().differenceInPips(stop, affected.getStop(), affected.isBuy()) > 0)
                                {
                                    try
                                    {
                                        DailyLog.addRangeInfo("mejora stops", "Mejorando stop: " + affected + ", stop anterior: " + affected.getStop() + ", nuevo stop: " + stop);
                                        affected.setStop(stop);
                                        changed = true;
                                        changedInternal = true;
                                    }
                                    catch(Exception e)
                                    {
                                        DailyLog.logError("Zulutrade error: " + e.getMessage());
                                    }
                                }
                            }
                        }
                        if(affected.isBuy())
                        {
                            if(affected.getStop() < signal.getStop())
                            {
                                affected.setStop(signal.getStop());
                                changed = true;
                                changedInternal = true;
                            }
                            affected.changeStopDaily(signal.getStop());
                        }
                        else
                        {
                            if(affected.getStop() > signal.getStop())
                            {
                                affected.setStop(signal.getStop());
                                changed = true;
                                changedInternal = true;
                            }
                            affected.changeStopDaily(signal.getStop());
                        }
                    }
                    if(!changedInternal)
                        affected.setUniqueId("dailyfx-lastchecktime", System.currentTimeMillis());
                    readLog += "\n" + signal + "\n" + "found: " + affected + ", changed: " + changedInternal + "\n";

                }
                else
                {
                    readLog += "\n" + signal + "\n" + "not found, opening\n";
                    current.processSignalChange(signal.getPair(), false, signal.isBuy(), signal.getLotNumber(),
                        signal.getEntryPrice(), affected);
                    changed = true;
                	lastChangeTime.set(System.currentTimeMillis());
                    if(current.getId() != StrategyId.BREAKOUT1)
                    {
                    	DailyLog.addRangeInfo("cambios", "Agregando: " + signal);
                    }
                }
                DailyLoopInfo.registerUpdate("DailyFX updater", "Processing state current signal",
                    "signal processed: " + signal);
            }
            DailyLoopInfo.registerUpdate("DailyFX updater last read objects", "Last read", readLog);
            DailyLoopInfo.closeLoop("DailyFX updater last read objects");
            for(StrategyId strategyId : strategies)
            {
                Strategy current = strategyId.strategy();
                DailyLoopInfo.registerUpdate("DailyFX updater", "Processing state",
                    "processing strategy, checking stops: " + strategyId);
                List <StrategySignal> signalsList = current.duplicateSignals();
                for(StrategySignal signal : signalsList)
                {
                    DailyLoopInfo.registerUpdate("DailyFX updater", "Processing state existing signals",
                        "processing existing signal: " + signal);
                    boolean found = false;
                    for(StrategySignal newSignal : readSignals)
                        if(current.getId().equals(newSignal.getStrategyId())
                            && signal.getPair().equals(newSignal.getPair()))
                        {
                            found = true;
                            break;
                        }
                    if(!found)
                    {
                    	lastChangeTime.set(System.currentTimeMillis());
                    	if(current.getId() != StrategyId.BREAKOUT1)
                    	{
                    		DailyLog.addRangeInfo("cambios", "No encontrada: " + signal);
                       	}
                        current.processSignalChange(signal.getPair(), true, signal.isBuy(), signal.getLotNumber(), 0,
                            signal);
                        changed = true;
                    }
                }
            }
        }
        catch(Exception e)
        {
        	if((errorCount.incrementAndGet() % 300) == 0)
        	{
        		DailyLog.logError(e.toString() + ": Error al procesar senales de dailyFX.");
        		String ste = "";
        		for(StackTraceElement element : e.getStackTrace())
        			ste += element.toString() + "\n";
        		DailyLog.logError(ste);
        	}
        }
    }
    
    private static final AtomicLong persistenceCounter = new AtomicLong(0);

    public void waitForChange()
    {
        long startTime = System.currentTimeMillis();
        boolean didChange = false;
        systemLock.lock();
        try
        {
            while(!changed && (System.currentTimeMillis() - startTime) < 100000)
                try
                {
                    systemChanged.await(120000, TimeUnit.MILLISECONDS);
                }
                catch(InterruptedException e)
                {
                    DailyLog.logError("Error de interrupcion en sistema dailyFx");
                }
            didChange = changed;
            changed = false;
        }
        finally
        {
            systemLock.unlock();
        }
        DailyLoopInfo.registerUpdate("DailyFX persistence", "State", "storing in db");
        if(didChange || ((persistenceCounter.incrementAndGet() % 12) == 0))
        writePersistence();
        DailyLoopInfo.registerUpdate("DailyFX persistence", "State", "stored to db without errors");
    }

    @Override
    public void writePersistence()
    {
        checkConsistency();
        for(StrategyId e : strategies)
            e.strategy().writePersistency();
    }
}