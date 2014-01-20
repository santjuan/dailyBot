package dailyBot.model;

import java.io.Serializable;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.model.SignalProvider.SignalProviderId;

public abstract class Filter implements Serializable
{
    private static final long serialVersionUID = 7582836081548863756L;

    public abstract void startFilter(SignalProviderId id);

    public abstract boolean filter(SignalHistoryRecord record);
}