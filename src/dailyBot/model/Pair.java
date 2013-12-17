package dailyBot.model;

import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dailyBot.control.DailyLog;
import dailyBot.control.DailyThread;
import dailyBot.control.connection.MySqlConnection;

public enum Pair
{
    EURUSD(10000), USDJPY(100), GBPUSD(10000), USDCHF(10000), EURCHF(EURUSD, USDCHF, 10000), AUDUSD(10000), USDCAD(
        10000), NZDUSD(10000), EURJPY(EURUSD, USDJPY, 100), GBPJPY(100), CHFJPY(USDCHF, USDJPY, 100), GBPCHF(GBPUSD,
        USDCHF, 10000), EURAUD(EURUSD, AUDUSD, 10000), AUDJPY(AUDUSD, USDJPY, 100), ALL(0);

    private transient final Pair fatherA;
    private transient final Pair fatherB;
    private transient final int multiplier;
    private transient double currentBid = 0;
    private transient double currentAsk = 0;
    private transient double currentSSI = 0;
    private transient double high = Double.NEGATIVE_INFINITY;
    private transient double low = Double.POSITIVE_INFINITY;
    private transient double open = Double.NEGATIVE_INFINITY;
    private transient Calendar date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    private transient final LinkedList <StrategySignal> signals = new LinkedList <StrategySignal>();
    private transient static String message = "";
    private transient static int startNumber = 0;
    private transient final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private transient final Lock read = readWriteLock.readLock();
    private transient final Lock write = DailyThread.getSafeWriteLock(readWriteLock);
    private transient final ReentrantReadWriteLock readWriteLockB = new ReentrantReadWriteLock(true);
    private transient final Lock readB = readWriteLockB.readLock();
    private transient final Lock writeB = DailyThread.getSafeWriteLock(readWriteLockB);

    private Pair(int multiplier)
    {
        fatherA = this;
        fatherB = this;
        this.multiplier = multiplier;
    }

    private Pair(Pair fatherA, Pair fatherB, int multiplier)
    {
        this.fatherA = fatherA;
        this.fatherB = fatherB;
        this.multiplier = multiplier;
    }

    public int pairMultiplier()
    {
        return multiplier;
    }

    public Pair pairFatherA()
    {
        return fatherA;
    }

    public Pair pairFatherB()
    {
        return fatherB;
    }

    public boolean isCorrect(double price)
    {
        if(multiplier == 100)
            return price >= 10;
        else
            return price < 10;
    }

    public int differenceInPips(double priceA, double priceB, boolean buy)
    {
        double currentPrice = priceA;
        double difference = buy ? currentPrice - priceB : priceB - currentPrice;
        return (int) Math.round(Math.max(-1000000d, Math.min(1000000d, (difference) * multiplier)));
    }

    public int differenceInPips(double priceB, boolean buy)
    {
        double currentPrice = getCurrentPrice(buy);
        double difference = buy ? currentPrice - priceB : priceB - currentPrice;
        return (int) Math.round(Math.max(-1000000d, Math.min(1000000d, (difference) * multiplier)));
    }

    public void changeBidAsk(double bid, double ask)
    {
        String errorMessage = null;
        boolean zeroBid = false;
        write.lock();
        try
        {
            zeroBid = currentBid == 0;
            if(zeroBid)
            {
                if(isCorrect(bid) && isCorrect(ask) && pairSpread(bid, ask, this) <= 75)
                {
                    currentBid = bid;
                    currentAsk = ask;
                    double[] initialData = MySqlConnection.getPairData(this, date);
                    low = initialData[0];
                    high = initialData[1];
                    open = initialData[2];
                    message += "\nInicializando par " + toString() + ", bid nuevo: " + bid + ", ask nuevo: " + ask;
                }
                else
                    errorMessage = "Error en Par inicializando " + toString() + ", bid anterior: " + currentBid
                        + ", bid nuevo: " + bid + ", ask anterior: " + currentAsk + ", ask nuevo: " + ask;
                startNumber++;
            }
            else
            {
                if(Math.abs(differenceInPips(bid, true)) <= 200 && Math.abs(differenceInPips(ask, false)) <= 200
                    && pairSpread(bid, ask, this) <= 75)
                {
                    currentBid = bid;
                    currentAsk = ask;
                }
                else
                {
                    errorMessage = "Error en par " + toString() + ", bid anterior: " + currentBid + ", bid nuevo: "
                        + bid + ", ask anterior: " + currentAsk + ", ask nuevo: " + ask;
                }
            }
        }
        finally
        {
            write.unlock();
        }
        if(zeroBid && startNumber >= values().length - 1)
            DailyLog.logInfo(message);
        if(errorMessage != null)
            DailyLog.logError(errorMessage);
    }

