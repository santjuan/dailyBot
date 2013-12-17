package dailyBot.control.connection.zulutrade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import com.zulutrade.configuration.ZuluTradeAuthenticationInfo;
import com.zulutrade.configuration.ZuluTradingServerConfig;
import com.zulutrade.trading.UniqueIdGenerator;
import com.zulutrade.trading.ZuluTradingGateway;
import com.zulutrade.trading.ZuluTradingGatewayImpl;
import com.zulutrade.trading.dtos.NewMarketTradeResponseMessage;
import com.zulutrade.trading.dtos.TradeClosedResponseMessage;
import com.zulutrade.trading.dtos.UpdateValueResponseMessage;

import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyThread;
import dailyBot.model.Broker;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySignal;
import dailyBot.model.UniqueIdSignal;
import dailyBot.model.Utils;

public class ZulutradeConnection implements Broker
{
    private class InstantZulutradeConnection
    {
        ZuluTradingServerConfig cfg;
        ZuluTradeAuthenticationInfo ai;
        ZuluTradingGateway tradingGateway;

        private InstantZulutradeConnection(SignalProviderId id)
        {
            cfg = new ZuluTradingServerConfig("http://tradingserver.zulutrade.com/");
            ai = new ZuluTradeAuthenticationInfo(
                DailyProperties.getProperty("dailyBot.control.connection.zulutrade.ZulutradeConnection." + id
                    + ".username"),
                DailyProperties.getProperty("dailyBot.control.connection.zulutrade.ZulutradeConnection." + id
                    + ".password"));
            tradingGateway = new ZuluTradingGatewayImpl(ai, cfg);
        }
    }

    public static class ZulutradeException extends Exception
    {
        private static final long serialVersionUID = -3162825748109413066L;

        public ZulutradeException(Exception cause)
        {
            super(cause);
        }
    }

    public class ZulutradeSignal
    {
        public final boolean isError;
        public final int errorCode;
        public final String errorMessage;
        public final long uniqueId;
        public final long dateOpened;
        public final Pair currency;
        public final boolean isBuy;
        public final double entryPrice;
        public final double stop;
        public final double limit;

        public ZulutradeSignal(NewMarketTradeResponseMessage signal) throws ZulutradeException
        {
            try
            {
                if(signal.getErrorCode() != 0)
                {
                    isError = true;
                    errorCode = signal.getErrorCode();
                    errorMessage = signal.getErrorMessage();
                    uniqueId = 0;
                    dateOpened = 0;
                    currency = null;
                    isBuy = true;
                    entryPrice = stop = limit = 0;
                }
                else
                {
                    isError = false;
                    errorCode = 0;
                    errorMessage = null;
                    if(signal.getUniqueId().contains("."))
                        uniqueId = Long
                            .parseLong(signal.getUniqueId().substring(signal.getUniqueId().indexOf('.') + 1));
                    else
                        uniqueId = Long.parseLong(signal.getUniqueId());
                    dateOpened = signal.getOpenUtcTimestamp();
                    currency = Pair.stringToPair(signal.getCurrencyName().replace("/", ""));
                    isBuy = signal.isBuy();
                    entryPrice = signal.getEntryRate();
                    stop = signal.getStopValue();
                    limit = signal.getLimitValue();
                }
            }
            catch(Exception e)
            {
                throw new ZulutradeException(e);
            }
        }

        @Override
        public String toString()
        {
            return "ZulutradeSignal [isError=" + isError + ", errorCode=" + errorCode + ", errorMessage="
                + errorMessage + ", uniqueId=" + uniqueId + ", dateOpened=" + dateOpened + ", currency=" + currency
                + ", isBuy=" + isBuy + ", entryPrice=" + entryPrice + ", stop=" + stop + ", limit=" + limit + "]";
        }
    }

    SignalProviderId id;

    public ZulutradeConnection(SignalProviderId id)
    {
        this.id = id;
    }

