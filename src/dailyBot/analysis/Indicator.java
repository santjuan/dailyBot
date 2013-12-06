package dailyBot.analysis;

import java.util.Calendar;
import java.util.Hashtable;

import dailyBot.analysis.Ranges.Range;

public enum Indicator
{
    BUY(new Calculable()
    {
        @Override
        public double calculate(SignalHistoryRecord record)
        {
            return record.buy ? 1 : 0;
        }
    }, new Range(0, 2, 0, 2), 1, true, new Object[][] { { 0, "venta" }, { 1, "compra" }, { 2, "ambos" } }),

    TIME(new Calculable()
    {
        @Override
        public double calculate(SignalHistoryRecord record)
        {
            Calendar date = Calendar.getInstance();
            final long week = 1000L * 60 * 60 * 24 * 7;
            final long month = 1000L * 60 * 60 * 24 * 30;
            if(record.openDate > date.getTimeInMillis() - week)
                return 0;
            if(record.openDate > date.getTimeInMillis() - 2 * week)
                return 1;
            if(record.openDate > date.getTimeInMillis() - 3 * week)
                return 2;
            if(record.openDate > date.getTimeInMillis() - month)
                return 3;
            if(record.openDate > date.getTimeInMillis() - 2 * month)
                return 4;
            if(record.openDate > date.getTimeInMillis() - 3 * month)
                return 5;
            if(record.openDate > date.getTimeInMillis() - 6 * month)
                return 6;
            if(record.openDate > date.getTimeInMillis() - 12 * month)
                return 7;
            return 8;
        }
    }, new Range(0, 8, 0, 8), 1, true, new Object[][] { { 0, "1s" }, { 1, "2s" }, { 2, "3s" }, { 3, "1m" },
        { 4, "2m" }, { 5, "3m" }, { 6, "6m" }, { 7, "1a" }, { 8, "t" } }),

    VIX(new Calculable()
    {
        @Override
        public double calculate(SignalHistoryRecord record)
        {
            return record.VIX;
        }

    }, new Range(0, 50, 0, 50), 2, false),

    SSI(new Calculable()
    {
        @Override
        public double calculate(SignalHistoryRecord record)
        {
            return Utils.getSSI(record);
        }

    }, new Range(-500, 500, -500, 500), 50, false, new Object[][] { { -500, "-500" }, { 500, "500" } }),

    ATR(new Calculable()
    {
        @Override
        public double calculate(SignalHistoryRecord record)
        {
            return record.ATR;
        }

    }, new Range(0, 400, 0, 400), 25, false),

    RSI(new Calculable()
    {
        @Override
        public double calculate(SignalHistoryRecord record)
        {
            return record.RSI;
        }

    }, new Range(0, 100, 0, 100), 5, false);

    private interface Calculable
    {
        double calculate(SignalHistoryRecord record);
    }

    Calculable calculable;
    Range range;
    int spaced;
    boolean isInfo;
    boolean hasLabels;
    Hashtable<Integer, Object> labels;

    private Indicator(Calculable calculable, Range range, int spaced, boolean isInfo)
    {
        this.calculable = calculable;
        this.range = range;
        this.spaced = spaced;
        this.isInfo = isInfo;
        hasLabels = false;
    }

    private Indicator(Calculable calculable, Range range, int spaced, boolean isInfo, Object[][] labelsArray)
    {
        this(calculable, range, spaced, isInfo);
        hasLabels = true;
        labels = new Hashtable<Integer, Object>();
        for(Object[] l : labelsArray)
            labels.put(new Integer((Integer) l[0]), l[1]);
    }

    public Range getRange()
    {
        return range;
    }

    public int getSpaced()
    {
        return spaced;
    }

    public Hashtable<Integer, Object> getLabels()
    {
        return labels;
    }

    public boolean hasLabels()
    {
        return hasLabels;
    }

    public double calculate(SignalHistoryRecord record)
    {
        double result = calculable.calculate(record);
        double min = record.buy ? range.getMinBuy() : range.getMinSell();
        double max = record.buy ? range.getMaxBuy() : range.getMaxSell();
        if(result < min)
            result = min;
        if(result > max)
            result = max;
        return result;
    }
}