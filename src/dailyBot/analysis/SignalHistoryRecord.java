package dailyBot.analysis;

import java.io.Serializable;

import dailyBot.control.DailyProperties;
import dailyBot.control.connection.dailyFx.DailyFxServerConnection;
import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;

public class SignalHistoryRecord implements Comparable <SignalHistoryRecord>, Serializable
{
    private static final long serialVersionUID = 6580617958872557468L;

    public StrategyId id;
    public Pair pair;
    public boolean buy;
    public long openDate;
    public long closeDate;
    public int profit;
    public double VIX;
    public double SSI1;
    public double SSI2;
    public double ATR;
    public double RSI;
    public int low;
    public int high;

    public SignalHistoryRecord()
    {
    }

    public SignalHistoryRecord(StrategyId id, Pair pair, boolean buy)
    {
        this.id = id;
        this.pair = pair;
        this.buy = buy;
        this.openDate = System.currentTimeMillis();
        this.VIX = DailyFxServerConnection.getVIX();
        this.SSI1 = pair.pairFatherA().pairSSI();
        this.SSI2 = pair.pairFatherB().pairSSI();
        this.ATR = StatisticsUtils.calculateATR(this.pair, 14, this.openDate);
        this.RSI = StatisticsUtils.calculateRSI(pair, 27, this.openDate);
        if(!buy)
            RSI = 100 - RSI;
    }

    public SignalHistoryRecord(StrategyId id, Pair pair, boolean buy, long openDate)
    {
        this.id = id;
        this.pair = pair;
        this.buy = buy;
        this.openDate = openDate;
        this.VIX = DailyFxServerConnection.getVIX();
        this.SSI1 = pair.pairFatherA().pairSSI();
        this.SSI2 = pair.pairFatherB().pairSSI();
        this.ATR = StatisticsUtils.calculateATR(this.pair, 14, this.openDate);
        this.RSI = StatisticsUtils.calculateRSI(pair, 27, this.openDate);
        if(!buy)
            RSI = 100 - RSI;
    }

    public SignalHistoryRecord(StrategyId id, Pair pair, boolean buy, long openDate, long closeDate, int profit,
        double vIX, double sSI1, double sSI2, int low, int high)
    {
        this.id = id;
        this.pair = pair;
        this.buy = buy;
        this.openDate = openDate;
        this.closeDate = closeDate;
        this.profit = profit;
        this.low = low;
        int stop = Integer.parseInt(DailyProperties.getProperty("dailyBot.control.connection.zulutrade.ZulutradeConnection.DAILYBOTSSIEURO.stop"));
        if(this.low <= -stop || this.profit <= -stop)
        {
            this.profit = -stop;
            this.low = -stop;
        }
        this.high = high;
        this.VIX = vIX;
        this.SSI1 = sSI1;
        this.SSI2 = sSI2;
        this.ATR = StatisticsUtils.calculateATR(this.pair, 14, this.openDate);
        this.RSI = StatisticsUtils.calculateRSI(pair, 27, this.openDate);
        if(!buy)
            RSI = 100 - RSI;
    }

    public double[] getCharacteristics()
    {
        double vix = VIX;
        double ssi = Utils.getSSI(this);
        double atr = ATR;
        double rsi = RSI;
        return new double[] { vix, ssi, atr, rsi };
    }

    public double[] getOutput()
    {
        return new double[] { profit };
    }

    @Override
    public int compareTo(SignalHistoryRecord other)
    {
        if(openDate - other.openDate == 0)
            if(id.ordinal() == other.id.ordinal())
                return pair.ordinal() - other.pair.ordinal();
            else
                return id.ordinal() - other.id.ordinal();
        return new Long(openDate).compareTo(other.openDate);
    }

    public String generateLine()
    {
    	 double[] characteristics = getCharacteristics();
         double[] output = getOutput();
         String toWrite = characteristics[0] + "";
         for(int i = 1; i < characteristics.length; i++)
             toWrite += " " + characteristics[i];
         for(double d : output)
             toWrite += " " + d;
         return toWrite;
    }
    @Override
    public String toString()
    {
        return "ATR = " + ATR + ", RSI = " + RSI + ", SSI1 = " + SSI1 + ", SSI2 = " + SSI2 + ", VIX = " + VIX
            + ", par = " + pair + ", compra = " + buy;
    }
}