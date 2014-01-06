package dailyBot.model.dailyFx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.TreeMap;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.analysis.Utils;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.model.ExternalProcessFilter;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.view.RMIClientMain;

public class NumpyFilter extends ExternalProcessFilter
{
    private static final long serialVersionUID = -1340889518022207051L;

    protected double percentage;
    protected int pips;

    @Override
    public void startFilter(SignalProviderId id)
    {
        this.percentage = Double.parseDouble(DailyProperties.getProperty("dailyBot.model.dailyFx.NumpyFilter."
            + id.toString() + ".percentage"));
        this.pips = Integer.parseInt(DailyProperties.getProperty("dailyBot.model.dailyFx.NumpyFilter." + id.toString()
            + ".pips"));
        super.startFilter(id);
    }

    public NumpyFilter()
    {
        super();
    }

    public NumpyFilter(SignalProviderId id)
    {
        super(id);
        startFilter(id);
    }

    private static String generateString(SignalHistoryRecord record)
    {
        double[] characteristics = record.getCharacteristics();
        double[] output = record.getOutput();
        String toWrite = characteristics[0] + "";
        for(int i = 1; i < characteristics.length; i++)
            toWrite += " " + characteristics[i];
        for(double d : output)
            toWrite += " " + d;
        return toWrite;
    }

    private static synchronized void write(SignalHistoryRecord record, double percentage, int pips)
    {
        List <SignalHistoryRecord> list = Utils.getRecords();
        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("numpy/matrix.txt"));
            int total = record.getCharacteristics().length + record.getOutput().length;
            String first = percentage + " " + pips;
            for(int i = 2; i < total; i++)
                first += " 0";
            bufferedWriter.write(first + "\n");
            bufferedWriter.write(generateString(record) + "\n");
            long currentTime = System.currentTimeMillis();
            boolean writeAny = false;
            for(SignalHistoryRecord currentRecord : list)
            {
                if((currentTime - currentRecord.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L)
                    && currentRecord.id.equals(record.id) && currentRecord.pair.equals(record.pair)
                    && (currentRecord.buy == record.buy))
                {
                    bufferedWriter.write(generateString(currentRecord) + "\n");
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
                        bufferedWriter.write(generateString(currentRecord) + "\n");
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

    public static synchronized String process(SignalHistoryRecord record, double percentage, int pips,
        TreeMap <SignalHistoryRecord, Boolean> map, SignalProviderId id)
    {
        if(!RMIClientMain.server)
            return "";
        write(record, percentage, pips);
        try
        {
            Process process = Runtime.getRuntime().exec(
                DailyProperties.getProperty("dailyBot.model.dailyFx.NumpyFilter.command"));
            process.waitFor();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String firstOutput = bufferedReader.readLine();
            if(firstOutput == null)
                return "NO";
            firstOutput = firstOutput.trim();
            String secondOutput = bufferedReader.readLine();
            if(secondOutput != null)
                firstOutput = firstOutput + secondOutput;
            firstOutput = firstOutput.trim();
            boolean answer = firstOutput.compareToIgnoreCase("YES") == 0;
            if(answer)
            {
                double percentageOctave = Double.parseDouble(DailyProperties.getProperty("dailyBot.model.dailyFx.OctaveFilter."
                        + id.toString() + ".percentage"));
                int pipsOctave = Integer.parseInt(DailyProperties.getProperty("dailyBot.model.dailyFx.OctaveFilter." + id.toString()
                        + ".pips"));
                firstOutput = OctaveFilter.process(record, percentageOctave, pipsOctave, new TreeMap <SignalHistoryRecord, Boolean>());
                answer = firstOutput.compareToIgnoreCase("YES") == 0;
            }
            map.put(record, answer);
            return firstOutput.toUpperCase();
        }
        catch(Exception e)
        {
            DailyLog.logError("Error de entrada salida en filtroProveedorIA " + e.getMessage());
            return "";
        }
    }

    @Override
    protected String process(SignalHistoryRecord record)
    {
        return process(record, percentage, pips, map, id);
    }

    public double getPercentage()
    {
        return percentage;
    }

    public void setPercentage(double percentage)
    {
        this.percentage = percentage;
    }

    public int getPips()
    {
        return pips;
    }

    public void setPips(int pips)
    {
        this.pips = pips;
    }
}