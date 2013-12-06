package dailyBot.analysis;

import dailyBot.control.DailyLog;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.view.RMIClientMain;

public class SignalProviderRanges extends Ranges
{
	private static final long serialVersionUID = 2028970098750246429L;
	
	SignalProviderId id;
	
	public SignalProviderRanges(SignalProviderId id)
	{
		this.id = id;
	}
	
	@Override
	public boolean fulfills(SignalHistoryRecord record, boolean ignoreInfo, String sendMessage) 
	{
		try 
		{
			if(id == null || RMIClientMain.connection.getActiveSignalProvider(id.ordinal(), record.id.ordinal(), record.pair.ordinal()))
				return RMIClientMain.connection.getFilterSignalProvider(id.ordinal()).filter(record, true, "", id);
			else
				return false;
		} 
        catch (Exception e)
        {        	
        	DailyLog.logError(e.getMessage() + " Error haciendo la conexion RMI");
        	System.exit(0);
        	return false;
        }
	}
}