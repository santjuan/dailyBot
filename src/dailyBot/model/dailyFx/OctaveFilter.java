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

public class OctaveFilter extends ExternalProcessFilter
{
    private static final long serialVersionUID = -1340889518022207051L;

    protected double percentage;
    protected int pips;

    @Override
    public void startFilter(SignalProviderId id)
    {
        this.percentage = Double.parseDouble(DailyProperties.getProperty("dailyBot.model.dailyFx.OctaveFilter."
                + id.toString() + ".percentage"));
        this.pips = Integer.parseInt(DailyProperties.getProperty("dailyBot.model.dailyFx.OctaveFilter." + id.toString()
                + ".pips"));
        super.startFilter(id);
    }

    public OctaveFilter()
    {
        super();
    }

    public OctaveFilter(SignalProviderId id)
    {
        super(id);
        startFilter(id);
    }

    private static synchronized void write(SignalHistoryRecord record, double percentage, int pips)
    {
        List<SignalHistoryRecord> list = Utils.getRecords();
        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("octave/matriz.txt"));
            double vix = record.VIX;
            double ssi = Utils.getSSI(record);
            double atr = record.ATR;
            double rsi = record.RSI;
            int profit = record.profit;
            bufferedWriter.write(percentage + " " + pips + " " + 0 + " " + 0 + " " + 0 + "\n");
            bufferedWriter.write(vix + " " + ssi + " " + atr + " " + rsi + " " + profit + "\n");
            long currentTime = System.currentTimeMillis();
            boolean writeAny = false;
            for(SignalHistoryRecord currentRecord : list)
            {
                if((currentTime - currentRecord.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L)
                        && currentRecord.id.equals(record.id) && currentRecord.pair.equals(record.pair)
                        && (currentRecord.buy == record.buy))
                {
                    vix = currentRecord.VIX;
                    ssi = Utils.getSSI(currentRecord);
                    atr = currentRecord.ATR;
                    rsi = currentRecord.RSI;
                    profit = currentRecord.profit;
                    bufferedWriter.write(vix + " " + ssi + " " + atr + " " + rsi + " " + profit + "\n");
                    writeAny = true;
                }
            }
            if(!writeAny)
            {
                for(SignalHistoryRecord currentRecord : list)
                {
                    if(currentRecord.id.equals(record.id) && currentRecord.pair.equals(record.pair))
                    {
                        vix = currentRecord.VIX;
                        ssi = Utils.getSSI(currentRecord);
                        atr = currentRecord.ATR;
                        rsi = currentRecord.RSI;
                        profit = currentRecord.profit;
                        bufferedWriter.write(vix + " " + ssi + " " + atr + " " + rsi + " " + profit + "\n");
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
            TreeMap<SignalHistoryRecord, Boolean> map)
    {
        if(!RMIClientMain.server)
            return "";
        write(record, percentage, pips);
        try
        {
            Process process = Runtime.getRuntime().exec(
                    DailyProperties.getProperty("dailyBot.model.dailyFx.OctaveFilter.command"));
            process.waitFor();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String firstOutput = bufferedReader.readLine().trim();
            String secondOutput = bufferedReader.readLine();
            if(secondOutput != null)
                firstOutput = firstOutput + secondOutput;
            firstOutput = firstOutput.trim();
            boolean answer = firstOutput.compareToIgnoreCase("YES") == 0;
            map.put(record, answer);
            return firstOutput.toUpperCase();
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

    @Override
    protected String process(SignalHistoryRecord record)
    {
        return process(record, percentage, pips, map);
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