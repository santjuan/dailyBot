package dailyBot.model.dailyFx;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import dailyBot.control.DailyLog;
import dailyBot.model.Pair;
import dailyBot.model.StrategySignal;
import dailyBot.model.Strategy.StrategyId;

public class dailyJSON
{
    private List<EstrategiaJSON> Strategy;
    private List<IndicadorJSON> Indicator;
    private List<AlertaJSON> Alert;
    private List<SenalJSON> Signal;
    private static Gson gson;

    public List<EstrategiaJSON> getStrategy()
    {
        return Strategy;
    }

    public void setStrategy(List<EstrategiaJSON> strategy)
    {
        Strategy = strategy;
    }

    public List<IndicadorJSON> getIndicator()
    {
        return Indicator;
    }

    public void setIndicator(List<IndicadorJSON> indicator)
    {
        Indicator = indicator;
    }

    public List<AlertaJSON> getAlert()
    {
        return Alert;
    }

    public void setAlert(List<AlertaJSON> alert)
    {
        Alert = alert;
    }

    public List<SenalJSON> getSignal()
    {
        return Signal;
    }

    public void setSignal(List<SenalJSON> signal)
    {
        Signal = signal;
    }

    static ArrayList<StrategySignal> nuevasSenales = new ArrayList<StrategySignal>();

    public static ArrayList<StrategySignal> leer(String entrada)
    {
        try
        {
            nuevasSenales.clear();
            gson = new Gson();
            dailyJSON dailyJson = gson.fromJson(entrada, dailyJSON.class);
            for(SenalJSON s : dailyJson.Signal)
            {
                boolean compra = s.direction.equals("Buy");
                double stop;
                if(compra)
                    stop = Double.MIN_VALUE;
                else
                    stop = Double.MAX_VALUE;
                for(ComponentesJSON c : s.components)
                {
                    if(c.sl.contains("STOP"))
                        if(compra)
                            stop = Math.max(stop, c.value);
                        else
                            stop = Math.min(stop, c.value);
                }
                s.components = null;
                if(darEstrategia(s.strategyId) == null)
                    continue;
                StrategySignal actual = new StrategySignal(darEstrategia(s.strategyId), compra,
                    Pair.parsePair(s.symbol), s.curOpLots, s.entryPrice, stop);
                nuevasSenales.add(actual);
            }
            dailyJson.Signal = null;
            for(IndicadorJSON i : dailyJson.Indicator)
            {
                Pair actual = Pair.parsePair(i.currency);
                if(actual != null)
                    actual.changeBidAsk(i.bid, i.ask);
            }
            dailyJson.Indicator = null;
            gson = null;
            return nuevasSenales;
        }
        catch(Exception e)
        {
            DailyLog.logError("Error leyendo las senales de dailyFX, en la lectura del JSON: " + e.getMessage());
            throw(new RuntimeException("Error en lectura dailyFX"));
        }
    }

    public static StrategyId darEstrategia(int id)
    {
        switch(id)
        {
        case 5:
            return StrategyId.BREAKOUT2;
        case 6:
            return StrategyId.MOMENTUM2;
        case 10:
            return StrategyId.RANGE2;
        case 11:
            return StrategyId.MOMENTUM1;
        case 12:
            return StrategyId.BREAKOUT1;
        case 14:
            return StrategyId.RANGE1;
        default:
            return null;
        }
    }
}

class EstrategiaJSON
{
    long currentTrades;
    long strategyID;
    double avpPips;
    String shortname;
    double winPct;
    String name;
    String marketDesc;
    int tradesConsidered;
    String timeframe;

    public long getCurrentTrades()
    {
        return currentTrades;
    }

    public void setCurrentTrades(long currentTrades)
    {
        this.currentTrades = currentTrades;
    }

    public long getStrategyID()
    {
        return strategyID;
    }

    public void setStrategyID(long strategyID)
    {
        this.strategyID = strategyID;
    }

    public double getAvpPips()
    {
        return avpPips;
    }

    public void setAvpPips(double avpPips)
    {
        this.avpPips = avpPips;
    }

    public String getShortname()
    {
        return shortname;
    }

    public void setShortname(String shortname)
    {
        this.shortname = shortname;
    }

    public double getWinPct()
    {
        return winPct;
    }

    public void setWinPct(double winPct)
    {
        this.winPct = winPct;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getMarketDesc()
    {
        return marketDesc;
    }

    public void setMarketDesc(String marketDesc)
    {
        this.marketDesc = marketDesc;
    }

    public int getTradesConsidered()
    {
        return tradesConsidered;
    }

    public void setTradesConsidered(int tradesConsidered)
    {
        this.tradesConsidered = tradesConsidered;
    }

    public String getTimeframe()
    {
        return timeframe;
    }

    public void setTimeframe(String timeframe)
    {
        this.timeframe = timeframe;
    }
}

class IndicadorJSON
{
    double volume;
    double bid;
    double ask;
    String currency;
    double volatility;
    double bidChangeDirection;
    double trend;
    double askChangeDirection;

