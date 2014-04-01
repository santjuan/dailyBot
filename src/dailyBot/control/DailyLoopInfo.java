package dailyBot.control;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public class DailyLoopInfo
{
    static class LastUpdate
    {
        public String name;
        public Map<String, String> updates = new ConcurrentHashMap<String, String>();
        long updateCreationTime = System.currentTimeMillis();

        LastUpdate(String name)
        {
            this.name = name;
            if(!uniqueIds.containsKey(name))
            {
                uniqueIds.put(name, currentUniqueId.incrementAndGet());
                uniqueIdsReverse.put(uniqueIds.get(name), name);
            }
        }
    }

    private static AtomicLong currentUniqueId = new AtomicLong(0);
    private static final Map<String, LastUpdate> building = new ConcurrentHashMap<String, LastUpdate>();
    private static final Map<String, Long> uniqueIds = new ConcurrentHashMap<String, Long>();
    private static final Map<Long, String> uniqueIdsReverse = new ConcurrentSkipListMap<Long, String>();
    private static final Map<String, LastUpdate> lastUpdates = new ConcurrentHashMap<String, LastUpdate>();

    public static void registerLoop(String name)
    {
        building.put(name, new LastUpdate(name));
    }

    public static void registerUpdate(String threadName, String key, String value)
    {
        LastUpdate current = building.get(threadName);
        if(current == null)
        {
        	if(!DailyProperties.isAnalysis())
        		DailyLog.logError("Error " + threadName + ", tried to register " + key + " before registering loop");
        }
        else
            current.updates.put(key, value);
    }

    public static void closeLoop(String name)
    {
        LastUpdate possible = building.get(name);
        if(possible != null)
            lastUpdates.put(name, possible);
        building.remove(name);
    }

    public static String listLoops(long minutesAgo)
    {
        long time = System.currentTimeMillis();
        time -= minutesAgo * 60L * 1000L;
        String answer = "";
        for(Map.Entry<Long, String> entry : uniqueIdsReverse.entrySet())
        {
            LastUpdate update = building.get(entry.getValue());
            if(update == null)
                update = lastUpdates.get(entry.getValue());
            if(update == null || (update.updateCreationTime >= time))
                answer += update.name + " " + uniqueIds.get(update.name) + "\n";
        }
        return answer + "\n";
    }

    public static String getLastUpdateInfo(long uniqueId, boolean current)
    {
        String answer = "";
        String threadName = "";
        for(Map.Entry<String, Long> entry : uniqueIds.entrySet())
            if(entry.getValue().longValue() == uniqueId)
                threadName = entry.getKey();
        LastUpdate update = (current && building.containsKey(threadName)) ? building.get(threadName) : lastUpdates
            .get(threadName);
        if(update == null)
            update = lastUpdates.get(threadName);
        if(update == null)
            return "Never updated\n";
        answer += update.name + '\n';
        answer += (System.currentTimeMillis() - update.updateCreationTime) + " milliseconds ago\n\n";
        for(Map.Entry<String, String> entry : update.updates.entrySet())
            answer += entry.getKey() + ": " + entry.getValue() + "\n";
        return answer + '\n';
    }
}