    public ZulutradeSignal[] listOpenSignals() throws ZulutradeException
    {
        try
        {
            ArrayList <ZulutradeSignal> all = new ArrayList <ZulutradeSignal>();
            for(NewMarketTradeResponseMessage marketTrade : new InstantZulutradeConnection(id).tradingGateway
                .getOpenTrades().getOpenPositions())
                all.add(new ZulutradeSignal(marketTrade));
            return all.toArray(new ZulutradeSignal[0]);
        }
        catch(Exception e)
        {
            throw new ZulutradeException(e);
        }
    }

    public ZulutradeSignal findSignalById(long id) throws ZulutradeException
    {
        try
        {
            for(ZulutradeSignal signal : listOpenSignals())
                if(signal.uniqueId == id)
                    return signal;
            return null;
        }
        catch(Exception e)
        {
            throw new ZulutradeException(e);
        }
    }

    public boolean changeLimit(long id, double newLimit) throws ZulutradeException
    {
        try
        {
            ZulutradeSignal signal = findSignalById(id);
            if(signal == null || signal.isError)
                throw new ZulutradeException(new Exception("Error changing limit: " + signal));
            Pair currency = signal.currency;
            UpdateValueResponseMessage updateResponse = new InstantZulutradeConnection(this.id).tradingGateway
                .updateLimit(currency.toString().substring(0, 3) + "/" + currency.toString().substring(3),
                    signal.isBuy, 1, id + "", newLimit);
            if(updateResponse == null)
                throw new ZulutradeException(new Exception("Null change limit response for id: " + id));
            if(!updateResponse.isSuccess())
                DailyLog.logError("Not possible to change limit: " + updateResponse.getErrorCode() + " "
                    + updateResponse.getErrorMessage());
            return updateResponse.isSuccess();
        }
        catch(Exception e)
        {
            throw new ZulutradeException(e);
        }
    }

    public boolean changeStop(long id, double newStop) throws ZulutradeException
    {
        try
        {
            ZulutradeSignal signal = findSignalById(id);
            if(signal == null || signal.isError)
                throw new ZulutradeException(new Exception("Error changing stop: " + signal));
            Pair currency = signal.currency;
            UpdateValueResponseMessage updateResponse = new InstantZulutradeConnection(this.id).tradingGateway
                .updateStop(currency.toString().substring(0, 3) + "/" + currency.toString().substring(3), signal.isBuy,
                    1, id + "", newStop);
            if(updateResponse == null)
                throw new ZulutradeException(new Exception("Null change stop response for id: " + id));
            if(!updateResponse.isSuccess())
                DailyLog.logError("Not possible to change stop: " + updateResponse.getErrorCode() + " "
                    + updateResponse.getErrorMessage());
            return updateResponse.isSuccess();
        }
        catch(Exception e)
        {
            throw new ZulutradeException(e);
        }
    }

    private final AtomicLong lastOpened = new AtomicLong(0);

    public synchronized ZulutradeSignal openTrade(Pair currency, boolean isBuy, double limit, double stop)
        throws ZulutradeException
    {
        try
        {
            long difference = System.currentTimeMillis() - lastOpened.get();
            if(difference >= 0 && difference < 16000)
                DailyThread.sleep(16000 - difference);
            lastOpened.set(System.currentTimeMillis());
            long uniqueId = Long.parseLong(UniqueIdGenerator.getNextUniqueId());
            ZulutradeSignal answer = new ZulutradeSignal(new InstantZulutradeConnection(id).tradingGateway.openMarket(
                currency.toString().substring(0, 3) + "/" + currency.toString().substring(3), 1, isBuy,
                currency.getCurrentPrice(isBuy), uniqueId + ""));
            if(answer.isError)
                throw new ZulutradeException(new Exception("Not possible to open: " + answer));
            if(!changeStop(answer.uniqueId, stop))
                throw new ZulutradeException(new Exception(
                    "Error setting initial stop, trade possibly opened without stop"));
            if(!changeLimit(answer.uniqueId, limit))
                throw new ZulutradeException(new Exception(
                    "Error setting initial limit, trade possibly opened without limit"));
            ZulutradeSignal signal = findSignalById(answer.uniqueId);
            if(signal == null)
                throw new ZulutradeException(new Exception("Not possible to open: " + answer));
            return signal;
        }
        catch(Exception e)
        {
            throw new ZulutradeException(e);
        }
    }

