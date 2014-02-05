package dailyBot.analysis;

import dailyBot.control.DailyLog;
import dailyBot.model.SignalProvider.SignalProviderId;

public class SignalProviderRanges extends Ranges
{
    private static final long serialVersionUID = 2028970098750246429L;

    SignalProviderId id;

    public SignalProviderRanges(SignalProviderId id)
    {
        this.id = id;
    }

    @Override
    public boolean fulfills(SignalHistoryRecord record)
    {
        try
        {
        	return Utils.getFilterSignalProvider(id.ordinal()).filter(record, false, 0.0d);
        }
        catch(Exception e)
        {
            DailyLog.logError(e.getMessage() + " Error haciendo la conexion RMI");
            System.exit(0);
            return false;
        }
    }
}