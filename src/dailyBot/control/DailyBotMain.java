package dailyBot.control;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dailyBot.analysis.DailyAnalysis;
import dailyBot.control.connection.ChatConnection;
import dailyBot.control.connection.SqlConnection;
import dailyBot.control.connection.dailyFx.DailyFxServerConnection;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySystem;
import dailyBot.model.dailyFx.DailyFXStrategySystem;

public class DailyBotMain
{
    private static ArrayList <StrategySystem> systems;

    private static Class <?>[] systemClasses = { DailyFXStrategySystem.class };

    private static void loadStrategySystems()
    {
        systems = new ArrayList <StrategySystem>();
        for(Class <?> clazz : systemClasses)
        {
            try
            {
                systems.add((StrategySystem) (clazz.getConstructor(new Class <?>[0]).newInstance(new Object[0])));
            }
            catch(Exception e)
            {
                DailyLog.logError("Error inicializando la clase: " + clazz.getCanonicalName());
            }
        }
    }

    private static void loadStrategies()
    {
        for(StrategyId strategyId : StrategyId.values())
            strategyId.startStrategy();
    }

    private static void loadSignalProviders()
    {
        for(SignalProviderId signalProviderId : SignalProviderId.values())
            signalProviderId.startSignalProvider();
    }

    private static void startMonitorThreads()
    {
        DailyRunnable SSIVixRunnable = new DailyRunnable("VIX-SSI monitor", 24L * 60L * 60L * 1000L, false)
        {
        	private final AtomicBoolean tenHalf = new AtomicBoolean(false);
        	private final AtomicBoolean nineteenHalf = new AtomicBoolean(false);
        	
            public boolean updateSSI()
            {
                DailyLoopInfo.registerLoop("SSI updater");
                DailyLoopInfo.registerUpdate("SSI updater", "State", "loading SSI");
                try
                {
                    boolean answer = DailyFxServerConnection.loadSSI();
                    DailyLoopInfo.registerUpdate("SSI updater", "State", "SSI loaded: " + answer);
                    return answer;
                }
                catch(RuntimeException e)
                {
                    DailyLoopInfo.registerUpdate("SSI updater", "State",
                        "exception loading: " + e + " " + e.getMessage());
                    throw e;
                }
                finally
                {
                    DailyLoopInfo.closeLoop("SSI updater");
                }
            }

            public void runOnce()
            {
            	try
            	{
            		DailyLoopInfo.registerLoop("SSI-VIX updater");
            		DailyLoopInfo.registerUpdate("SSI-VIX updater", "State", "checking time and updates");
            		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            		int hour = calendar.get(Calendar.HOUR_OF_DAY);
            		int minute = calendar.get(Calendar.MINUTE);
            		if((hour == 8 && minute >= 30) || (hour > 9 && hour < 17))
            		{
            			nineteenHalf.set(false);
            			if(!tenHalf.get())
            				tenHalf.set(updateSSI());
            		}
            		else
            		{
            			tenHalf.set(false);
            			if(!nineteenHalf.get())
            				nineteenHalf.set(updateSSI());
            		}
            		DailyLoopInfo.registerLoop("VIX updater");
            		DailyLoopInfo.registerUpdate("VIX updater", "State", "loading VIX");
            		try
            		{
            			DailyFxServerConnection.loadVix();
            			DailyLoopInfo.registerUpdate("VIX updater", "State", "VIX loaded");
            		}
            		catch(RuntimeException e)
            		{
            			DailyLoopInfo.registerUpdate("VIX updater", "State",
            					"error loading: " + e + " " + e.getMessage());
            			throw e;
            		}
            		finally
            		{
            			DailyLoopInfo.closeLoop("VIX updater");
            			DailyLoopInfo.closeLoop("SSI-VIX updater");
            		}
            	}
            	catch(Exception e)
            	{
            		DailyLog.logError("Error en el hilo monitor de ConexionServidor " + e + " " + e.getMessage());
            		DailyLoopInfo.registerUpdate("SSI-VIX updater", "State",
            				"error in loop: " + e + " " + e.getMessage());
            		DailyLoopInfo.closeLoop("SSI-VIX updater");
            	}
            	finally
            	{
            	}
            }
        };
        DailyExecutor.addRunnable(SSIVixRunnable, 10, TimeUnit.MINUTES);
        DailyRunnable pairsRunnable = new DailyRunnable("Pairs monitor", 360000L, false)
        {
        	public void runOnce()
        	{
        		try
        		{
        			DailyLoopInfo.registerLoop("Pair high-low monitor");
        			for(Pair pair : Pair.values())
        			{
        				DailyLoopInfo.registerUpdate("Pair high-low monitor", "State", "updating pair: " + pair);
        				try
        				{
        					pair.processSignals();
        					DailyLoopInfo.registerUpdate("Pair high-low monitor", "State", "pair updated: "
        							+ pair);
        				}
        				catch(RuntimeException e)
        				{
        					DailyLoopInfo.registerUpdate("Pair high-low monitor", "State",
        							"error updating pair: " + pair + ", " + e + " " + e.getMessage());
        					throw e;
        				}
        			}
        			DailyLoopInfo.registerUpdate("Pair high-low monitor", "State", "all pairs updated");
        		}
        		catch(Exception e)
        		{
        			DailyLog.logError("Error en el monitor de pares " + e + " " + e.getMessage());
        			DailyLoopInfo.registerUpdate("Pair high-low monitor", "State", "error updating pairs: " + e
        					+ " " + e.getMessage());
        		}
        		finally
        		{
        			DailyLoopInfo.closeLoop("Pair high-low monitor");
        		}
        	}
        };
        DailyExecutor.addRunnable(pairsRunnable, 6, TimeUnit.SECONDS, 5, TimeUnit.MINUTES);
        DailyRunnable chatRunnable = new DailyRunnable("Chat monitor", 360000L, true)
        {
            private final ExecutorService executor = Executors.newSingleThreadExecutor();

            public void runOnce()
            {
            	try
            	{
            		DailyLoopInfo.registerLoop("Chat ping");
            		DailyLoopInfo.registerUpdate("Chat ping", "State", "sending ping");
            		Future <String> future = executor.submit(new Callable <String> ()
            		{
            			@Override
            			public String call() throws Exception
            			{
            				ChatConnection.sendMessage("", false);
            				return "";
            			}
            		});
            		future.get(300000, TimeUnit.MILLISECONDS);
            		DailyLoopInfo.registerUpdate("Chat ping", "State", "ping sent");
            	}
            	catch(Exception e)
            	{
            		DailyLog.logError("Error en el monitor del chat " + e.getMessage());
            		DailyLoopInfo.registerUpdate("Chat ping", "State",
            				"error sending ping " + e + " " + e.getMessage());
            	}
            	finally
            	{
            		DailyLoopInfo.closeLoop("Chat ping");
            	}
            }
        };
        DailyExecutor.addRunnable(chatRunnable, 1, TimeUnit.MINUTES, 1, TimeUnit.MINUTES);
        DailyRunnable backupRunnable = new DailyRunnable("Backup", 6000000L, true)
        {
        	final AtomicBoolean done = new AtomicBoolean(false);
        	
            public void runOnce()
            {
            	try
            	{
            		Calendar calendar = Calendar.getInstance();
        			if(calendar.get(Calendar.HOUR_OF_DAY) == 17)
        			{
        				if(!done.get())
        				{
        					SqlConnection.backupDatabase();
        					done.set(true);
        				}
        			}
        			else
        				done.set(false);
            	}
            	catch(Exception e)
            	{
            	}
            }
        };
        DailyExecutor.addRunnable(backupRunnable, 10, TimeUnit.MINUTES, 4, TimeUnit.MINUTES);
        for(SignalProviderId signalProvider : SignalProviderId.values())
        	signalProvider.signalProvider().startPersistenceThread();
        for(StrategySystem system : systems)
            system.startThreads();
        DailyExecutor.registerRunnableMonitor();
    }