    public boolean closeTrade(long id) throws ZulutradeException
    {
        try
        {
            ZulutradeSignal signal = findSignalById(id);
            if(signal == null)
                throw new ZulutradeException(new Exception("Signal with id: " + id + ", not found"));
            TradeClosedResponseMessage closedResponse = new InstantZulutradeConnection(this.id).tradingGateway
                .closeMarket(
                    signal.currency.toString().substring(0, 3) + "/" + signal.currency.toString().substring(3), 1,
                    signal.isBuy, signal.uniqueId + "", signal.currency.getCurrentPrice(signal.isBuy));
            if(closedResponse == null)
                throw new ZulutradeException(new Exception("Null close response for id: " + id));
            return closedResponse.isSuccess();
        }
        catch(Exception e)
        {
            throw new ZulutradeException(e);
        }
    }

    SignalProviderId idProveedor;

    @Override
    public void checkConsistency()
    {
        try
        {
            ZulutradeSignal[] signals = listOpenSignals();
            TreeSet <Long> ids = new TreeSet <Long>();
            for(ZulutradeSignal s : signals)
                ids.add(s.uniqueId);
            StrategySignal[] strategySignals = Utils.getAllSignals();
            for(StrategySignal signal : strategySignals)
            {
                if(signal != null && signal.getUniqueId("zulutrade-" + id.toString()) != 0
                    && !ids.contains(signal.getUniqueId("zulutrade-" + id.toString())))
                {
                    DailyLog.logError("Senal: " + signal
                        + ", no existia realmente en zulutrade, quitando id. Senales existentes: "
                        + Arrays.toString(signals));
                    signal.setUniqueId("zulutrade-" + id.toString() + "-old",
                        signal.getUniqueId("zulutrade-" + id.toString()));
                    signal.setUniqueId("zulutrade-" + id.toString(), 0L);
                }
                else if(signal != null && signal.getUniqueId("zulutrade-" + id.toString()) != 0)
                {
                    if(signal.getPair().differenceInPips(signal.getStop(), signal.isBuy()) < 0
                        && Math.abs(signal.getStop()) > 1e-5d
                        && ((signal.getLotNumber() < 4) || (signal.getPair().differenceInPips(signal.getStop(),
                            signal.stopDaily(), signal.isBuy()) > 0)))
                    {
                        closeSignal(signal, signal.getStrategyId(), signal.getPair(), signal.isBuy());
                        DailyLog.logError("Senal: " + signal + " toco stop, cerrando. Precio actual = "
                            + signal.getPair().getCurrentPrice(signal.isBuy()));
                        signal.setStopTouched(true);
                    }
                    else
                        signal.setUniqueId("zulutrade-" + id.toString() + "-lastchecktime", System.currentTimeMillis());
                }
                else if(signal != null && signal.getUniqueId("zulutrade-" + id.toString() + "-old") != 0
                    && ids.contains(signal.getUniqueId("zulutrade-" + id.toString() + "-old")))
                {
                    DailyLog.logError("Senal: " + signal + ", volvio a aparecer, restaurando: "
                        + Arrays.toString(signals));
                    signal.setUniqueId("zulutrade-" + id.toString(),
                        signal.getUniqueId("zulutrade-" + id.toString() + "-old"));
                    signal.setUniqueId("zulutrade-" + id.toString() + "-old", 0L);
                }
            }
        }
        catch(Exception e)
        {
            if((countErrorsZulu.incrementAndGet() % 30) == 0)
                DailyLog.logError(e.getMessage());
        }
    }

    private String zuluString(ZulutradeSignal zuluSignal, StrategySignal dailySignal)
    {
        return dailySignal.getStrategyId() + " " + (dailySignal.isBuy() ? "Compra" : "Venta") + " "
            + dailySignal.getLotNumber() + " Lotes de " + zuluSignal.currency + " a: " + zuluSignal.entryPrice
            + " Stop: " + zuluSignal.stop;
    }

