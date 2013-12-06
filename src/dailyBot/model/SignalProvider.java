package dailyBot.model;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyThreadAdmin;
import dailyBot.control.DailyBotMain;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyThread;
import dailyBot.control.DailyRunnable;
import dailyBot.control.DailyThreadInfo;
import dailyBot.control.connection.XMLPersistentObject;
import dailyBot.control.connection.zulutrade.ZulutradeConnection;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.dailyFx.OctaveFilter;

public class SignalProvider extends XMLPersistentObject
{
    private interface FilterFactory
    {
        public Filter[] getFilters(SignalProviderId id);
    }

    private interface BrokerFactory
    {
        public Broker[] getBrokers(SignalProviderId id);
    }

    public enum SignalProviderId
    {
        DEMO(new FilterFactory()
        {
            @Override
            public Filter[] getFilters(SignalProviderId id)
            {
                return new Filter[] { new BasicFilter() };
            }
        }, new BrokerFactory()
        {
            @Override
            public Broker[] getBrokers(SignalProviderId id)
            {
                return new Broker[] { new ZulutradeConnection(id) };
            }

        }), DAILYBOTSSIEURO(new FilterFactory()
        {
            @Override
            public Filter[] getFilters(SignalProviderId id)
            {
                return new Filter[] { "octave".equals(DailyProperties
                    .getProperty("dailyBot.model.SignalProvider.DAILYBOTSSIEURO.filter")) ? new OctaveFilter(id)
                    : new BasicFilter() };
            }
        }, new BrokerFactory()
        {
            @Override
            public Broker[] getBrokers(SignalProviderId id)
            {
                return new Broker[] { new ZulutradeConnection(id) };
            }
        });

        volatile SignalProvider thisSignalProvider = null;
        Broker[] brokers;
        FilterFactory filterFactory;

        private SignalProviderId(FilterFactory filter, BrokerFactory brokerFactory)
        {
            filterFactory = filter;
            brokers = brokerFactory.getBrokers(this);
        }

        public synchronized SignalProvider signalProvider()
        {
            if(thisSignalProvider == null)
                DailyLog.logError("Proveedor " + this + " fue llamado antes de ser registrado.");
            return thisSignalProvider;
        }

        public synchronized void startSignalProvider()
        {
            thisSignalProvider = SignalProvider.loadPersistence(this);
            if(thisSignalProvider == null)
                thisSignalProvider = new SignalProvider(this);
            if(thisSignalProvider.filters == null)
                thisSignalProvider.filters = filterFactory.getFilters(this);
            else
                for(Filter filter : thisSignalProvider.filters)
                    filter.startFilter(this);
            thisSignalProvider.brokers = brokers;
            thisSignalProvider.startPersistenceThread();
        }
    }

    protected volatile SignalProviderId id;
    protected volatile Filter[] filters;
    protected volatile Broker[] brokers;

    public SignalProvider()
    {
    }

    public SignalProvider(SignalProviderId id)
    {
        this.id = id;
    }

    public String checkAllBrokers()
    {
        String answer = "";
        for(Broker broker : brokers)
            answer += "\n\n" + broker.checkConsistencyFull(false);
        return answer;
    }

