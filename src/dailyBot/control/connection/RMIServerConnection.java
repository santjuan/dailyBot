package dailyBot.control.connection;

import java.rmi.RemoteException;
import java.util.List;

import dailyBot.model.MultiFilter;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySignal;

public class RMIServerConnection extends BasicConnection implements RMIConnection
{
    public static class Local implements RMIConnection
    {
        RMIConnection server;

        static Boolean[][][] dpActive = new Boolean[SignalProviderId.values().length][StrategyId.values().length][Pair
            .values().length];
        static MultiFilter[] dpFilters = new MultiFilter[SignalProviderId.values().length];

        public Local(RMIConnection server)
        {
            this.server = server;
            try
            {
                this.server.getStrategySignals(0, 0);
            }
            catch(RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public synchronized void setActiveSignalProvider(int signalProviderId, int strategyId, int pairId,
            boolean active, boolean open) throws RemoteException
        {
        	 if(dpFilters[signalProviderId] != null)
                 dpFilters[signalProviderId].changeActive(StrategyId.values()[strategyId], Pair.values()[pairId], active);
            server.setActiveSignalProvider(signalProviderId, strategyId, pairId, active, open);
            dpActive[signalProviderId][strategyId][pairId] = active;
        }

        @Override
        public synchronized boolean getOpenSignalProvider(int signalProviderId, int strategyId, int pairId) throws RemoteException
        {
            return server.getOpenSignalProvider(signalProviderId, strategyId, pairId);
        }

        @Override
        public synchronized boolean getActiveSignalProvider(int signalProviderId, int strategyId, int pairId)
            throws RemoteException
        {
            if(dpActive[signalProviderId][strategyId][pairId] != null)
                return dpActive[signalProviderId][strategyId][pairId];
            return dpActive[signalProviderId][strategyId][pairId] = server.getActiveSignalProvider(signalProviderId,
                strategyId, pairId);
        }

        @Override
        public synchronized MultiFilter getFilterSignalProvider(int signalProviderId) throws RemoteException
        {
            if(dpFilters[signalProviderId] != null)
                return dpFilters[signalProviderId];
            return dpFilters[signalProviderId] = server.getFilterSignalProvider(signalProviderId);
        }

        @Override
        public synchronized List<StrategySignal> getStrategySignals(int strategyId) throws RemoteException
        {
            return server.getStrategySignals(strategyId);
        }

        @Override
        public synchronized List<StrategySignal> getSignalProviderSignals(int signalProviderId) throws RemoteException
        {
            return server.getSignalProviderSignals(signalProviderId);
        }

        @Override
        public synchronized int getProfitStrategySignal(int strategyId, int pairId) throws RemoteException
        {
            return server.getProfitStrategySignal(strategyId, pairId);
        }

        @Override
        public synchronized StrategySignal getStrategySignals(int strategyId, int pairId) throws RemoteException
        {
            return server.getStrategySignals(strategyId, pairId);
        }

        @Override
        public synchronized boolean getActive(int signalProviderId) throws RemoteException
        {
            return server.getActive(signalProviderId);
        }

        @Override
        public synchronized void setActive(int signalProviderId, boolean active) throws RemoteException
        {
            if(dpFilters[signalProviderId] != null && (dpFilters[signalProviderId].isActive() == active))
                return;
            if(dpFilters[signalProviderId] != null)
                dpFilters[signalProviderId].setActive(active);
            dpActive = new Boolean[SignalProviderId.values().length][StrategyId.values().length][Pair.values().length];
            server.setActive(signalProviderId, active);
        }

		@Override
		public synchronized void setActiveFilter(int signalProviderId, int strategyId,
				int pairId, int newValue) throws RemoteException 
		{
			if(dpFilters[signalProviderId] != null)
                dpFilters[signalProviderId].changeActiveFilter(StrategyId.values()[strategyId], Pair.values()[pairId], newValue);
			server.setActiveFilter(signalProviderId, strategyId, pairId, newValue);
		}
    }

    @Override
    public void setActiveSignalProvider(int signalProviderId, int strategyId, int pairId, boolean active, boolean open)
        throws RemoteException
    {
        SignalProviderId.values()[signalProviderId].signalProvider().setActive(StrategyId.values()[strategyId],
            Pair.values()[pairId], active);
        if(active && open)
            SignalProviderId.values()[signalProviderId].signalProvider().openActive(StrategyId.values()[strategyId],
                Pair.values()[pairId]);
    }

    @Override
    public boolean getActiveSignalProvider(int signalProviderId, int strategyId, int pairId) throws RemoteException
    {
        return SignalProviderId.values()[signalProviderId].signalProvider().getActive(StrategyId.values()[strategyId],
            Pair.values()[pairId]);
    }

    @Override
    public boolean getOpenSignalProvider(int signalProviderId, int strategyId, int pairId) throws RemoteException
    {
        return SignalProviderId.values()[signalProviderId].signalProvider().isOpen(StrategyId.values()[strategyId],
            Pair.values()[pairId]);
    }

    @Override
    public MultiFilter getFilterSignalProvider(int signalProviderId) throws RemoteException
    {
        return SignalProviderId.values()[signalProviderId].signalProvider().getFilter();
    }

    @Override
    public List<StrategySignal> getStrategySignals(int strategyId) throws RemoteException
    {
        return StrategyId.values()[strategyId].strategy().duplicateSignals();
    }

    @Override
    public List<StrategySignal> getSignalProviderSignals(int strategyId) throws RemoteException
    {
        return SignalProviderId.values()[strategyId].signalProvider().providerSignals();
    }

    @Override
    public int getProfitStrategySignal(int strategyId, int pairId) throws RemoteException
    {
        return StrategyId.values()[strategyId].strategy().hasPair(Pair.values()[pairId]).currentProfit();
    }

    @Override
    public StrategySignal getStrategySignals(int strategyId, int pairId) throws RemoteException
    {
        return StrategyId.values()[strategyId].strategy().hasPair(Pair.values()[pairId]);
    }

    @Override
    public boolean getActive(int signalProviderId) throws RemoteException
    {
        return SignalProviderId.values()[signalProviderId].signalProvider().filterActive();
    }

    @Override
    public void setActive(int signalProviderId, boolean active) throws RemoteException
    {
        SignalProviderId.values()[signalProviderId].signalProvider().changeFilterActive(active);
    }

	@Override
	public void setActiveFilter(int signalProviderId, int strategyId,
			int pairId, int newValue) throws RemoteException
	{
		SignalProviderId.values()[signalProviderId].signalProvider().changeActiveFilter(StrategyId.values()[strategyId], Pair.values()[pairId], newValue);
	}
}