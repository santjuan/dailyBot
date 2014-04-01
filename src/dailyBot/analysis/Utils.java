package dailyBot.analysis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dailyBot.control.connection.SqlConnection;
import dailyBot.model.MultiFilter;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySignal;

public class Utils
{
    static final AtomicReference < List <SignalHistoryRecord> > allRecords = new AtomicReference < List <SignalHistoryRecord> > (null);

    static final AtomicInteger lastUpdate = new AtomicInteger(-1);

    public static synchronized void reloadRecords()
    {
    	List <SignalHistoryRecord> records = SqlConnection.getRecords();
    	TreeMap <FilterMap.DoubleArray, SignalHistoryRecord> map = new TreeMap <FilterMap.DoubleArray, SignalHistoryRecord> ();
    	for(SignalHistoryRecord record : records)
    	{
    		FilterMap.DoubleArray doubleArray = new FilterMap.DoubleArray(record);
    		if(map.containsKey(doubleArray))
    		{
    			if(map.get(doubleArray).closeDate < record.closeDate)
        			map.put(doubleArray, record);
    		}
    		else
    			map.put(doubleArray, record);
    	}
    	List <SignalHistoryRecord> uniqueRecords = new ArrayList <SignalHistoryRecord> ();
    	IdentityHashMap <SignalHistoryRecord, SignalHistoryRecord> choosen = new IdentityHashMap <SignalHistoryRecord, SignalHistoryRecord> ();
    	for(SignalHistoryRecord record : map.values())
    		choosen.put(record, record);
    	for(SignalHistoryRecord record : records)
    		if(choosen.containsKey(record))
    			uniqueRecords.add(record);
    	allRecords.getAndSet(Collections.unmodifiableList(uniqueRecords));
        lastUpdate.getAndSet(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
    }

    public static double getSSI(SignalHistoryRecord record)
    {
        if(record.pair.pairFatherA() == record.pair.pairFatherB())
            return record.buy ? -record.SSI1 * 100 : record.SSI1 * 100;
        double ssi1 = record.SSI1;
        double ssi2 = record.SSI2;
        switch(record.pair.pairFatherA())
        {
        case USDCAD:
        case USDCHF:
        case USDJPY:
            ssi1 = -ssi1;
            break;
        default:
            break;
        }
        switch(record.pair.pairFatherB())
        {
        case USDCAD:
        case USDCHF:
        case USDJPY:
            ssi2 = -ssi2;
            break;
        default:
            break;
        }
        double ssi = ssi1 - ssi2;
        if(record.buy)
            ssi = -ssi;
        return 100 * ssi;
    }

    public static List <SignalHistoryRecord> getRecords()
    {
        if((allRecords.get() == null) || (lastUpdate.getAndAdd(0) != Calendar.getInstance().get(Calendar.DAY_OF_MONTH)))
            reloadRecords();
        return allRecords.get();
    }

    public static List <SignalHistoryRecord> getStrategyRecords(StrategyId id, Pair pair)
    {
        LinkedList <SignalHistoryRecord> toReturn = new LinkedList <SignalHistoryRecord> ();
        for(SignalHistoryRecord record : getRecords())
            if(record.id == id && record.pair.equals(pair))
                toReturn.add(record);
        return toReturn;
    }
    
    private static final ConcurrentHashMap <Integer, MultiFilter> filterMap = new ConcurrentHashMap <Integer, MultiFilter> ();

	public static MultiFilter getFilterSignalProvider(int ordinal) 
	{
		if(!filterMap.containsKey(ordinal))
			filterMap.put(ordinal, DailyAnalysis.getFilterSignalProvider(SignalProviderId.values()[ordinal]));
		return filterMap.get(ordinal);
	}

	private static final ConcurrentHashMap <Integer, MultiFilter> filterAllActiveMap = new ConcurrentHashMap <Integer, MultiFilter> ();

	public static MultiFilter getFilterSignalProvider(int ordinal, boolean allActive) 
	{
		if(!allActive)
			return getFilterSignalProvider(ordinal);
		if(!filterAllActiveMap.containsKey(ordinal))
		{
			filterAllActiveMap.put(ordinal, DailyAnalysis.getFilterSignalProvider(SignalProviderId.values()[ordinal]));
			MultiFilter filter = filterAllActiveMap.get(ordinal);
			for(StrategyId strategyId : StrategyId.values())
				for(Pair pair : Pair.values())
					for(boolean isBuy : new boolean[]{true, false})
						filter.changeActive(strategyId, pair, isBuy, false);
		}
		return filterAllActiveMap.get(ordinal);
	}

	private static final ConcurrentHashMap <Integer, Strategy> strategyMap = new ConcurrentHashMap <Integer, Strategy> ();

	public static List<StrategySignal> getStrategySignals(int ordinal) 
	{
		if(!strategyMap.containsKey(ordinal))
			strategyMap.put(ordinal, Strategy.readPersistency(StrategyId.values()[ordinal]));
		return strategyMap.get(ordinal).getSignals();
	}
	
	private static final ConcurrentHashMap < Integer, List <StrategySignal> > signalProviderMap = new ConcurrentHashMap < Integer, List <StrategySignal> > ();

	public static List<StrategySignal> getSignalProviderSignals(int ordinal) 
	{
		if(!signalProviderMap.containsKey(ordinal))
		{
			ArrayList <StrategySignal> signals = new ArrayList <StrategySignal> ();
			for(StrategyId id : StrategyId.values())
				for(StrategySignal signal : getStrategySignals(id.ordinal()))
					if(signal.getUniqueId(SignalProviderId.values()[ordinal].toString()) != 0)
						signals.add(signal);
			signalProviderMap.put(ordinal, signals);
		}
		return signalProviderMap.get(ordinal);
	}

	public static StrategySignal getStrategySignal(int ordinalStrategy, int ordinalPair)
	{
		for(StrategySignal signal : getStrategySignals(ordinalStrategy))
			if(signal.getPair().ordinal() == ordinalPair)
				return signal;
		return null;
	}

	public static String getFilterNameSignalProvider(SignalProviderId id, int i)
	{
		return DailyAnalysis.getFilterNameSignalProvider(id, i);
	}
	
	public static boolean isRelevant(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		if(calendar.get(Calendar.YEAR) >= 2013)
			return true;
		else if(calendar.get(Calendar.YEAR) == 2012 && 
		   calendar.get(Calendar.MONTH) >= Calendar.OCTOBER)
			return true;
		else
			return false;
	}
}