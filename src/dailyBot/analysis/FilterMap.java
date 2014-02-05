package dailyBot.analysis;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import dailyBot.control.DailyLog;

public class FilterMap implements Serializable
{
	private static final long serialVersionUID = -493259477013147023L;
	
	public static class DoubleArray implements Serializable, Comparable <DoubleArray>
	{
		private static final long serialVersionUID = 8856878561844025513L;
		
		double[] array;
		
		public DoubleArray()
		{
		}
		
		public DoubleArray(SignalHistoryRecord record)
		{
			double[] answer = new double[record.getCharacteristics().length + 4];
			int current = 0;
			for(int i = 0; i < record.getCharacteristics().length; i++)
				answer[current++] = record.getCharacteristics()[i];
			answer[current++] = record.id.ordinal();
			answer[current++] = record.pair.ordinal();
			answer[current++] = record.buy ? 1 : 0;
			answer[current++] = record.openDate;
			array = answer;
		}

		private int compare(double[] o1, double[] o2)
		{
			if(o1.length != o2.length)
				return o1.length - o2.length;
			for(int i = 0; i < o1.length; i++)
				if(o1[i] != o2[i])
					return (int) Math.signum(o1[i] - o2[i]);
			return 0;
		}
		
		
		public double[] getArray() 
		{
			return array;
		}

		public void setArray(double[] array) 
		{
			this.array = array;
		}

		@Override
		public int compareTo(DoubleArray o)
		{
			return compare(array, o.array);
		}
	}

	private ConcurrentHashMap < String, Map <DoubleArray, Boolean> > map = new ConcurrentHashMap < String, Map <DoubleArray, Boolean> > ();

	public ConcurrentHashMap < String, Map<DoubleArray, Boolean> > getMap() 
	{
		return map;
	}

	public void setMap(ConcurrentHashMap < String, Map <DoubleArray, Boolean> > map) 
	{
		this.map = map;
	}
	
	public Map <DoubleArray, Boolean> getFilterMap(String filter)
	{
		if(!map.containsKey(filter))
			map.put(filter, new ConcurrentSkipListMap <DoubleArray, Boolean> ());
		return map.get(filter);
	}
	
    public void writePersistence()
    {
        try
        {
        	FileOutputStream fos = new FileOutputStream("analysis/filterMap.xml");
            XMLEncoder encoder = new XMLEncoder(fos);
            encoder.writeObject(this);
            encoder.close();
            fos.close();
        }
        catch(Exception e)
        {
            DailyLog.logError("Error writing filterMap");
        }
    }
    
    public static FilterMap loadPersistence()
	{
		try
        {
        	if(!new File("analysis/filterMap.xml").exists())
        		return new FilterMap();
        	else
        	{
        		FileInputStream fis = new FileInputStream("analysis/filterMap.xml");
        		XMLDecoder decoder = new XMLDecoder(fis);
                FilterMap answer = (FilterMap) decoder.readObject();
                fis.close();
                decoder.close();
                return answer;
        	}
        }
        catch(Exception e)
        {
            DailyLog.logError("Error reading filterMap");
    		return new FilterMap();
        }
	}
}