    private void startPersistenceThread()
    {
        DailyThread persistenceThread = new DailyThread(new DailyRunnable()
        {
            boolean messageSent = false, checked = false;

            public void run()
            {
                DailyThread.sleep(120000);
                while(true)
                {
                    if(id.signalProvider() == null || checkConsistency())
                    {
                        DailyLog.logError("Error de consistencia en " + id);
                        id.startSignalProvider();
                        return;
                    }
                    DailyThread.sleep(10000L);
                    DailyThreadInfo.registerThreadLoop(id.toString() + " persistence");
                    DailyThreadInfo.registerUpdate(id.toString() + " persistence", "State", "storing in db");
                    try
                    {
                        writePersistence();
                        DailyThreadInfo.registerUpdate(id.toString() + " persistence", "State",
                            "data stored without errors");
                    }
                    catch(Exception e)
                    {
                        DailyThreadInfo.registerUpdate(id.toString() + " persistence", "State", "error storing in db "
                            + e + " " + e.getMessage());
                        throw e;
                    }
                    finally
                    {
                        DailyThreadInfo.closeThreadLoop(id.toString() + " persistence");
                    }
                    setLastUpdate(System.currentTimeMillis());
                    DailyThreadInfo.registerThreadLoop(id.toString() + " checker");
                    DailyThreadInfo.registerUpdate(id.toString() + " checker", "State", "starting check");
                    while(DailyBotMain.marketClosed())
                    {
                        DailyThread.sleep(120000);
                        setLastUpdate(System.currentTimeMillis());
                    }
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    if(minute > 40)
                        messageSent = checked = false;
                    else
                    {
                        try
                        {
                            if(!messageSent && hour == 19)
                            {
                                DailyLog.acummulateLog();
                                DailyThreadInfo.registerThreadLoop(id.toString() + " check");
                                DailyThreadInfo.registerUpdate(id.toString() + " check", "State", "checking brokers");
                                for(Broker broker : brokers)
                                    broker.checkConsistencyFull(true);
                                DailyThreadInfo.registerUpdate(id.toString() + " check", "State", "brokers checked");
                                DailyThreadInfo.closeThreadLoop(id.toString() + " check");
                                messageSent = true;
                                checked = true;
                                DailyThread.sleep(900000L);
                                DailyLog.sendAcummulated();
                            }
                            if(!checked)
                            {
                                DailyThreadInfo.registerThreadLoop(id.toString() + " check");
                                DailyThreadInfo.registerUpdate(id.toString() + " check", "State", "checking brokers");
                                for(Broker broker : brokers)
                                    broker.checkConsistencyFull(false);
                                DailyThreadInfo.registerUpdate(id.toString() + " check", "State", "brokers checked");
                                DailyThreadInfo.closeThreadLoop(id.toString() + " check");
                                checked = true;
                            }
                        }
                        catch(RuntimeException e)
                        {
                            DailyThreadInfo.registerUpdate(id.toString() + " checker", "State",
                                "error checking brokers " + e + " " + e.getMessage());
                            throw e;
                        }
                        finally
                        {
                            DailyThreadInfo.registerUpdate(id.toString() + " checker", "State", "check finished");
                            DailyThreadInfo.closeThreadLoop(id.toString() + " checker");
                        }
                    }
                    setLastUpdate(System.currentTimeMillis());
                }
            }

        }, 2000000L);
        persistenceThread.setName("Presistencia " + id);
        DailyThreadAdmin.addThread(persistenceThread);
    }

    public boolean isActive(StrategyId strategyId, Pair pair)
    {
        for(Filter filter : filters)
            if(!filter.isActive(strategyId, pair))
                return false;
        return true;
    }

    public boolean filterAllow(SignalHistoryRecord record)
    {
        if(!isActive(record.id, record.pair))
            return false;
        for(Filter filter : filters)
            if(!filter.filter(record))
                return false;
        return true;
    }

    public void processSignal(StrategySignal signal, boolean hit)
    {
        if(hit)
        {
            if(signal.getUniqueId(id.toString()) == 0L)
                DailyLog.logError("Senal con par: " + signal.getPair() + ", estrategia: " + signal.getStrategyId()
                    + ", proveedor " + id + " no existe y se intento cerrar.");
            else
            {
                for(Broker broker : brokers)
                    if(broker.getUniqueId(signal) != 0)
                        broker.closeSignal(signal, signal.getStrategyId(), signal.getPair(), signal.isBuy());
                signal.setUniqueId(id.toString(), 0L);
            }
        }
        else
        {
            if(signal.getUniqueId(id.toString()) != 0L)
                DailyLog.logError("Senal con par: " + signal.getPair() + ", estrategia: " + signal.getStrategyId()
                    + ", proveedor " + id + " ya existe y se intento abrir otra vez.");
            else
            {
                if(!filterAllow(new SignalHistoryRecord(signal.getStrategyId(), signal.getPair(), signal.isBuy(),
                    signal.getStartDate())))
                    for(Broker broker : brokers)
                        broker.setUniqueId(signal, 0L);
                else
                    for(Broker broker : brokers)
                        broker.openSignal(signal, signal.getStrategyId(), signal.getPair(), signal.isBuy());
                signal.setUniqueId(id.toString(), 1);
            }
        }
    }

    public void openActive(StrategyId strategyId, Pair pair)
    {
        for(Filter filter : filters)
            if(!filter.isActive(strategyId, pair))
                return;
        StrategySignal toOpen = strategyId.strategy().hasPair(pair);
        if((toOpen.getUniqueId(id.toString()) == 0L) || (toOpen == null) || (toOpen.getUniqueId(id.toString()) == 0L))
            DailyLog.logError("Senal con par: " + pair + ", estrategia: " + strategyId + ", proveedor " + id
                + " no estaba abierta y se intento reabrir.");
        else
        {
            DailyLog.logInfoWithTitle("rangos", id + " abriendo senal por orden manual: " + strategyId + ", " + pair);
            for(Broker broker : brokers)
                if(broker.getUniqueId(toOpen) == 0L)
                    broker.openSignal(toOpen, toOpen.getStrategyId(), toOpen.getPair(), toOpen.isBuy());
        }
    }

