package dailyBot.model;

import java.util.ArrayList;

import dailyBot.model.Strategy.StrategyId;

public class Utils 
{
	public static StrategySignal[] getAllSignals()
	{
		ArrayList <StrategySignal> allSignals = new ArrayList <StrategySignal> ();
		for(StrategyId strategyId : StrategyId.values())
			allSignals.addAll(strategyId.strategy().duplicateSignals());
		return allSignals.toArray(new StrategySignal[0]);
	}
}
