package dailyBot.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.analysis.Utils;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.connection.EmailConnection;

public class ExternalProcessFilter implements Filter
{
	public static String getUniqueName(String name)
    {
    	return name.intern();
    }
	
    private final String name;
    private final double[] parameters;
    private final int loopCount;
    
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
    	loopCount = Integer.parseInt(DailyProperties.getProperty("dailyBot.model.ExternalProcessFilter." + propertyName + ".loopCount"));
    }

    @Override
    public boolean filter(SignalHistoryRecord record)
    {
        return process(record);
    }

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
            boolean writeAny = false;
            for(SignalHistoryRecord currentRecord : list)
            {
                if(Utils.isRelevant(currentRecord.openDate) &&
                   currentRecord.id.equals(record.id) && 
                   currentRecord.pair.equals(record.pair) &&
                   (currentRecord.buy == record.buy) && currentRecord != record)
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
    
    public final static Map <String, Integer> lastHit = Collections.synchronizedMap(new TreeMap <String, Integer> ());

    private boolean process(SignalHistoryRecord record)
    {
    	synchronized(name)
    	{
	        write(record);
	        try
	        {
	        	for(int i = 0; i < loopCount; i++)
	        	{
		            Process process = Runtime.getRuntime().exec(
		                DailyProperties.getProperty("dailyBot.model.ExternalProcessFilter." + name + ".command"));
		            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		            process.waitFor();
		            try
		            {
			            String firstOutput = bufferedReader.readLine().trim();
			            String secondOutput = bufferedReader.readLine();
			            if(secondOutput != null)
			                firstOutput = firstOutput + secondOutput;
			            firstOutput = firstOutput.trim();
			            if(firstOutput.toUpperCase().compareToIgnoreCase("YES") != 0)
			            {
			            	if(!lastHit.containsKey(name))
			            	{			            		
			            		lastHit.put(name, -1);
			            		if(i > 0)
			            			EmailConnection.sendEmail("DailyBot-error", "Starting " + name + " - new: " + lastHit.get(name), EmailConnection.SUPERADMINS);
			            	}
			            	if(i > lastHit.get(name))
			            	{
			            		if(i > 0)
			            			EmailConnection.sendEmail("DailyBot-error", "Increasing " + name + " - new: " + i + ", before: " + lastHit.get(name), EmailConnection.SUPERADMINS);
			            		lastHit.put(name, i);
			            	}
			            	return false;
			            }
		            }
		            catch(Exception e)
		            {
		            }
	        	}
	        	return true;
	        }
	        catch(IOException e)
	        {
	            DailyLog.logError("Error de entrada salida en filtroProveedorIA " + e.getMessage());
	            return false;
	        }
	        catch(InterruptedException e)
	        {
	            DailyLog.logError("Error de interrupcion en filtroProveedorIA " + e.getMessage());
	            return false;
	        }
    	}
    }

    @Override
    public String getName()
    {
    	return name;
    }
}