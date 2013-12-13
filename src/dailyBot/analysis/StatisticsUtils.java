package dailyBot.analysis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;

import dailyBot.control.connection.MySqlConnection;
import dailyBot.model.Pair;

public class StatisticsUtils
{
    public static class PairHistory
    {
        public Date date;
        public double high;
        public double low;
        public double open;
        public double close;

        public PairHistory(Date date, double low, double high, double open, double close)
        {
            this.date = date;
            this.low = low;
            this.high = high;
            this.open = open;
            this.close = close;
        }
    }

    public static double calculateEMA(ArrayList <Double> values, int period)
    {
        double ema = 0;
        for(int i = 0; i < Math.min(values.size(), 14); i++)
            ema += values.get(i);
        ema /= Math.min(values.size(), 14);
        double alpha = 2.0d / (period + 1.0d);
        for(int i = 14; i < values.size(); i++)
            ema = alpha * values.get(i) + (1 - alpha) * ema;
        return ema;
    }

    public static double calculateATR(Pair pair, int period, long closingDate)
    {
        ArrayList <Double> values = new ArrayList <Double>();
        SortedMap <Date, PairHistory> map = MySqlConnection.getPairHistory(pair, new Date(closingDate));
        double lastClose = 0;
        int i = 0;
        for(PairHistory actual : map.values())
        {
            if(i++ == 0)
            {
                lastClose = actual.close;
                continue;
            }
            values.add(Math.max(
                (actual.high - actual.low) * pair.pairMultiplier(),
                Math.max(Math.abs(actual.high - lastClose) * pair.pairMultiplier(), Math.abs(actual.low - lastClose)
                    * pair.pairMultiplier())));
            lastClose = actual.close;
        }
        return StatisticsUtils.calculateEMA(values, period);
    }

    public static double calculateATR(Pair pair, int period)
    {
        return calculateATR(pair, period, Calendar.getInstance().getTimeInMillis());
    }

    public static double calculateRSI(Pair pair, int period, long closingDate)
    {
        ArrayList <Double> values = new ArrayList <Double>();
        SortedMap <Date, PairHistory> map = MySqlConnection.getPairHistory(pair, new Date(closingDate));
        double lastClose = 0;
        int i = 0;
        for(PairHistory actual : map.values())
        {
            if(i++ == 0)
            {
                lastClose = actual.close;
                continue;
            }
            double newPrice = actual.close - lastClose;
            if(newPrice > 0)
                values.add(newPrice);
            else
                values.add(0d);
            lastClose = actual.close;
        }
        double emaU = StatisticsUtils.calculateEMA(values, period);
        lastClose = 0;
        values.clear();
        i = 0;
        for(PairHistory actual : map.values())
        {
            if(i++ == 0)
            {
                lastClose = actual.close;
                continue;
            }
            double newPrice = actual.close - lastClose;
            if(newPrice < 0)
                values.add(-newPrice);
            else
                values.add(0d);
            lastClose = actual.close;
        }
        double emaD = StatisticsUtils.calculateEMA(values, period);
        double rS = emaU / emaD;
        return emaD < 1e-8 ? 100 : 100d - 100d / (1d + rS);
    }
}