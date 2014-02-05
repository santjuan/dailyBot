package dailyBot.control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dailyBot.control.connection.ChatConnection;
import dailyBot.control.connection.EmailConnection;
import dailyBot.model.Strategy.StrategyId;

public class DailyLog
{
    private static volatile int hour = 0;
    private static volatile int errorCount = 0;
    private static volatile String acummulatedInfo = "";
    private static volatile String acummulatedError = "";
    private static volatile boolean acummulate = false;
    
    public static void logError(String error, boolean sendChat)
    {
    	if(sendChat)
    		logError(error);
    	else
    	{
	        sendMessage("DailyBot-error", error, true, true);
	        checkHourErrors();
    	}
    }

    public static void logError(String error)
    {
        if(!acummulate)
            sendMessage("DailyBot-error", error, true, true);
        else
            acummulatedError += error + "\n";
        ChatConnection.sendMessage(error, true);
        checkHourErrors();
    }

    public static void logInfo(String info)
    {
        if(!acummulate)
            sendMessage("DailyBot-info", info, true, false);
        else
            acummulatedInfo += info + "\n";
    }

    public static void sendAcummulated()
    {
        if(!acummulatedInfo.trim().isEmpty())
            sendMessage("DailyBot-info", acummulatedInfo, true, false);
        if(!acummulatedError.trim().isEmpty())
            sendMessage("DailyBot-error", acummulatedError, true, true);
        acummulatedInfo = "";
        acummulatedError = "";
        acummulate = false;
    }

    public static void acummulateLog()
    {
        acummulate = true;
    }

    public static void logErrorToDisk(String error)
    {
        sendMessage("DailyBot-error", error, false, true);
        if(!error.contains("quota") && !error.contains("Daily"))
            checkHourErrors();
    }

    public static void logInfoWithTitle(String title, String info)
    {
        sendMessage("DailyBot-" + title, info, true, true);
        ChatConnection.sendMessage("DailyBot-" + title + "\n" + info, true);
    }

    private static void sendMessage(String title, String message, boolean email, boolean chat)
    {
        try
        {
        	Calendar calendar = Calendar.getInstance();
        	String number = calendar.get(Calendar.YEAR) + "" + (calendar.get(Calendar.MONTH) + 1) + "" + calendar.get(Calendar.DATE);
            FileWriter fileWriter = new FileWriter(new File("logs/log" + number + (DailyProperties.isTesting() ? "testing" : "") + ".txt"), true);
            fileWriter.write(message);
            fileWriter.write(System.getProperty("line.separator"));
            fileWriter.close();
            if(email)
                EmailConnection.sendEmail(title, message);
        }
        catch(Exception e)
        {
            System.out.println("Error en el gestionador de errores");
        }
    }

    private static void checkHourErrors()
    {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int lastErrorCount;
        synchronized(DailyLog.class.getClass())
        {
            if(hour == currentHour)
                errorCount++;
            else
            {
                hour = currentHour;
                errorCount = 1;
            }
            lastErrorCount = errorCount;
        }
        if(lastErrorCount == 200)
        {
            sendMessage("DailyBot-error", "200 errores en una hora, reiniciando", true, true);
            tryReboot();
        }
    }

    public static void tryReboot()
    {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        for(final StrategyId strategyId : StrategyId.values())
        {
            Future <Boolean> writer = executor.submit(new Callable <Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    strategyId.strategy().writePersistency();
                    return true;
                }
            });
            try
            {
                writer.get(10, TimeUnit.SECONDS);
            }
            catch(Exception e1)
            {
                DailyLog.logError("Al reiniciar imposible escribir " + strategyId);
            }
        }
        reboot();
    }

    public static void tryInmediateReboot()
    {
        reboot();
    }

    private static void reboot()
    {
        try
        {
            String message = "Reiniciando, stack:\n";
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if(stackTrace != null)
                for(StackTraceElement stackTraceElement : stackTrace)
                    message += stackTraceElement + "\n";
            DailyLog.logError(message);
            sendAcummulated();
            DailyUtils.sleep(180000);
            Runtime.getRuntime().exec("service dailybot restart");
            System.exit(0);
            throw(new RuntimeException());
        }
        catch(IOException e)
        {
            DailyLog.logError("Error reiniciando equipo " + e.getMessage());
            reboot();
        }
    }
}