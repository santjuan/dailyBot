package dailyBot.model;

import dailyBot.analysis.SignalHistoryRecord;

public abstract interface Filter
{
    public boolean filter(SignalHistoryRecord record);
    public String getName();
}