    public void stopped(StrategySignal signal)
    {
        if((!signal.isStopTouched()) && (signal.getUniqueId(id.toString()) != 0L))
        {
            DailyLog.logInfoWithTitle("rangos",
                id + " cerrando por stop " + signal.getStrategyId() + ", " + signal.getPair() + " Precio entrada: "
                    + signal.getEntryPrice() + " Stop: " + signal.getStop() + " Es compra: " + signal.isBuy());
            for(Broker broker : brokers)
                if(broker.getUniqueId(signal) != 0)
                    broker.closeSignal(signal, signal.getStrategyId(), signal.getPair(), signal.isBuy());
            signal.setStopTouched(true);
        }
        else if(!signal.isStopTouched())
            DailyLog.logError("Senal con par: " + signal.getPair() + ", estrategia: " + signal.getStrategyId()
                + ", proveedor " + id + " no existe y se intento cerrar (toco stop).");
    }

    public boolean checkConsistency()
    {
        return filters == null || id == null || brokers == null;
    }

    public boolean getActive(StrategyId strategyId, Pair pair)
    {
        return isActive(strategyId, pair);
    }

    public boolean closeSignal(StrategyId strategyId, Pair pair)
    {
        StrategySignal toClose = strategyId.strategy().hasPair(pair);
        if(toClose.getUniqueId(strategyId.toString()) != 0L)
        {
            DailyLog.logInfoWithTitle("rangos", strategyId + " cerrando senal por orden manual: " + strategyId + ", "
                + pair);
            boolean any = false;
            for(Broker broker : brokers)
                if(broker.getUniqueId(toClose) != 0L)
                {
                    broker.closeSignal(toClose, toClose.getStrategyId(), toClose.getPair(), toClose.isBuy());
                    any = true;
                }
            return any;
        }
        else
            return false;
    }

    public boolean openManualSignal(Pair pair, boolean buy)
    {
        boolean any = false;
        for(Broker broker : brokers)
            if(broker.openManualSignal(pair, buy))
                any = true;
        return any;
    }

    public boolean closeManualSignal(long id)
    {
        boolean any = false;
        for(Broker broker : brokers)
            if(broker.closeManualSignal(id))
                any = true;
        return any;
    }

    public void setActive(StrategyId strategyId, Pair pair, boolean newActive)
    {
        boolean currentActive = isActive(strategyId, pair);
        StrategySignal signal = strategyId.strategy().hasPair(pair);
        if(currentActive && !newActive)
        {
            if(signal != null && signal.getUniqueId(strategyId.toString()) != 0)
            {
                for(Broker broker : brokers)
                    if(broker.getUniqueId(signal) != 0)
                    {
                        broker.closeSignal(signal, signal.getStrategyId(), signal.getPair(), signal.isBuy());
                        DailyLog.logError("Proveedor " + this.id + ", estrategia: " + strategyId + ", par: " + pair
                            + " estaba abierta y se desactivo, cerrada manualmente.");
                    }
                signal.setUniqueId(strategyId.toString(), 0L);
            }
        }
        else if(currentActive && newActive)
        {
            StrategySignal signal2 = strategyId.strategy().hasPair(pair);
            if(signal2 != null && signal.getUniqueId(strategyId.toString()) == 0)
                signal.setUniqueId(strategyId.toString(), 1L);
        }
        for(Filter filter : filters)
            filter.changeActive(strategyId, pair, newActive);
    }

    public boolean isOpen(StrategyId strategyId, Pair pair)
    {
        StrategySignal signal = strategyId.strategy().hasPair(pair);
        return signal != null && signal.getUniqueId(id.toString()) != 0;
    }

    public void writePersistence()
    {
        try
        {
            writeObject();
        }
        catch(Exception e)
        {
            DailyLog.logError("Error en la escritura en la base de datos: " + id.name() + " " + e.getMessage());
        }
    }

    public static SignalProvider loadPersistence(SignalProviderId id)
    {
        try
        {
            return XMLPersistentObject.readObject(SignalProvider.class, id.ordinal());
        }
        catch(Exception e)
        {
            DailyLog.logError("Error de lectura de base de datos: " + id.name());
            DailyLog.tryInmediateReboot();
            return null;
        }
    }

    public Filter[] getFilters()
    {
        return filters;
    }

    public void setFilters(Filter[] filters)
    {
        this.filters = filters;
    }

    public List<StrategySignal> providerSignals()
    {
        LinkedList<StrategySignal> all = new LinkedList<StrategySignal>();
        for(StrategySignal signal : Utils.getAllSignals())
            if(signal.getUniqueId(id.toString()) != 0)
                all.add(signal);
        return all;
    }

    public SignalProviderId getId()
    {
        return id;
    }

    public void setId(SignalProviderId id)
    {
        this.id = id;
    }

    public boolean filterActive()
    {
        for(Filter filter : filters)
            if(!filter.isActive())
                return false;
        return true;
    }

    public void changeFilterActive(boolean newActive)
    {
        for(Filter filter : filters)
            filter.setActive(newActive);
    }

    public void checkBrokerConsistency()
    {
        for(Broker broker : brokers)
            broker.checkConsistency();
    }

    @Override
    protected int objectId()
    {
        return id.ordinal();
    }
}