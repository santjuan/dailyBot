package dailyBot.model;

import java.util.ArrayList;
import java.util.Map;

import dailyBot.model.Strategy.StrategyId;

public class Utils
{
    public static StrategySignal[] getAllSignals()
    {
        ArrayList<StrategySignal> allSignals = new ArrayList<StrategySignal>();
        for(StrategyId strategyId : StrategyId.values())
            allSignals.addAll(strategyId.strategy().duplicateSignals());
        return allSignals.toArray(new StrategySignal[0]);
    }

    public static String checkSignals(StrategyId id)
    {
        String answer = id.toString() + "\n\n";
        for(StrategySignal signal : id.strategy().getSignals())
        {
            answer += signal + "\n";
            String keys = "";
            for(Map.Entry<String, Long> entry : signal.getUniqueIdsMap().entrySet())
            {
                if(entry.getKey().contains("time"))
                    keys += entry.getKey() + ": " + (System.currentTimeMillis() - entry.getValue())
                        + " milliseconds ago";
                else
                    keys += entry.getKey() + ": " + (entry.getValue());
                keys += ", ";
            }
            if(keys.endsWith(", "))
                keys = keys.substring(0, keys.length() - 2);
            answer += keys + "\n\n";
        }
        return answer;
    }
}