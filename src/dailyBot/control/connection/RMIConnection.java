package dailyBot.control.connection;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import dailyBot.model.MultiFilter;
import dailyBot.model.StrategySignal;

public interface RMIConnection extends Remote
{
    public List<StrategySignal> getStrategySignals(int strategyId) throws RemoteException;

    public List<StrategySignal> getSignalProviderSignals(int signalProviderId) throws RemoteException;

    public boolean getActiveSignalProvider(int signalProviderId, int strategyId, int pairId) throws RemoteException;

    public boolean getOpenSignalProvider(int signalProviderId, int strategyId, int pairId) throws RemoteException;

    public void setActiveSignalProvider(int signalProviderId, int strategyId, int pairId, boolean active, boolean open)
        throws RemoteException;

    public void setActiveFilter(int signalProviderId, int strategyId, int pairId, int newValue)
            throws RemoteException;
    
    public MultiFilter getFilterSignalProvider(int signalProviderId) throws RemoteException;

    public StrategySignal getStrategySignals(int strategyId, int pairId) throws RemoteException;

    public boolean getActive(int signalProviderId) throws RemoteException;

    public void setActive(int signalProviderId, boolean active) throws RemoteException;

    public int getProfitStrategySignal(int strategyId, int pairId) throws RemoteException;
}