package dailyBot.model;

import java.io.Serializable;

import dailyBot.control.connection.dailyFx.DailyFxServerConnection;
import dailyBot.model.Strategy.StrategyId;

public class StrategySignal extends UniqueIdSignal implements Serializable
{
    private static final long serialVersionUID = 771057177601632118L;

    private StrategyId strategyId;
    private boolean buy;
    private Pair pair;
    private int lotNumber;
    private double entryPrice;
    private double VIX;
    private double SSI1;
    private double SSI2;
    private long startDate;
    private int low = Short.MAX_VALUE;
    private int high = Short.MIN_VALUE;
    private boolean stopTouched = false;
    private double stop;
    private double stopDaily = -1;

    public StrategySignal()
    {
        strategyId = null;
        buy = false;
        pair = null;
        entryPrice = Double.NEGATIVE_INFINITY;
        VIX = Double.NEGATIVE_INFINITY;
        SSI1 = Double.NEGATIVE_INFINITY;
        SSI2 = Double.NEGATIVE_INFINITY;
        startDate = Long.MIN_VALUE;
    }

    public StrategySignal(StrategyId strategyId, boolean buy, Pair pair, int lotNumber, double entryPrice, double stop)
    {
        this.strategyId = strategyId;
        this.buy = buy;
        this.pair = pair;
        this.lotNumber = lotNumber;
        this.entryPrice = entryPrice;
        this.VIX = DailyFxServerConnection.getVIX();
        this.SSI1 = pair.pairFatherA().pairSSI();
        this.SSI2 = pair.pairFatherB().pairSSI();
        this.startDate = System.currentTimeMillis();
        this.stop = stop;
    }

    public void setStrategyId(StrategyId strategyId)
    {
        this.strategyId = strategyId;
    }

    public StrategyId getStategyId()
    {
        return strategyId;
    }

    public void setBuy(boolean buy)
    {
        this.buy = buy;
    }

    public boolean isBuy()
    {
        return buy;
    }

    public void setPair(Pair pair)
    {
        if(this.pair != null)
            throw new UnsupportedOperationException("Campo par de SenalEstrategia es inmutable");
        this.pair = pair;
    }

    public Pair getPair()
    {
        return pair;
    }

    public void setEntryPrice(double entryPrice)
    {
        if(this.entryPrice != Double.NEGATIVE_INFINITY)
            throw new UnsupportedOperationException("Campo precioEntrada de SenalEstrategia es inmutable");
        this.entryPrice = entryPrice;
    }

    public double getEntryPrice()
    {
        return entryPrice;
    }

    public void setVIX(double vIX)
    {
        if(this.VIX != Double.NEGATIVE_INFINITY)
            throw new UnsupportedOperationException("Campo VIX de SenalEstrategia es inmutable");
        VIX = vIX;
    }

    public double getVIX()
    {
        return VIX;
    }

    public void setSSI1(double sSI1)
    {
        if(this.SSI1 != Double.NEGATIVE_INFINITY)
            throw new UnsupportedOperationException("Campo SSI1 de SenalEstrategia es inmutable");
        SSI1 = sSI1;
    }

    public double getSSI1()
    {
        return SSI1;
    }

    public void setSSI2(double sSI2)
    {
        if(this.SSI2 != Double.NEGATIVE_INFINITY)
            throw new UnsupportedOperationException("Campo SSI2 de SenalEstrategia es inmutable");
        SSI2 = sSI2;
    }

    public double getSSI2()
    {
        return SSI2;
    }

    public void setStartDate(long startDate)
    {
        if(this.startDate != Long.MIN_VALUE)
            throw new UnsupportedOperationException("Campo fechaInicio de SenalEstrategia es inmutable");
        this.startDate = startDate;
    }

    public long getStartDate()
    {
        return startDate;
    }

    public int currentProfit()
    {
        return pair.differenceInPips(entryPrice, buy);
    }

    public synchronized void setLotNumber(int lotNumber)
    {
        this.lotNumber = lotNumber;
    }

    public synchronized int getLotNumber()
    {
        return lotNumber;
    }

    public synchronized void setLow(int low)
    {
        this.low = low;
    }

    public synchronized int getLow()
    {
        return low;
    }

    public synchronized void setHigh(int high)
    {
        this.high = high;
    }

    public synchronized int getHigh()
    {
        return high;
    }

    public synchronized void setStopTouched(boolean tocoStop)
    {
        this.stopTouched = tocoStop;
    }

    public synchronized boolean isStopTouched()
    {
        return stopTouched;
    }

    public synchronized void setStop(double stop)
    {
        this.stop = stop;
    }

    public synchronized double getStop()
    {
        return stop;
    }

    public synchronized void changeStopDaily(double stopDaily)
    {
        this.stopDaily = stopDaily;
    }

    public synchronized double stopDaily()
    {
        return stopDaily;
    }

    @Override
    public String toString()
    {
        int lotNumber;
        double stop;
        synchronized(this)
        {
            lotNumber = this.lotNumber;
            stop = this.stop;
        }
        return strategyId + " " + (buy ? "Compra" : "Venta") + " " + lotNumber + " Lotes de " + pair + " a: "
            + entryPrice + " Stop: " + stop;
    }
}