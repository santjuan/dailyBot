package dailyBot.analysis;

import java.io.Serializable;

import java.util.EnumMap;

import dailyBot.control.DailyLog;

public class Ranges implements Serializable
{
    private static final long serialVersionUID = -6461964459317876445L;

    static public class Range implements Serializable
    {
        private static final long serialVersionUID = 3013772711096500473L;

        private boolean invertedBuy;
        private boolean invertedSell;
        private double minBuy;
        private double maxBuy;
        private double minSell;
        private double maxSell;

        public Range()
        {
        }

        public Range(double minBuy, double maxBuy, double minSell, double maxSell, boolean invertedBuy, boolean invertedSell)
        {
            this.minBuy = minBuy;
            this.maxBuy = maxBuy;
            this.minSell = minSell;
            this.maxSell = maxSell;
            this.invertedBuy = invertedBuy;
            this.invertedSell = invertedSell;
        }

        public Range(double minBuy, double maxBuy, double minSell, double maxSell)
        {
            this(minBuy, maxBuy, minSell, maxSell, false, false);
        }

        public synchronized Range duplicate()
        {
            return new Range(minBuy, maxBuy, minSell, maxSell, invertedBuy, invertedSell);
        }

        public synchronized boolean isInside(double value, boolean buy)
        {
            double minimum = buy ? minBuy : minSell;
            double maximum = buy ? maxBuy : maxSell;
            boolean inverted = buy ? invertedBuy : invertedSell;
            return !inverted ? minimum <= value && value <= maximum : minimum >= value || value >= maximum;
        }

        public synchronized void setInvertedBuy(boolean inverted)
        {
            this.invertedBuy = inverted;
        }

        public synchronized boolean isInvertedBuy()
        {
            return invertedBuy;
        }

        public synchronized void setInvertedSell(boolean inverted)
        {
            this.invertedSell = inverted;
        }

        public synchronized boolean isInvertedSell()
        {
            return invertedSell;
        }

        public synchronized void setMinBuy(double minBuy)
        {
            this.minBuy = minBuy;
        }

        public synchronized double getMinBuy()
        {
            return minBuy;
        }

        public synchronized void setMinSell(double minSell)
        {
            this.minSell = minSell;
        }

        public synchronized double getMinSell()
        {
            return minSell;
        }

        public synchronized void setMaxBuy(double maxBuy)
        {
            this.maxBuy = maxBuy;
        }

        public synchronized double getMaxBuy()
        {
            return maxBuy;
        }

        public synchronized void setMaxSell(double maxSell)
        {
            this.maxSell = maxSell;
        }

        public synchronized double getMaxSell()
        {
            return maxSell;
        }

        public synchronized String toString(double value, boolean buy)
        {
            double min = buy ? minBuy : minSell;
            double max = buy ? maxBuy : maxSell;
            boolean inverted = buy ? invertedBuy : invertedSell;
            if(inverted)
                return value + " <= " + min + " or " + value + " >= " + max;
            else
                return min + " <= " + value + " <= " + max;
        }

        public boolean isInverted(Ranges ranges)
        {
            boolean buy = ((int) ranges.getRange(Indicator.BUY).getMinBuy()) == 1;
            return buy ? invertedBuy : invertedSell;
        }

        public double getMin(Ranges ranges)
        {
            boolean buy = ((int) ranges.getRange(Indicator.BUY).getMinBuy()) == 1;
            return buy ? minBuy : minSell;
        }

        public double getMax(Ranges ranges)
        {
            boolean buy = ((int) ranges.getRange(Indicator.BUY).getMinBuy()) == 1;
            return buy ? maxBuy : maxSell;
        }

        public void setMin(double value, Ranges ranges)
        {
            boolean buy = ((int) ranges.getRange(Indicator.BUY).getMinBuy()) == 1;
            if(buy)
                minBuy = value;
            else
                minSell = value;
        }

        public void setMax(double value, Ranges ranges)
        {
            boolean buy = ((int) ranges.getRange(Indicator.BUY).getMinBuy()) == 1;
            if(buy)
                maxBuy = value;
            else
                maxSell = value;
        }
    }

    EnumMap<Indicator, Range> ranges = new EnumMap<Indicator, Range>(Indicator.class);

    public Ranges()
    {
        for(Indicator indicator : Indicator.values())
            ranges.put(indicator, indicator.range.duplicate());
    }

    public EnumMap<Indicator, Range> getRanges()
    {
        return ranges;
    }

    public void setRanges(EnumMap<Indicator, Range> rangesEnumMap)
    {
        ranges = rangesEnumMap;
        ranges.put(Indicator.BUY, Indicator.BUY.getRange().duplicate());
        for(Indicator indicator : Indicator.values())
            if(!ranges.containsKey(indicator))
                ranges.put(indicator, indicator.range.duplicate());
    }

    public Range getRange(Indicator indicator)
    {
        if(!ranges.containsKey(indicator))
            ranges.put(indicator, indicator.range.duplicate());
        return ranges.get(indicator);
    }

    public void changeRange(Indicator indicator, Range range)
    {
        if(!ranges.containsKey(indicator))
            ranges.put(indicator, indicator.range.duplicate());
        Range toChange = ranges.get(indicator);
        toChange.setMinBuy(range.getMinBuy());
        toChange.setMaxBuy(range.getMaxBuy());
        toChange.setMinSell(range.getMinSell());
        toChange.setMaxSell(range.getMaxSell());
        toChange.setInvertedBuy(range.isInvertedBuy());
        toChange.setInvertedSell(range.isInvertedSell());
    }

    public boolean fulfills(SignalHistoryRecord record, boolean ignoreInfo, String sendMessage)
    {
        String message = sendMessage + "\n\n" + record.toString() + "\n\n";
        for(Indicator indicator : Indicator.values())
        {
            message += indicator.toString();
            if(!ranges.containsKey(indicator))
                ranges.put(indicator, indicator.range.duplicate());
            if(ignoreInfo && indicator.isInfo)
            {
                message += ", ignorando: " + ranges.get(indicator).toString(indicator.calculate(record), record.buy) + "\n";
                continue;
            }
            if(indicator == Indicator.BUY)
            {
                int value = (int) ranges.get(Indicator.BUY).getMinBuy();
                if(value != 2 && value == 0 && record.buy)
                    return false;
                if(value != 2 && value == 1 && !record.buy)
                    return false;
            }
            else if(!ranges.get(indicator).isInside(indicator.calculate(record), record.buy))
            {
                message += ", no cumple: " + ranges.get(indicator).toString(indicator.calculate(record), record.buy)
                        + ", terminando con false\n";
                if(!sendMessage.equals(""))
                    DailyLog.logInfoWithTitle("rangos", message);
                return false;
            }
            else
                message += ", cumple: " + ranges.get(indicator).toString(indicator.calculate(record), record.buy) + "\n";
        }
        if(!sendMessage.equals(""))
            DailyLog.logInfoWithTitle("rangos", message);
        return true;
    }

    public Ranges duplicate()
    {
        Ranges newRanges = new Ranges();
        for(Indicator indicator : Indicator.values())
        {
            if(!ranges.containsKey(indicator))
                ranges.put(indicator, indicator.range.duplicate());
            Range range = ranges.get(indicator);
            newRanges.changeRange(indicator, range.duplicate());
        }
        return newRanges;
    }
}