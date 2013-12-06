package dailyBot.model;

import dailyBot.model.Strategy.StrategyId;

public interface Broker
{
	public long getUniqueId(UniqueIdSignal signal);
	public void setUniqueId(UniqueIdSignal signal, long value);
	public boolean closeSignal(UniqueIdSignal signal, StrategyId strategyId, Pair pair, boolean buy);
	public boolean openSignal(UniqueIdSignal signal, StrategyId strategyId, Pair pair, boolean buy);
	public boolean openManualSignal(Pair pair, boolean buy);
	public boolean closeManualSignal(long id);
	public void checkConsistency();
	public String checkConsistencyFull(boolean sendMessage);
}