    @Override
    public String checkConsistencyFull(boolean sendMessage)
    {
        checkConsistency();
        try
        {
            ZulutradeSignal[] signals = listOpenSignals();
            TreeMap <Long, ZulutradeSignal> ids = new TreeMap <Long, ZulutradeSignal>();
            for(ZulutradeSignal s : signals)
                ids.put(s.uniqueId, s);
            StrategySignal[] strategySignals = Utils.getAllSignals();
            String messages = id.toString();
            for(StrategySignal signal : strategySignals)
            {
                if(signal != null && signal.getUniqueId("zulutrade-" + id.toString()) != 0)
                {
                    messages += "\n";
                    messages += signal.getUniqueId("zulutrade-" + id.toString()) + "\n";
                    messages += signal + "\n";
                    messages += zuluString(ids.get(signal.getUniqueId("zulutrade-" + id.toString())), signal) + "\n";
                    ids.remove(signal.getUniqueId("zulutrade-" + id.toString()));
                }
                else if(signal != null)
                {
                    messages += "\n";
                    messages += signal + "\n";
                    messages += "Magico zulutrade = 0\n";
                }
            }
            if(sendMessage)
                DailyLog.logInfo(messages);
            messages = "";
            for(ZulutradeSignal signal : ids.values())
            {
                messages += "\n";
                messages += signal.uniqueId + "\n";
                messages += signal + " abierta manualmente.\n";
            }
            if(!messages.trim().isEmpty())
                DailyLog.logInfo(messages);
            return messages;
        }
        catch(Exception e)
        {
            DailyLog.logError(e.getMessage());
            return e.getMessage();
        }
    }

    static final AtomicLong countErrorsZulu = new AtomicLong(-1);

    @Override
    public boolean openSignal(UniqueIdSignal affected, StrategyId strategyId, Pair pair, boolean buy)
    {
        try
        {
            DailyLog.logInfoWithTitle("rangos", id + " abriendo senal: " + strategyId + ", " + pair);
            DailyLog.logError("Abriendo zulutrade: ");
            ZulutradeSignal signal = openTrade(pair, buy, pair.getCurrentPriceMinus(-300, buy),
                pair.getCurrentPriceMinus(75, buy));
            affected.setUniqueId("zulutrade-" + id.toString(), signal.uniqueId);
            DailyLog.logError("Abierta: " + signal + "");
            return true;
        }
        catch(Exception e)
        {
            DailyLog.logError("Zulutrade error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean closeSignal(UniqueIdSignal affected, StrategyId strategyId, Pair pair, boolean buy)
    {
        try
        {
            DailyLog.logInfoWithTitle("rangos", id + " cerrando senal: " + strategyId + ", " + pair + ", zulutrade: "
                + findSignalById(affected.getUniqueId("zulutrade-" + id.toString())));
            closeTrade(affected.getUniqueId("zulutrade-" + id.toString()));
            affected.setUniqueId("zulutrade-" + id.toString(), 0L);
            return true;
        }
        catch(Exception e)
        {
            DailyLog.logError("Zulutrade error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public long getUniqueId(UniqueIdSignal signal)
    {
        return signal.getUniqueId("zulutrade-" + id.toString());
    }

    @Override
    public void setUniqueId(UniqueIdSignal signal, long value)
    {
        signal.setUniqueId("zulutrade-" + id.toString(), value);
    }

    @Override
    public boolean openManualSignal(Pair pair, boolean buy)
    {
        try
        {
            return !openTrade(pair, buy, pair.getCurrentPriceMinus(-300, buy), pair.getCurrentPriceMinus(75, buy)).isError;
        }
        catch(Exception e)
        {
            DailyLog.logError("Zulutrade error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean closeManualSignal(long id)
    {
        try
        {
            return closeTrade(id);
        }
        catch(Exception e)
        {
            DailyLog.logError("Zulutrade error: " + e.getMessage());
            return false;
        }
    }
}