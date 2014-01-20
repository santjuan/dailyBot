package dailyBot.model;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.model.SignalProvider.SignalProviderId;

public class BasicFilter extends Filter
{
    private static final long serialVersionUID = 766214723267275304L;

    SignalProviderId id;

    @Override
    public void startFilter(SignalProviderId id)
    {
        setId(id);
    }

    @Override
    public boolean filter(SignalHistoryRecord record)
    {
        return true;
    }

    public synchronized SignalProviderId getId()
    {
        return id;
    }

    public synchronized void setId(SignalProviderId id)
    {
        this.id = id;
    }
}