package dailyBot.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.analysis.Utils;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;

public class ExternalProcessFilter implements Filter
{
	public static String getUniqueName(String name)
    {
    	return name.intern();
    }
	
    private final String name;
    private final double[] parameters;
    
    public ExternalProcessFilter(String name, String propertyName)
    {
    	this.name = getUniqueName(name);
    	ArrayList <Double> params = new ArrayList <Double> ();
    	for(int i = 0; true; i++)
    	{
    		String property = DailyProperties.getProperty("dailyBot.model.ExternalProcessFilter." + propertyName + ".arg" + i);
    		if(property == null)
    			break;
    		params.add(Double.parseDouble(property));
    	}
    	parameters = new double[params.size()];
    	for(int i = 0; i < params.size(); i++)
    		parameters[i] = params.get(i);
    }

    @Override
    public boolean filter(SignalHistoryRecord record)
    {
        String result = process(record);
        return result.compareToIgnoreCase("YES") == 0;
    }

//    protected void load()
//    {
//        final String className = getClass().getSimpleName();
//        new Thread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                DailyThreadInfo.registerThreadLoop(className + " filter " + id.toString());
//                DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "State", "processing records");
//                List<SignalHistoryRecord> records = Utils.getRecords();
//                int count = 0;
//                for(SignalHistoryRecord record : records)
//                {
//                    process(record);
//                    DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "Current record",
//                        "processing record " + ++count + "/" + records.size());
//                }
//                DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "Current record",
//                    "all records processed");
//                for(StrategyId strategyId : StrategyId.values())
//                    for(StrategySignal signal : strategyId.strategy().duplicateSignals())
//                        process(new SignalHistoryRecord(strategyId, signal.getPair(), signal.isBuy(),
//                            signal.getStartDate(), System.currentTimeMillis(), -1, signal.getVIX(), signal.getSSI1(),
//                            signal.getSSI2(), signal.getLow(), signal.getLow()));
//                loaded.set(true);
//                DailyThreadInfo.registerUpdate(className + " filter " + id.toString(), "State",
//                    "finished processing records");
//                DailyThreadInfo.closeThreadLoop(className + " filter " + id.toString());
//            }
//
//        }).start();
//    }
//
//    public static synchronized Boolean getFilterAnswer(TreeMap<SignalHistoryRecord, Boolean> map,
//        SignalHistoryRecord key)
//    {
//        return map.get(key);
//    }
//
//    public static synchronized boolean containKey(TreeMap<SignalHistoryRecord, Boolean> map, SignalHistoryRecord key)
//    {
//        return map.containsKey(key);
//    }

    private void write(SignalHistoryRecord record)
    {
        List <SignalHistoryRecord> list = Utils.getRecords();
        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(
            		DailyProperties.getProperty("dailyBot.model.ExternalProcessFilter." + name + ".file")));
            int total = record.getCharacteristics().length + record.getOutput().length;
            String first = "";
            for(double d : parameters)
            	first += " " + d;
            for(int i = parameters.length; i < total - 1; i++)
                first += " 0";
            first += " " + dailyBot.model.Utils.getId(record);
            first = first.trim();
            bufferedWriter.write(first + "\n");
            bufferedWriter.write(record.generateLine() + "\n");
            long currentTime = System.currentTimeMillis();
            boolean writeAny = false;
            for(SignalHistoryRecord currentRecord : list)
            {
                if((currentTime - currentRecord.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L)
                    && currentRecord.id.equals(record.id) && currentRecord.pair.equals(record.pair)
                    && (currentRecord.buy == record.buy) && currentRecord != record)
                {
                    bufferedWriter.write(currentRecord.generateLine() + "\n");
                    writeAny = true;
                }
            }
            if(!writeAny)
            {
                for(SignalHistoryRecord currentRecord : list)
                {
                    if(currentRecord.id.equals(record.id) && currentRecord.pair.equals(record.pair)
                        && (currentRecord.buy == record.buy))
                    {
                        bufferedWriter.write(currentRecord.generateLine() + "\n");
                        writeAny = true;
                    }
                }
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch(IOException e)
        {
            DailyLog.logError("Error escribiendo registros en filtroProveedorIA " + e.getMessage());
        }
    }

    private String process(SignalHistoryRecord record)
    {
    	synchronized(name)
    	{
	        write(record);
	        try
	        {
	            Process process = Runtime.getRuntime().exec(
	                DailyProperties.getProperty("dailyBot.model.ExternalProcessFilter." + name + ".command"));
	            process.waitFor();
	            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	            try
	            {
		            String firstOutput = bufferedReader.readLine().trim();
		            String secondOutput = bufferedReader.readLine();
		            if(secondOutput != null)
		                firstOutput = firstOutput + secondOutput;
		            firstOutput = firstOutput.trim();
		            return firstOutput.toUpperCase();
	            }
	            catch(Exception e)
	            {
	            	return "NO";
	            }
	        }
	        catch(IOException e)
	        {
	            DailyLog.logError("Error de entrada salida en filtroProveedorIA " + e.getMessage());
	            return "";
	        }
	        catch(InterruptedException e)
	        {
	            DailyLog.logError("Error de interrupcion en filtroProveedorIA " + e.getMessage());
	            return "";
	        }
    	}
    }

    @Override
    public String getName()
    {
    	return name;
    }
}