package dailyBot.analysis;

import java.beans.XMLDecoder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.model.Filter;
import dailyBot.model.MultiFilter;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;

public class DailyAnalysis
{
	public static void updateAnalysis()
	{
		DailyLog.logError("Starting updating analysis", false);
		FilterMap filterMap = new FilterMap();
		Utils.reloadRecords();
		List <SignalHistoryRecord> allRecords = Utils.getRecords();
		for(SignalProviderId id : SignalProviderId.values())
		{
			MultiFilter multiFilter = new MultiFilter(id);
			multiFilter.startFilters();
			for(Filter filter : multiFilter.filters())
			{
				Map <FilterMap.DoubleArray, Boolean> map = filterMap.getFilterMap(id.toString() + "." + filter.getName());
				for(SignalHistoryRecord record : allRecords)
					map.put(new FilterMap.DoubleArray(record), filter.filter(record));
			}
		}
		filterMap.writePersistence();
		for(StrategyId strategy : StrategyId.values())
		{
			for(Pair pair : Pair.values())
			{
				for(boolean buy : new boolean[]{true, false})
				{
					File directory = new File("analysis/rawData/" + strategy.toString() + "/" + pair.toString() + "/");
					directory.mkdirs();
					File file = new File("analysis/rawData/" + strategy.toString() + "/" + pair.toString() + "/" + (buy ? "buy_" : "sell_") + dailyBot.model.Utils.getId(strategy, pair, buy) + ".txt");
					file.delete();
					try
					{
						BufferedWriter bw = new BufferedWriter(new FileWriter(file));
						for(SignalHistoryRecord record : allRecords)
							bw.write(record.generateLine() + "\n");
						bw.close();
					}
					catch(Exception e)
					{
					}
				}
			}
		}
		DailyLog.logError("Analysis updated", false);
	}
	
	private static class MapFilter implements Filter
	{
		private final Map <FilterMap.DoubleArray, Boolean> map;
		
		public MapFilter(Map <FilterMap.DoubleArray, Boolean> map)
		{
			this.map = map;
		}
		
		@Override
		public boolean filter(SignalHistoryRecord record) 
		{
			if(map.containsKey(new FilterMap.DoubleArray(record)))
				return map.get(new FilterMap.DoubleArray(record));
			else
				return false;
		}

		@Override
		public String getName()
		{
			return "MapFilter";
		}
	}
	
	public static MultiFilter readMultiFilter(SignalProviderId id)
	{
		try
		{
			FileInputStream fis = new FileInputStream("filters/" + id + ".xml");
			XMLDecoder decoder = new XMLDecoder(fis);
	        MultiFilter answer = (MultiFilter) decoder.readObject();
	        fis.close();
	        decoder.close();
	        return answer;
		}
		catch(Exception e)
		{
			return new MultiFilter(id);
		}
	}
	
	public static MultiFilter getFilterSignalProvider(SignalProviderId id)
	{
		FilterMap filterMap = FilterMap.loadPersistence();
		MultiFilter multiFilter = readMultiFilter(id);
		multiFilter.startFilters();
		Filter[] filtersBefore = multiFilter.filters();
		Filter[] newFilters = new Filter[filtersBefore.length];
		for(int i = 0; i < filtersBefore.length; i++)
			newFilters[i] = new MapFilter(filterMap.getFilterMap(id.toString() + "." + filtersBefore[i].getName()));
		multiFilter.changeFilters(newFilters);
		return multiFilter;
	}
	
	public static String getFilterNameSignalProvider(SignalProviderId id, int i) 
	{
		MultiFilter multiFilter = readMultiFilter(id);
		multiFilter.startFilters();
		return multiFilter.filters()[i].getName();
	}
	
	public static void main(String[] args)
	{
		DailyProperties.setAnalysis(true);
		DailyProperties.setVerbose(true);
		boolean saved = false;
		updateAnalysis();
		while(true)
		{
			Calendar calendar = Calendar.getInstance();
			if(calendar.get(Calendar.HOUR_OF_DAY) == 18)
			{
				if(!saved)
					updateAnalysis();
				saved = true;
			}
			else
				saved = false;
		}
	}
}