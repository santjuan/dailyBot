package dailyBot.model.dailyFx;

import dailyBot.analysis.Indicator;
import dailyBot.analysis.Ranges;
import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.model.Filter;
import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;

public class RangeFilter extends Filter
{
    private static final long serialVersionUID = -2416161660423132544L;

    protected Ranges[][] ranges = new Ranges[StrategyId.values().length][Pair.values().length];

    @Override
    public void startFilter(SignalProviderId id)
    {
    }

    public RangeFilter()
    {
        for(int i = 0; i < StrategyId.values().length; i++)
            for(int j = 0; j < Pair.values().length; j++)
                ranges[i][j] = new Ranges();
    }

    @Override
    public boolean filter(SignalHistoryRecord signalHistoryRecord, Object... parameters)
    {
        return ranges[signalHistoryRecord.id.ordinal()][signalHistoryRecord.pair.ordinal()].fulfills(
            signalHistoryRecord, (Boolean) parameters[0], (String) parameters[1]);
    }

    public Ranges getRanges(Pair pair, StrategyId strategyId)
    {
        return ranges[strategyId.ordinal()][pair.ordinal()];
    }

    public Ranges[][] getRanges()
    {
        return ranges;
    }

    public void setRanges(Ranges[][] r)
    {
        for(int i = 0; i < StrategyId.values().length; i++)
            for(int j = 0; j < Pair.values().length; j++)
                for(Indicator indicador : Indicator.values())
                    ranges[i][j].changeRange(indicador, r[i][j].getRange(indicador));
    }
}
