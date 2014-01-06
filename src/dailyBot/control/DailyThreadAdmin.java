package dailyBot.control;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import dailyBot.model.Pair;

public class DailyThreadAdmin
{
    private static final LinkedBlockingQueue<DailyThread> threads = new LinkedBlockingQueue<DailyThread>();
    private static final DailyThread threadMonitor = startThreadMonitor(1);

    private static DailyThread startThreadMonitor(final int number)
    {
        DailyThread monitor = new DailyThread(new DailyRunnable()
        {
            public void run()
            {
                boolean messageSent = false;
                while(true)
                {
                    while(DailyBotMain.marketClosed())
                        DailyThread.sleep(300000L);
                    try
                    {
                        DailyThread.sleep(300000);
                        DailyThreadInfo.registerThreadLoop("Thread monitor");
                        DailyThreadInfo.registerUpdate("Thread monitor", "State", "starting monitor");
                        Calendar calendar = Calendar.getInstance();
                        int minute = calendar.get(Calendar.MINUTE);
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        if(minute > 40)
                            messageSent = false;
                        else
                        {
                            if(!messageSent && (hour == 19))
                            {
                                DailyThreadInfo.registerUpdate("Thread monitor", "State", "checking all threads");
                                DailyLog.acummulateLog();
                                String message = "";
                                for(DailyThread dailyThread : threads)
                                {
                                    DailyThreadInfo.registerUpdate("Thread monitor", "Thread check state",
                                        "checking thread " + dailyThread.getName());
                                    StackTraceElement[] stackTrace = dailyThread.getStackTrace();
                                    message += dailyThread.getName() + " " + dailyThread.getState() + " Stack:\n";
                                    for(StackTraceElement stackTraceElement : stackTrace)
                                        message += stackTraceElement + " * \n";
                                    message += "Ultima actualizacion hace: "
                                        + (System.currentTimeMillis() - dailyThread.runnable.getLastUpdate())
                                        + " milisegundos, limite espera: " + dailyThread.runnable.getUpdateInterval()
                                        + "\n";
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
                                messageSent = true;
                                DailyThread.sleep(900000L);
                                DailyLog.sendAcummulated();
                            }
                        }
                        for(DailyThread dailyThread : threads)
                        {
                            if(dailyThread == threadMonitor)
                                continue;
                            DailyThreadInfo.registerUpdate("Thread monitor", "State", "checking thread for problems "
                                + dailyThread.getName());
                            if(!dailyThread.isAlive())
                            {
                                DailyLog.logError("Error, hilo termino su ejecucion inesperadamente, reiniciando");
                                DailyThreadInfo.registerUpdate("Thread monitor", "State",
                                    "thread ended unexpectedly, rebooting " + dailyThread.getName());
                                DailyLog.tryReboot();
                            }
                            if((System.currentTimeMillis() - dailyThread.runnable.getLastUpdate()) > dailyThread.runnable
                                .getUpdateInterval())
                            {
                                DailyThreadInfo.registerUpdate("Thread monitor", "State",
                                    "thread not updated in a lot of time, rebooting " + dailyThread.getName());
                                DailyLog
                                    .logError("Error, hilo: "
                                        + dailyThread.getName()
                                        + " no se actualizo en mucho tiempo, intervalo aceptable: "
                                        + dailyThread.runnable.getUpdateInterval()
                                        + ", ultima actualizacion hace: "
                                        + (System.currentTimeMillis() - dailyThread.runnable.getLastUpdate() + " haciendo debug"));
                                String message = "";
                                long start = System.currentTimeMillis();
                                Random random = new Random();
                                for(int i = 0; i < 100; i++)
                                {
                                    DailyThread.sleep(random.nextInt(1000));
                                    message += "+ " + (System.currentTimeMillis() - start) + " ms :\n";
                                    for(DailyThread thread : threads)
                                    {
                                        StackTraceElement[] stackTrace = thread.getStackTrace();
                                        message += (stackTrace.length == 0 ? " null" : " "
                                            + Arrays.toString(stackTrace))
                                            + "\n\n";
                                    }
                                }
                                for(int i = 0; i < 50; i++)
                                {
                                    DailyThread.sleep(random.nextInt(5000));
                                    message += "+ " + (System.currentTimeMillis() - start) + " ms :\n";
                                    for(DailyThread thread : threads)
                                    {
                                        StackTraceElement[] stackTrace = thread.getStackTrace();
                                        message += (stackTrace.length == 0 ? " null" : " "
                                            + Arrays.toString(stackTrace))
                                            + "\n\n";
                                    }
                                }
                                DailyLog.logError("Debug :\n" + message);
                                DailyLog.tryReboot();
                            }
                        }
                        Calendar current = Calendar.getInstance();
                        DailyThreadInfo.registerUpdate("Thread monitor", "State", "ending monitor, no problem found");
                        DailyThreadInfo.closeThreadLoop("Thread monitor");
                        DailyLog.logErrorToDisk("Monitor hilos " + number + " corrio sin problemas: "
                            + current.get(Calendar.DAY_OF_MONTH) + "/" + (current.get(Calendar.MONTH) + 1) + "/"
                            + current.get(Calendar.YEAR) + " " + current.get(Calendar.HOUR_OF_DAY) + ":"
                            + current.get(Calendar.MINUTE) + ":" + current.get(Calendar.SECOND) + "."
                            + current.get(Calendar.MILLISECOND));
                    }
                    catch(Exception e)
                    {
                        DailyLog.logError("Error en el monitor de hilos " + e.getMessage());
                    }
                    setLastUpdate(System.currentTimeMillis());
                }
            }
        }, Long.MAX_VALUE);
        monitor.setName("Monitor hilos " + number);
        threads.add(monitor);
        monitor.start();
        return monitor;
    }

    public static String checkThreads()
    {
        String message = "";
        for(DailyThread dailyThread : threads)
        {
            StackTraceElement[] stackTrace = dailyThread.getStackTrace();
            message += dailyThread.getName() + " " + dailyThread.getState() + " Stack:\n";
            for(StackTraceElement stackTraceElement : stackTrace)
                message += stackTraceElement + " * \n";
            message += "Ultima actualizacion hace: "
                + (System.currentTimeMillis() - dailyThread.runnable.getLastUpdate())
                + " milisegundos, limite espera: " + dailyThread.runnable.getUpdateInterval() + "\n";
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

    public static void addThread(DailyThread thread)
    {
        threads.add(thread);
        thread.start();
    }
}