    public double getPriceMinus(double priceA, int pips, boolean buy)
    {
        double currentPrice = priceA;
        double pipsD = pips;
        pipsD /= multiplier;
        if(buy)
            return currentPrice - pipsD;
        else
            return currentPrice + pipsD;
    }

    public double getCurrentPriceMinus(int pips, boolean buy)
    {
        double currentPrice = getCurrentPrice(buy);
        double pipsD = pips;
        pipsD /= multiplier;
        if(buy)
            return currentPrice - pipsD;
        else
            return currentPrice + pipsD;
    }

    public double getCurrentPrice(boolean buy)
    {
        read.lock();
        try
        {
            return buy ? currentBid : currentAsk;
        }
        finally
        {
            read.unlock();
        }
    }

    public void changeSSI(double ssi)
    {
        write.lock();
        try
        {
            currentSSI = ssi;
        }
        finally
        {
            write.unlock();
        }
    }

    public double pairSSI()
    {
        read.lock();
        try
        {
            return currentSSI;
        }
        finally
        {
            read.unlock();
        }
    }

    private int getProfitInPips(StrategySignal s)
    {
        double currentPrice = getCurrentPrice(s.isBuy());
        double difference = s.isBuy() ? currentPrice - s.getEntryPrice() : s.getEntryPrice() - currentPrice;
        return (int) Math.round((difference) * multiplier);
    }

    public void addSignal(StrategySignal signal)
    {
        writeB.lock();
        try
        {
            if(!signals.contains(signal))
                signals.add(signal);
        }
        finally
        {
            writeB.unlock();
        }
    }

    public void deleteSignal(StrategySignal signal)
    {
        writeB.lock();
        try
        {
            for(Iterator <StrategySignal> it = signals.iterator(); it.hasNext();)
                if(signal == it.next())
                    it.remove();
        }
        finally
        {
            writeB.unlock();
        }
    }

    public void processSignals()
    {
        if(this == ALL)
            return;
        writeB.lock();
        try
        {
            if(Math.abs(getCurrentPrice(true) - 0.0d) < 10e-4d || Math.abs(getCurrentPrice(false) - 0.0d) < 10e-4d)
                return;
            for(StrategySignal signal : signals)
            {
                signal.setLow(Math.min(signal.getLow(), getProfitInPips(signal)));
                signal.setHigh(Math.max(signal.getHigh(), getProfitInPips(signal)));
            }
            low = Math.min(low, getCurrentPrice(true));
            high = Math.max(high, getCurrentPrice(true));
            if(open == Double.NEGATIVE_INFINITY)
                open = getCurrentPrice(true);
            Calendar current = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            int day = current.get(Calendar.DAY_OF_MONTH);
            if(day != date.get(Calendar.DAY_OF_MONTH))
                closeDay();
            else
                MySqlConnection.addPairData(this, open, getCurrentPrice(true), low, high);
        }
        finally
        {
            writeB.unlock();
        }
    }

    public void closeDay()
    {
        writeB.lock();
        try
        {
            high = getCurrentPrice(true);
            low = getCurrentPrice(true);
            open = getCurrentPrice(true);
            date = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        }
        finally
        {
            writeB.unlock();
        }
    }

    public String checkSignals()
    {
        readB.lock();
        try
        {
            String debug = "";
            for(StrategySignal s : signals)
                debug += s.getStrategyId().toString() + " " + s.getPair().toString() + " " + s.getEntryPrice() + " "
                    + s.isBuy() + " " + s.getLow() + " " + s.getHigh() + "\n";
            return debug;
        }
        finally
        {
            readB.unlock();
        }
    }

    public int pairSpread()
    {
        double currentBid = 0;
        double currentAsk = 0;
        read.lock();
        try
        {
            currentBid = this.currentBid;
            currentAsk = this.currentAsk;
        }
        finally
        {
            read.unlock();
        }
        return pairSpread(currentBid, currentAsk, this);
    }

    public static int pairSpread(double bid, double ask, Pair pair)
    {
        double difference = ask - bid;
        return (int) Math.round((difference) * pair.multiplier);
    }

    public static Pair stringToPair(String string)
    {
        for(Pair pair : values())
            if(string.equals(pair.toString()))
                return pair;
        return ALL;
    }

    public static Pair parsePair(String content)
    {
        Pair answer = null;
        for(Pair pair : values())
            if(pair != ALL && content.contains(pair.toString()))
                answer = pair;
        return answer;
    }

    public boolean equals(Pair par)
    {
        if(par == ALL)
            return true;
        return super.equals(par);
    }
}