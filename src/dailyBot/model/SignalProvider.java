package dailyBot.model;

import java.beans.XMLDecoder;
import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.control.DailyExecutor;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyLoopInfo;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyRunnable;
import dailyBot.control.DailyUtils;
import dailyBot.control.connection.zulutrade.ZulutradeConnection;
import dailyBot.model.Strategy.StrategyId;

public class SignalProvider
{
    private interface BrokerFactory
    {
        public Broker[] getBrokers(SignalProviderId id);
    }

    public enum SignalProviderId
    {
        DEMO(new BrokerFactory()
        {
            @Override
            public Broker[] getBrokers(SignalProviderId id)
            {
                return new Broker[] { new ZulutradeConnection(id) };
            }

        }, true), DAILYBOTSSIEURO(new BrokerFactory()
        {
            @Override
            public Broker[] getBrokers(SignalProviderId id)
            {
                return new Broker[] { new ZulutradeConnection(id) };
            }
        }, false);

        volatile transient SignalProvider thisSignalProvider = null;
        volatile transient BrokerFactory brokerFactory;
        volatile transient boolean inTesting;

        private SignalProviderId(BrokerFactory broker, boolean testing)
        {
            brokerFactory = broker;
            this.inTesting = testing;
        }

        public synchronized SignalProvider signalProvider()
        {
            if(thisSignalProvider == null)
                DailyLog.logError("Proveedor " + this + " fue llamado antes de ser registrado.");
            return thisSignalProvider;
        }

        public synchronized void startSignalProvider()
        {
            if(DailyProperties.isTesting() == inTesting)
            {
                thisSignalProvider = new SignalProvider(this);
                thisSignalProvider.brokers = brokerFactory.getBrokers(this);
            }
            else
                thisSignalProvider = new InactiveSignalProvider();
        }
    }

    protected volatile SignalProviderId id;
    protected final AtomicReference <MultiFilter> filter = new AtomicReference <MultiFilter> ();
    protected volatile Broker[] brokers;

    public SignalProvider()
    {
    }

    public SignalProvider(SignalProviderId id)
    {
        this.id = id;
        loadFilter();
    }

    public String checkAllBrokers()
    {
        String answer = "";
        for(Broker broker : brokers)
            answer += "\n\n" + broker.checkConsistencyFull(false);
        return answer;
    }

    public void startPersistenceThread()
    {
    	DailyRunnable persistenceRunnable = new DailyRunnable("Presistence " + id, 2000000L, true)
    	{
    		AtomicBoolean messageSent = new AtomicBoolean(false);
    		AtomicBoolean checked = new AtomicBoolean(false);

    		public void runOnce()
    		{
    			if(id.signalProvider() == null || checkConsistency())
    			{
    				DailyLog.logError("Error de consistencia en " + id);
    				id.startSignalProvider();
    				throw new RuntimeException("Consistency error");
    			}
    			DailyLoopInfo.registerLoop(id.toString() + " persistence");
    			DailyLoopInfo.registerUpdate(id.toString() + " persistence", "State", "loading filter");
    			try
    			{
    				loadFilter();
    				DailyLoopInfo.registerUpdate(id.toString() + " persistence", "State",
    						"filter loaded without errors");
    			}
    			catch(Exception e)
    			{
    				DailyLoopInfo.registerUpdate(id.toString() + " persistence", "State", "error loading filter "
    						+ e + " " + e.getMessage());
    			}
    			finally
    			{
    				DailyLoopInfo.closeLoop(id.toString() + " persistence");
    			}
    			DailyLoopInfo.registerLoop(id.toString() + " checker");
    			DailyLoopInfo.registerUpdate(id.toString() + " checker", "State", "checking brokers consistency");
    			try
    			{
    				checkBrokerConsistency();
    			}
    			catch(Exception e)
    			{
    				DailyLog.logError("Error chequeando consistencia del proveedor " + id);
    			}
    			finally
    			{
    				DailyLoopInfo.registerUpdate(id.toString() + " checker", "State",
    						"brokers consistency checked");
    			}
    			Calendar calendar = Calendar.getInstance();
    			int hour = calendar.get(Calendar.HOUR_OF_DAY);
    			int minute = calendar.get(Calendar.MINUTE);
    			if(minute > 40)
    			{
    				messageSent.set(false); 
    				checked.set(false);
    			}
    			else
    			{
    				try
    				{
    					if(!messageSent.get() && hour == 19)
    					{
    						DailyLog.acummulateLog();
    						DailyLoopInfo.registerLoop(id.toString() + " check");
    						DailyLoopInfo.registerUpdate(id.toString() + " check", "State", "checking brokers");
    						for(Broker broker : brokers)
    							broker.checkConsistencyFull(true);
    						DailyLoopInfo.registerUpdate(id.toString() + " check", "State", "brokers checked");
    						DailyLoopInfo.closeLoop(id.toString() + " check");
    						messageSent.set(true);
    						checked.set(true);
    						DailyUtils.sleep(10000L);
    						DailyLog.sendAcummulated();
    					}
    					if(!checked.get())
    					{
    						DailyLoopInfo.registerLoop(id.toString() + " check");
    						DailyLoopInfo.registerUpdate(id.toString() + " check", "State", "checking brokers");
    						for(Broker broker : brokers)
    							broker.checkConsistencyFull(false);
    						DailyLoopInfo.registerUpdate(id.toString() + " check", "State", "brokers checked");
    						DailyLoopInfo.closeLoop(id.toString() + " check");
    						if(Utils.getSendHours().contains(calendar.get(Calendar.HOUR_OF_DAY)))
    							Utils.makeCheck(SignalProvider.this);
    						checked.set(true);
    					}
    				}
    				catch(RuntimeException e)
    				{
    					DailyLoopInfo.registerUpdate(id.toString() + " checker", "State",
    							"error checking brokers " + e + " " + e.getMessage());
    				}
    				finally
    				{
    					DailyLoopInfo
    					.registerUpdate(id.toString() + " checker", "Hourly check", "check finished");
    					DailyLoopInfo.closeLoop(id.toString() + " checker");
    				}
    			}
    		}
    	};
    	DailyExecutor.addRunnable(persistenceRunnable, 10, TimeUnit.MINUTES, 3, TimeUnit.MINUTES);
    }

