package dailyBot.model;

import java.rmi.RemoteException;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.analysis.Utils;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyThreadInfo;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.view.RMIClientMain;

public abstract class ExternalProcessFilter extends Filter
{
	private static final long serialVersionUID = -6554440173950762614L;

	protected TreeMap <SignalHistoryRecord, Boolean> map = new TreeMap <SignalHistoryRecord, Boolean> ();
	private AtomicBoolean loading = new AtomicBoolean(false);
	private AtomicBoolean loaded = new AtomicBoolean(false);
	protected SignalProviderId id;
	
	public ExternalProcessFilter()
	{
	}
	
	public ExternalProcessFilter(SignalProviderId id)
	{
		setId(id);
	}
	
	@Override
	public void startFilter(SignalProviderId id) 
	{
		setId(id);
		if(!loading.get())
		{
			loading.set(true);
			load();
		}
	}
	
	@Override
	public boolean filter(SignalHistoryRecord record, Object... parameters) 
	{
		try
		{
			if(RMIClientMain.connection != null && !RMIClientMain.connection.getActiveSignalProvider(id.ordinal(), record.id.ordinal(), record.pair.ordinal()))
				return false;
			if(RMIClientMain.connection == null && !isActive(record.id, record.pair))
				return false;
		}
		catch (RemoteException e) 
		{
			DailyLog.logRMI("Error preguntando si el proveedor esta activo " + e.getMessage());
		}
		String message = "Intentando abrir " + id.toString() + ", " + record.id.toString() + ", " + record.pair.toString() + ", " + record.pair.getCurrentPrice(record.buy) + ", " + (record.buy ? "BUY" : "SELL");
		if(containKey(map, record))
		{
			if(!message.equals(""))
			{
				message += "\n" + "in map: " + getFilterAnswer(map, record);
				if(RMIClientMain.connection == null)
					DailyLog.logInfoWithTitle("rangos", message);
			}
			return getFilterAnswer(map, record);
		}
		String result = process(record);
		if(!message.equals(""))
		{
			message += "\n" + result;
			if(RMIClientMain.connection == null)
				DailyLog.logInfoWithTitle("rangos", message);
		}
		return result.compareToIgnoreCase("YES") == 0;
	}
	
	protected abstract String process(SignalHistoryRecord record);
	
	protected void load()
	{
		final String className = getClass().getSimpleName();
		new Thread(new Runnable() 
		{
			@Override
			public void run() 
			{
				DailyThreadInfo.registerThreadLoop(className + " filter " + id.toString());
				DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "State", "processing records");
				List <SignalHistoryRecord> records = Utils.getRecords();
				int count = 0;
				for(SignalHistoryRecord record : records)
				{
					process(record);				
					DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "Current record", "processing record " + ++count + "/" + records.size());
				}
				DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "Current record", "all records processed");
				for(StrategyId strategyId : StrategyId.values())
					for(StrategySignal signal : strategyId.strategy().duplicateSignals())
						process(new SignalHistoryRecord(strategyId, signal.getPair(), signal.isBuy(), signal.getStartDate(), System.currentTimeMillis(), -1, signal.getVIX(), signal.getSSI1(), signal.getSSI2(), signal.getLow(), signal.getLow()));
				loaded.set(true);
				DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "State", "finished processing records");
				DailyThreadInfo.closeThreadLoop(className + " filter " + id.toString());
			}
			
		}).start();
	}
	
	public static synchronized Boolean getFilterAnswer(TreeMap <SignalHistoryRecord, Boolean> map, SignalHistoryRecord key)
	{
		return map.get(key);
	}
	
	public static synchronized boolean containKey(TreeMap <SignalHistoryRecord, Boolean> map, SignalHistoryRecord key)
	{
		return map.containsKey(key);
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