    public static void loadProperties()
    {
    	System.setProperty("java.security.policy",
            "file:" + DailyProperties.getProperty("dailyBot.control.DailyBotMain.dailyBotFolder" + (DailyProperties.isTesting() ? ".testing" : "") + "/server.policy"));
    }

    private static final AtomicBoolean lastMarketClosedAnswer = new AtomicBoolean(true);
    
    public static boolean marketClosed()
    {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        boolean answer = (day == Calendar.FRIDAY && hour > 16) || (day == Calendar.SATURDAY)
            || (day == Calendar.SUNDAY && hour <= 16);
        if(answer && (!lastMarketClosedAnswer.get()))
        {
        	try
        	{
        		Runtime.getRuntime().exec(DailyProperties.getProperty("dailyBot.control.DailyBotMain.startCommand"));
        	}
        	catch(Throwable t)
        	{
        	}
        }
        lastMarketClosedAnswer.set(answer);
        return answer;
    }

    public static volatile boolean inLinux = true;

    public static void main(String[] args)
    {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
        if(args.length >= 1 && args[0] != null && args[0].equals("analysis"))
            DailyAnalysis.main(args);
        else
        {
	        if(args.length >= 1 && args[0] != null && args[0].equals("testing"))
	            DailyProperties.setTesting(true);
	        try
	        {
	            Runtime.getRuntime().exec(DailyProperties.getProperty("dailyBot.control.DailyBotMain.linuxCheck"));
	        }
	        catch(Exception e)
	        {
	            inLinux = false;
	        }
	        try
	        {
	            ChatConnection.sendMessage("starting", true);
	        }
	        catch(Exception e)
	        {
	        }
	        DailyLoopInfo.registerLoop("initial thread");
	        if(inLinux)
	        {
	        	DailyLoopInfo.registerUpdate("initial thread", "Backup state", "running first command");
	        	try
	        	{
	        		Runtime.getRuntime().exec(DailyProperties.getProperty("dailyBot.control.DailyBotMain.startCommand"));	
	        	}
	        	catch(Throwable t)
	        	{
	        	}
	        	DailyLoopInfo.registerUpdate("initial thread", "Backup state", "first command ok");
	        }
	        Calendar current = Calendar.getInstance();
	        DailyLog.acummulateLog();
	        DailyLog.logInfo("Iniciando operaciones automaticamente: " + current.get(Calendar.DAY_OF_MONTH) + "/"
	            + (current.get(Calendar.MONTH) + 1) + "/" + current.get(Calendar.YEAR) + " "
	            + current.get(Calendar.HOUR_OF_DAY) + ":" + current.get(Calendar.MINUTE) + ":"
	            + current.get(Calendar.SECOND) + "." + current.get(Calendar.MILLISECOND));
	        loadProperties();
	        DailyLoopInfo.registerUpdate("initial thread", "Strategies load", "loading strategies");
	        loadStrategySystems();
	        loadStrategies();
	        DailyLoopInfo.registerUpdate("initial thread", "Strategies load", "strategies loaded correctly");
	        DailyLoopInfo.registerUpdate("initial thread", "Executors load", "loading executors");
	        loadSignalProviders();
	        DailyLoopInfo.registerUpdate("initial thread", "Executors load", "executors loaded");
	        DailyLoopInfo.registerUpdate("initial thread", "Monitor threads load", "loading monitor threads");
	        startMonitorThreads();
	        DailyLoopInfo.registerUpdate("initial thread", "Monitor threads load", "monitor threads loaded");
	        DailyUtils.sleep(300000);
	        DailyLog.sendAcummulated();
	        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
	        {
	            @Override
	            public void run()
	            {
	                DailyLog.logError("Reiniciando al cerrar la aplicacion");
	                DailyLog.tryInmediateReboot();
	            }
	        }));
	        DailyLoopInfo.registerUpdate("initial thread", "SSI load", "loading first SSI");
	        DailyLoopInfo.registerLoop("SSI updater");
	        DailyFxServerConnection.loadSSI();
	        DailyLoopInfo.closeLoop("SSI updater");
	        DailyLoopInfo.registerUpdate("initial thread", "SSI load", "first SSI loaded");
	        DailyLoopInfo.closeLoop("initial thread");
        }
    }
}