    public boolean isActive(StrategyId strategyId, Pair pair)
    {
    	return filter.get().hasActive(strategyId, pair);
    }

    public boolean filterAllow(SignalHistoryRecord record, double entryPrice)
    {
        if(!isActive(record.id, record.pair))
            return false;
        return filter.get().filter(record, true, entryPrice);
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
                    signal.getStartDate()), signal.getEntryPrice()))
                    for(Broker broker : brokers)
                        broker.setUniqueId(signal, 0L);
                else
                {
                	if(Math.abs(signal.getPair().differenceInPips(signal.getEntryPrice(), signal.isBuy())) >= 50)
                	{
                		DailyLog.logError("Senal " + signal + " se abrio demasiado tarde, diferencia absoluta en"
                				+ " pips: " + Math.abs(signal.getPair().differenceInPips(signal.getEntryPrice(), signal.isBuy())));
                        for(Broker broker : brokers)
                            broker.setUniqueId(signal, 0L);
                	}
                	else
	                    for(Broker broker : brokers)
	                        broker.openSignal(signal, signal.getStrategyId(), signal.getPair(), signal.isBuy());
                }
                signal.setUniqueId(id.toString(), 1);
            }
        }
    }

    public void openActive(StrategyId strategyId, Pair pair)
    {
    	if(!isActive(strategyId, pair))
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

    public boolean checkConsistency()
    {
        return filter == null || id == null || brokers == null;
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
            if(!any)
                DailyLog.logError("Error cerrando, senal no tenia id de ningun broker "
                    + toClose.getUniqueIdsMap().toString());
            return any;
        }
        else
        {
            DailyLog.logError("Error cerrando, senal no tenia id de proveedor " + toClose.getUniqueIdsMap().toString());
            return false;
        }
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
        filter.get().changeActive(strategyId, pair, newActive);
    }

    public boolean isOpen(StrategyId strategyId, Pair pair)
    {
        StrategySignal signal = strategyId.strategy().hasPair(pair);
        return signal != null && signal.getUniqueId(id.toString()) != 0;
    }
    
    public int getProfit()
    {
    	int profit = 0;
    	for(StrategySignal signal : Utils.getAllSignals())
    	{
    		boolean opened = false;
    		for(Broker b : brokers)
    			if(b.getUniqueId(signal) != 0)
    				opened = true;
    		if(opened)
    			profit += signal.getPair().differenceInPips(signal.getEntryPrice(), signal.isBuy());
    	}
    	return profit;
    }

    public void loadFilter()
    {
        try
        {
        	if(!new File("filters/" + id + ".xml").exists())
        	{
        		filter.set(new MultiFilter(id));
        		filter.get().startFilters();
        	}
        	else
        	{
        		FileInputStream fis = new FileInputStream("filters/" + id + ".xml");
        		XMLDecoder decoder = new XMLDecoder(fis);
                MultiFilter answer = (MultiFilter) decoder.readObject();
                fis.close();
                decoder.close();
                filter.set(answer);
                filter.get().startFilters();
        	}
        }
        catch(Exception e)
        {
            DailyLog.logError("Error reading filter: " + id.name());
    		filter.set(new MultiFilter(id));
    		filter.get().startFilters();
        }
    }

    public MultiFilter getFilter()
    {
        return filter.get();
    }

    public List <StrategySignal> providerSignals()
    {
        LinkedList <StrategySignal> all = new LinkedList <StrategySignal>();
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
        return filter.get().isActive();
    }

    public void changeFilterActive(boolean newActive)
    {
    	filter.get().setActive(newActive);
    }

    public void checkBrokerConsistency()
    {
        for(Broker broker : brokers)
            broker.checkConsistency();
    }

	public void changeActiveFilter(StrategyId strategyId, Pair pair,
			int newValue) 
	{
		filter.get().changeActiveFilter(strategyId, pair, newValue);
	}
}