    public double getVolume()
    {
        return volume;
    }

    public void setVolume(double volume)
    {
        this.volume = volume;
    }

    public double getBid()
    {
        return bid;
    }

    public void setBid(double bid)
    {
        this.bid = bid;
    }

    public double getAsk()
    {
        return ask;
    }

    public void setAsk(double ask)
    {
        this.ask = ask;
    }

    public String getCurrency()
    {
        return currency;
    }

    public void setCurrency(String currency)
    {
        this.currency = currency;
    }

    public double getVolatility()
    {
        return volatility;
    }

    public void setVolatility(double volatility)
    {
        this.volatility = volatility;
    }

    public double getBidChangeDirection()
    {
        return bidChangeDirection;
    }

    public void setBidChangeDirection(double bidChangeDirection)
    {
        this.bidChangeDirection = bidChangeDirection;
    }

    public double getTrend()
    {
        return trend;
    }

    public void setTrend(double trend)
    {
        this.trend = trend;
    }

    public double getAskChangeDirection()
    {
        return askChangeDirection;
    }

    public void setAskChangeDirection(double askChangeDirection)
    {
        this.askChangeDirection = askChangeDirection;
    }

    public String toString()
    {
        try
        {
            return Pair.parsePair(currency).toString() + " - bid: " + bid + " - ask: " + ask;
        }
        catch(Exception e)
        {
            return currency + " no existe - " + e.getMessage();
        }
    }
}

class SenalJSON
{
    int curOpLots;
    int strategyId;
    String type;
    long pipMultiplier;
    String strategy;
    String status;
    double entryPrice;
    long id;
    String symbol;
    long standingLots;
    List<ComponentesJSON> components;
    String direction;
    double currentRate;
    double estPnl;
    String shortname;
    long lots;
    String timeframe;

    public String getDirection()
    {
        return direction;
    }

    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    public double getCurrentRate()
    {
        return currentRate;
    }

    public void setCurrentRate(double currentRate)
    {
        this.currentRate = currentRate;
    }

    public double getEstPnl()
    {
        return estPnl;
    }

    public void setEstPnl(double estPnl)
    {
        this.estPnl = estPnl;
    }

    public String getShortname()
    {
        return shortname;
    }

    public void setShortname(String shortname)
    {
        this.shortname = shortname;
    }

    public long getLots()
    {
        return lots;
    }

    public void setLots(long lots)
    {
        this.lots = lots;
    }

    public String getTimeframe()
    {
        return timeframe;
    }

    public void setTimeframe(String timeframe)
    {
        this.timeframe = timeframe;
    }

    public int getCurOpLots()
    {
        return curOpLots;
    }

    public void setCurOpLots(int curOpLots)
    {
        this.curOpLots = curOpLots;
    }

    public int getStrategyId()
    {
        return strategyId;
    }

    public void setStrategyId(int strategyId)
    {
        this.strategyId = strategyId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public long getPipMultiplier()
    {
        return pipMultiplier;
    }

    public void setPipMultiplier(long pipMultiplier)
    {
        this.pipMultiplier = pipMultiplier;
    }

    public String getStrategy()
    {
        return strategy;
    }

    public void setStrategy(String strategy)
    {
        this.strategy = strategy;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public double getEntryPrice()
    {
        return entryPrice;
    }

    public void setEntryPrice(double entryPrice)
    {
        this.entryPrice = entryPrice;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public long getStandingLots()
    {
        return standingLots;
    }

    public void setStandingLots(long standingLots)
    {
        this.standingLots = standingLots;
    }

    public List<ComponentesJSON> getComponents()
    {
        return components;
    }

    public void setComponents(List<ComponentesJSON> components)
    {
        this.components = components;
    }

    public String toString()
    {
        String componentes = "";
        for(ComponentesJSON c : components)
            componentes += c.toString() + " ";
        return dailyJSON.darEstrategia((int) strategyId).toString() + ", lotes: " + curOpLots + ", entrada: "
            + entryPrice + ", par: " + Pair.parsePair(symbol).toString() + ", componentes: {" + componentes + "}";
    }
}

class ComponentesJSON
{
    double value;
    String sl;

    public double getValue()
    {
        return value;
    }

    public void setValue(double value)
    {
        this.value = value;
    }

    public String getSl()
    {
        return sl;
    }

    public void setSl(String sl)
    {
        this.sl = sl;
    }

    public String toString()
    {
        return "valor: " + value + ", sl: " + sl;
    }
}

class AlertaJSON
{
    String timeFrame;
    String type;
    String message;
    long creationDate;
    String name;
    String marketDesc;
    long id;
    String symbol;

    public String getTimeFrame()
    {
        return timeFrame;
    }

    public void setTimeFrame(String timeFrame)
    {
        this.timeFrame = timeFrame;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public long getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(long creationDate)
    {
        this.creationDate = creationDate;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getMarketDesc()
    {
        return marketDesc;
    }

    public void setMarketDesc(String marketDesc)
    {
        this.marketDesc = marketDesc;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }
}