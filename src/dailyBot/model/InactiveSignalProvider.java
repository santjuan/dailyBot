package dailyBot.model;

import java.util.LinkedList;
import java.util.List;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.model.Strategy.StrategyId;

public class InactiveSignalProvider extends SignalProvider
{
    @Override
    public String checkAllBrokers()
    {
        return "";
    }

    @Override
    public boolean isActive(StrategyId strategyId, Pair pair, boolean isBuy)
    {
        return false;
    }

    @Override
    public boolean filterAllow(SignalHistoryRecord record, double price)
    {
        return false;
    }

    @Override
    public void processSignal(StrategySignal signal, boolean hit)
    {
    }

    @Override
    public boolean checkConsistency()
    {
        return false;
    }

    @Override
    public boolean closeSignal(StrategyId strategyId, Pair pair)
    {
        return true;
    }

    @Override
    public boolean openManualSignal(Pair pair, boolean buy)
    {
        return true;
    }

    @Override
    public boolean closeManualSignal(long id)
    {
        return true;
    }

    @Override
    public void setActive(StrategyId strategyId, Pair pair, boolean isBuy, boolean newActive)
    {
    }

    @Override
    public boolean isOpen(StrategyId strategyId, Pair pair)
    {
        return false;
    }

    @Override
    public int getProfit()
    {
    	return 0;
    }

    @Override
    public MultiFilter getFilter() 
    {
    	return null;
    }
    
    @Override
    public List <StrategySignal> providerSignals()
    {
        return new LinkedList <StrategySignal>();
    }

    @Override
    public boolean filterActive()
    {
        return false;
    }

    @Override
    public void changeFilterActive(boolean newActive)
    {
    }

    @Override
    public void checkBrokerConsistency()
    {
    }
    
    @Override
	public void changeActiveFilter(StrategyId strategyId, Pair pair, boolean isBuy,
			int newValue) 
    {
    }

    @Override
	public void startPersistenceThread()
    {
    }
}