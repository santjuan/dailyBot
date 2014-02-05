package dailyBot.control;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dailyBot.model.Pair;

public class DailyExecutor
{
	private final static ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(5);
	private final static LinkedBlockingQueue <DailyRunnable> runnables = new LinkedBlockingQueue <DailyRunnable> ();
	
	public static void addRunnable(DailyRunnable runnable, long time, TimeUnit unit)
	{
		executor.scheduleWithFixedDelay(runnable, 0, TimeUnit.MILLISECONDS.convert(time, unit), TimeUnit.MILLISECONDS);
		runnables.add(runnable);
	}
	
	public static void addRunnable(DailyRunnable runnable, long periodTime, TimeUnit periodUnit, long initialDelay, TimeUnit initialDelayUnit)
	{
		executor.scheduleWithFixedDelay(runnable, TimeUnit.MILLISECONDS.convert(initialDelay, initialDelayUnit), TimeUnit.MILLISECONDS.convert(periodTime, periodUnit), TimeUnit.MILLISECONDS);
		runnables.add(runnable);
	}
	
    public static void registerRunnableMonitor()
    {
        DailyRunnable monitor = new DailyRunnable("Runnable monitor", Long.MAX_VALUE, true)
        {
            final AtomicBoolean messageSent = new AtomicBoolean(false);
            
            @Override
            public void runOnce()
            {
            	try
            	{
            		DailyLoopInfo.registerLoop("Runnable monitor");
            		DailyLoopInfo.registerUpdate("Runnable monitor", "State", "starting monitor");
            		Calendar calendar = Calendar.getInstance();
            		int minute = calendar.get(Calendar.MINUTE);
            		int hour = calendar.get(Calendar.HOUR_OF_DAY);
            		if(minute > 40)
            			messageSent.set(false);
            		else
            		{
            			if((!messageSent.get()) && (hour == 19))
            			{
            				DailyLoopInfo.registerUpdate("Runnable monitor", "State", "checking all runnables");
            				DailyLog.acummulateLog();
            				String message = "";
            				for(DailyRunnable dailyRunnable : runnables)
            				{
            					DailyLoopInfo.registerUpdate("Runnable monitor", "Runnable check state",
            							"checking runnable " + dailyRunnable.getName());
            					message += "Ultima actualizacion hace: "
            							+ (System.currentTimeMillis() - dailyRunnable.getLastUpdate())
            							+ " milisegundos, limite espera: " + dailyRunnable.getUpdateInterval()
            							+ "\n";
            		            message += "Ultimo run hace: "
            		                    + (System.currentTimeMillis() - dailyRunnable.getLastRun());
            					message += "\n";
            				}
            				for(Pair pair : Pair.values())
            					message += pair.checkSignals();
            				Runtime runtime = Runtime.getRuntime();
            				long kilobytes = 1024L;
            				message += "\nMemoria usada: "
            						+ ((runtime.totalMemory() - runtime.freeMemory()) / kilobytes) + " kb";
            				message += "\nMemoria libre: " + (runtime.freeMemory() / kilobytes) + " kb";
            				message += "\nMemoria total: " + (runtime.totalMemory() / kilobytes) + " kb";
            				message += "\nMemoria limite: " + (runtime.maxMemory() / kilobytes) + " kb";
            				DailyLog.logInfo(message);
            				messageSent.set(true);
            				DailyUtils.sleep(900000L);
            				DailyLog.sendAcummulated();
            			}
            		}
            		for(DailyRunnable dailyRunnable : runnables)
            		{
            			if(dailyRunnable == this)
            				continue;
            			DailyLoopInfo.registerUpdate("Runnable monitor", "State", "checking runnable for problems "
            					+ dailyRunnable.getName());

            			if((System.currentTimeMillis() - dailyRunnable.getLastUpdate()) > dailyRunnable
            					.getUpdateInterval())
            			{
            				DailyLoopInfo.registerUpdate("Runnable monitor", "State",
            						"runnable not updated in a lot of time, rebooting " + dailyRunnable.getName());
            				DailyLog
            				.logError("Error, runnable: "
            						+ dailyRunnable.getName()
            						+ " no se actualizo en mucho tiempo, intervalo aceptable: "
            						+ dailyRunnable.getUpdateInterval()
            						+ ", ultima actualizacion hace: "
            						+ (System.currentTimeMillis() - dailyRunnable.getLastUpdate() + " haciendo debug"));
            				String message = "";
            				long start = System.currentTimeMillis();
            				Random random = new Random();
            				for(int i = 0; i < 100; i++)
            				{
            					DailyUtils.sleep(random.nextInt(1000));
            					message += "+ " + (System.currentTimeMillis() - start) + " ms :\n";
            					for(StackTraceElement[] stackTrace : Thread.getAllStackTraces().values())
            					{
            						message += (stackTrace.length == 0 ? " null" : " "
            								+ Arrays.toString(stackTrace))
            								+ "\n\n";
            					}
            				}
            				DailyLog.logError("Debug :\n" + message);
            				DailyLog.tryReboot();
            			}
            		}
            		DailyLoopInfo.registerUpdate("Runnable monitor", "State", "ending monitor, no problem found");
            	}
            	catch(Exception e)
            	{
            		DailyLog.logError("Error en el monitor de hilos " + e.getMessage());
            	}
            	finally
            	{
            		DailyLoopInfo.closeLoop("Runnable monitor");
            	}
            }
        };
        addRunnable(monitor, 5, TimeUnit.MINUTES);
    }

    public static String checkRunnables()
    {
        String message = "";
        for(DailyRunnable dailyRunnable : runnables)
        {
            message += dailyRunnable.getName() + "\n";
            message += "Ultima actualizacion hace: "
                + (System.currentTimeMillis() - dailyRunnable.getLastUpdate())
                + " milisegundos, limite espera: " + dailyRunnable.getUpdateInterval() + "\n";
            message += "Ultimo run hace: "
                    + (System.currentTimeMillis() - dailyRunnable.getLastRun());
            message += "\n";
        }
        for(Pair pair : Pair.values())
            message += pair.checkSignals();
        Runtime runtime = Runtime.getRuntime();
        long kilobytes = 1024L;
        message += "\nMemoria usada: " + ((runtime.totalMemory() - runtime.freeMemory()) / kilobytes) + " kb";
        message += "\nMemoria libre: " + (runtime.freeMemory() / kilobytes) + " kb";
        message += "\nMemoria total: " + (runtime.totalMemory() / kilobytes) + " kb";
        message += "\nMemoria limite: " + (runtime.maxMemory() / kilobytes) + " kb";
        return message;
    }
}