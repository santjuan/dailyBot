package dailyBot.model.dailyFx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;

import dailyBot.control.DailyBotMain;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyRunnable;
import dailyBot.control.DailyThread;
import dailyBot.control.DailyThreadAdmin;
import dailyBot.control.DailyThreadInfo;
import dailyBot.control.connection.dailyFx.DailyFxServerConnection;
import dailyBot.model.SignalProvider.SignalProviderId;
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
    public void startThread()
    {
        final DailyFXStrategySystem thisSystem = this;
        DailyThread monitorThread = new DailyThread(new DailyRunnable()
        {
            public void run()
            {
                DailyThread.sleep(120000);
                DailyLog.logInfo("Iniciando hilo " + thisSystem.getClass().getCanonicalName());
                int errorCount = 0;
                while(true)
                {
                    try
                    {
                        boolean onLoop = false;
                        while(DailyBotMain.marketClosed())
                        {
                            if(!onLoop)
                            {
                                DailyLog.acummulateLog();
                                if(DailyBotMain.inLinux)
                                    Runtime.getRuntime().exec(
                                        DailyProperties.getProperty("dailyBot.control.DailyBotMain.startCommand"));
                                onLoop = true;
                            }
                            DailyThread.sleep(300000L);
                            setLastUpdate(System.currentTimeMillis());
                        }
                        System.gc();
                        DailyThreadInfo.registerThreadLoop("DailyFX updater");
                        checkConsistency();
                        DailyThread.sleep(1000);
                        DailyThreadInfo.registerUpdate("DailyFX updater", "State", "reading from DailyFX server");
                        String[] read = DailyFxServerConnection.readDailyFxServer();
                        DailyThreadInfo.registerUpdate("DailyFX updater", "Last read", Arrays.toString(read));
                        DailyThreadInfo.registerUpdate("DailyFX updater", "State", "readed without errors");
                        systemLock.lock();
                        try
                        {
                            DailyThreadInfo.registerUpdate("DailyFX updater", "State", "processing read data");
                            startProcessing(read);
                            checkConsistency();
                            DailyThreadInfo.registerUpdate("DailyFX updater", "State", "data processed");
                            if(changed)
                                systemChanged.signalAll();
                        }
                        finally
                        {
                            systemLock.unlock();
                        }
                        errorCount = 0;
                        if(onLoop)
                        {
                            DailyThread.sleep(300000);
                            DailyLog.logInfo("Reiniciando procesamiento");
                            DailyLog.sendAcummulated();
                        }
                    }
                    catch(Exception e)
                    {
                        try
                        {
                            System.gc();
                            DailyThreadInfo.registerUpdate("DailyFX updater", "State", "error processing " + e + " "
                                + e.getMessage());
                            errorCount++;
                            DailyLog.logError(e.getMessage() + " Error en el ciclo dailyFX");
                            DailyThread.sleep(60000);
                            if(errorCount == 30)
                            {
                                DailyLog.logError(e.getMessage() + " Error de lectura, intentando reiniciar.");
                                DailyLog.tryReboot();
                            }
                        }
                        catch(Exception e1)
                        {
                            DailyLog.logError(e.getMessage() + " Error en el ciclo de error DailyFX");
                        }
                    }
                    finally
                    {
                        DailyThreadInfo.closeThreadLoop("DailyFX updater");
                    }
                    setLastUpdate(System.currentTimeMillis());
                }
            }
        }, 900000L);
        monitorThread.setName("Principal " + getClass().getCanonicalName());
        DailyThreadAdmin.addThread(monitorThread);
        DailyThread persistenceThread = new DailyThread(new DailyRunnable()
        {
            public void run()
            {
                while(true)
                {
                    DailyThreadInfo.registerThreadLoop("DailyFX persistence");
                    DailyThreadInfo.registerUpdate("DailyFX persistence", "State", "waiting for changes");
                    try
                    {
                        waitForChange();
                        DailyThreadInfo.registerUpdate("DailyFX persistence", "State", "stored in db");
                    }
                    catch(RuntimeException e)
                    {
                        DailyThreadInfo.registerUpdate("DailyFX persistence", "State",
                            "error storing " + e + " " + e.getMessage());
                        throw e;
                    }
                    finally
                    {
                        DailyThreadInfo.closeThreadLoop("DailyFX persistence");
                    }
                    setLastUpdate(System.currentTimeMillis());
                }
            }
        }, 600000L);
        persistenceThread.setName("Presistencia " + getClass().getCanonicalName());
        DailyThreadAdmin.addThread(persistenceThread);
    }

    public static final AtomicReference<Vector<StrategySignal>> lastRead = new AtomicReference<Vector<StrategySignal>>();
    public static final AtomicLong lastReadTime = new AtomicLong();

    @Override
    protected ArrayList<StrategySignal> read(String[] input)
    {
        try
        {
            DailyThreadInfo.registerUpdate("DailyFX updater", "Read state", "reading from DailyFX server");
            ArrayList<StrategySignal> read = dailyJSON.leer(input[0]);
            DailyThreadInfo.registerUpdate("DailyFX updater", "Read state", "readed JSON without errors");
            lastRead.set(new Vector<StrategySignal>(read));
            lastReadTime.set(System.currentTimeMillis());
            return read;
        }
        catch(Exception e)
        {
            DailyThreadInfo.registerUpdate("DailyFX updater", "Read state", "error reading JSON");
            DailyLog.logError("Error leyendo las senales de DailyFX " + e.getMessage());
            throw(new RuntimeException("Error de lectura"));
        }
    }

    static final AtomicLong processCount = new AtomicLong(0);

    @Override
    protected void process(String[] read)
    {
        try
        {
            if(((processCount.incrementAndGet()) % 10) == 0)
            {
                for(SignalProviderId signalProviderId : SignalProviderId.values())
                {
                    try
                    {
                        signalProviderId.signalProvider().checkBrokerConsistency();
                    }
                    catch(Exception e)
                    {
                        DailyLog.logError("Error chequeando consistencia del proveedor " + signalProviderId);
                    }
                }
            }
            ArrayList<StrategySignal> readSignals = read(read);
            DailyThreadInfo.registerUpdate("DailyFX updater", "Processing state", "processing readed signals");
            for(StrategySignal signal : readSignals)
            {
                DailyThreadInfo.registerUpdate("DailyFX updater", "Processing state current signal",
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
                        DailyLog.logInfoWithTitle("rangos", "Cambio: " + affected + " por: " + signal);
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
                        DailyLog.logInfoWithTitle("rangos", "Cambio: " + affected + " por: " + signal);
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
                            if(profit >= 75)
                            {
                                double stop = affected.getPair().getCurrentPriceMinus(75, affected.isBuy());
                                if(affected.getPair().differenceInPips(stop, affected.getStop(), affected.isBuy()) > 0)
                                {
                                    try
                                    {
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
                }
                else
                {
                    current.processSignalChange(signal.getPair(), false, signal.isBuy(), signal.getLotNumber(),
                        signal.getEntryPrice(), affected);
                    changed = true;
                    DailyLog.logInfoWithTitle("rangos", "Agregando: " + signal);
                }
                DailyThreadInfo.registerUpdate("DailyFX updater", "Processing state current signal",
                    "signal processed: " + signal);
            }
            for(StrategyId strategyId : strategies)
            {
                Strategy current = strategyId.strategy();
                DailyThreadInfo.registerUpdate("DailyFX updater", "Processing state",
                    "processing strategy, checking stops: " + strategyId);
                current.checkStops();
                List<StrategySignal> signalsList = current.duplicateSignals();
                for(StrategySignal signal : signalsList)
                {
                    DailyThreadInfo.registerUpdate("DailyFX updater", "Processing state existing signals",
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
                        DailyLog.logInfoWithTitle("rangos", "No encontrada: " + signal);
                        current.processSignalChange(signal.getPair(), true, false, signal.getLotNumber(), 0, signal);
                        changed = true;
                    }
                }
            }
        }
        catch(Exception e)
        {
            DailyLog.logError(e.getMessage() + ": Error al procesar senales de dailyFX.");
        }
    }

    public void waitForChange()
    {
        long startTime = System.currentTimeMillis();
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
            changed = false;
        }
        finally
        {
            systemLock.unlock();
        }
        DailyThreadInfo.registerUpdate("DailyFX persistence", "State", "storing in db");
        writePersistence();
        DailyThreadInfo.registerUpdate("DailyFX persistence", "State", "stored to db without errors");
    }

    @Override
    public void writePersistence()
    {
        checkConsistency();
        for(StrategyId e : strategies)
            e.strategy().writePersistency();
    }
}