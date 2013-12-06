package dailyBot.analysis;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import dailyBot.control.connection.MySqlConnection;
import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;

public class Utils
{
    static AtomicReference<List<SignalHistoryRecord>> allRecords = new AtomicReference<List<SignalHistoryRecord>>(
            MySqlConnection.getRecords());

    static AtomicInteger lastUpdate = new AtomicInteger(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

    public static void reloadRecords()
    {
        allRecords.getAndSet(MySqlConnection.getRecords());
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

    public static List<SignalHistoryRecord> getRecords()
    {
        if(lastUpdate.getAndAdd(0) != Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
        {
            reloadRecords();
            lastUpdate.getAndSet(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        }
        return allRecords.get();
    }

    public static List<SignalHistoryRecord> getStrategyRecords(StrategyId id, Pair pair)
    {
        if(lastUpdate.getAndAdd(0) != Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
        {
            reloadRecords();
            lastUpdate.getAndSet(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        }
        LinkedList<SignalHistoryRecord> toReturn = new LinkedList<SignalHistoryRecord>();
        for(SignalHistoryRecord record : allRecords.get())
            if(record.id == id && record.pair.equals(pair))
                toReturn.add(record);
        return toReturn;
    }
}