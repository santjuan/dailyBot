package dailyBot.model;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class UniqueIdSignal implements Serializable
{
	private static final long serialVersionUID = 3423708676637926899L;
	
	private Map <String, Long> uniqueIdsMap = new ConcurrentHashMap <String, Long> ();

	public long getUniqueId(String key)
	{
		Long answer = uniqueIdsMap.get(key);
		if(answer == null)
			return 0;
		else
			return answer.longValue();
	}
	
	public void setUniqueId(String key, long value)
	{
		if(value == 0)
			uniqueIdsMap.remove(key);
		else
			uniqueIdsMap.put(key, value);
	}
	
	public TreeMap <String, Long> getUniqueIdsMap()
	{
		return new TreeMap <String, Long> (uniqueIdsMap);
	}
	
	public void setUniqueIdsMap(TreeMap <String, Long> map)
	{
		uniqueIdsMap.clear();
		uniqueIdsMap.putAll(map);
	}
}
