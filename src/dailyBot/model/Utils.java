package dailyBot.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import dailyBot.control.connection.EmailConnection;
import dailyBot.model.Strategy.StrategyId;

public class Utils
{
    public static StrategySignal[] getAllSignals()
    {
        ArrayList <StrategySignal> allSignals = new ArrayList <StrategySignal>();
        for(StrategyId strategyId : StrategyId.values())
            allSignals.addAll(strategyId.strategy().duplicateSignals());
        return allSignals.toArray(new StrategySignal[0]);
    }

    public static String sendTable(String[] labels, String[][] contents)
    {
        StringBuilder answer = new StringBuilder("<html><body><table border=\"1\"><tr>");
        for(String label : labels)
            answer.append("<th>").append(label).append("</th>");
        answer.append("</tr>");
        for(String[] row : contents)
        {
            answer.append("<tr>");
            for(String cell : row)
                answer.append("<td>").append((cell == null) ? "N/A" : cell).append("</td>");
            answer.append("</tr>");
        }
        answer.append("</table></body></html>");
        EmailConnection.sendEmail("DailyBot-info", answer.toString());
        return Arrays.deepToString(labels) + "\n\n" + Arrays.deepToString(contents);
    }

    public static String sendSignalTable(List <StrategySignal> signals)
    {
        TreeMap <String, Integer> labelsMap = new TreeMap <String, Integer>();
        ArrayList <String> allLabels = new ArrayList <String>();
        allLabels.add("STRATEGYID");
        allLabels.add("IS_BUY");
        allLabels.add("LOTS");
        allLabels.add("PAIR");
        allLabels.add("ENTRY");
        allLabels.add("STOP");
        allLabels.add("PROFIT");
        allLabels.add("STOPDISTANCE");
        for(StrategySignal signal : signals)
        {
            for(String key : signal.getUniqueIdsMap().keySet())
                if(!labelsMap.containsKey(key))
                {
                    labelsMap.put(key, allLabels.size());
                    allLabels.add(key);
                }
        }
        String[][] signalTable = new String[signals.size()][allLabels.size()];
        int index = 0;
        for(StrategySignal signal : signals)
        {
            signalTable[index][0] = String.valueOf(signal.getStrategyId());
            signalTable[index][1] = String.valueOf(signal.isBuy());
            signalTable[index][2] = String.valueOf(signal.getLotNumber());
            signalTable[index][3] = String.valueOf(signal.getPair());
            signalTable[index][4] = String.valueOf(signal.getEntryPrice());
            signalTable[index][5] = String.valueOf(signal.getStop());
            signalTable[index][6] = String.valueOf(signal.getPair().differenceInPips(signal.getEntryPrice(),
                signal.isBuy()));
            signalTable[index][7] = String.valueOf(signal.getPair().differenceInPips(signal.getStop(), signal.isBuy()));
            for(String key : signal.getUniqueIdsMap().keySet())
                signalTable[index][labelsMap.get(key)] = String.valueOf(key.contains("time") ? (System
                    .currentTimeMillis() - signal.getUniqueId(key)) : signal.getUniqueId(key));
            index++;
        }
        return sendTable(allLabels.toArray(new String[0]), signalTable);
    }

    public static String checkSignals(String key)
    {
        ArrayList <StrategySignal> signals = new ArrayList <StrategySignal>();
        for(StrategySignal signal : getAllSignals())
            if(signal.getUniqueId(key) != 0)
                signals.add(signal);
        return (signals.isEmpty() ? "NO SIGNALS" : sendSignalTable(signals)) + "\n";
    }

    public static String checkSignals(StrategyId id)
    {
        ArrayList <StrategySignal> signals = new ArrayList <StrategySignal>();
        if(id == null)
            signals.addAll(Arrays.asList(getAllSignals()));
        else
            signals.addAll(id.strategy().getSignals());
        return (signals.isEmpty() ? "NO SIGNALS" : sendSignalTable(signals)) + "\n";
    }
}