package dailyBot.model;

import dailyBot.analysis.SignalHistoryRecord;

public class BasicFilter implements Filter
{
    @Override
    public boolean filter(SignalHistoryRecord record)
    {
        return true;
    }

	@Override
	public String getName() 
	{
		return "basic";
	}
}