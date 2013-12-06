package dailyBot.control;

import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import dailyBot.control.connection.ChatConnection;
import dailyBot.control.connection.RMIConnection;
import dailyBot.control.connection.RMIServerConnection;
import dailyBot.control.connection.dailyFx.DailyFxServerConnection;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySystem;
import dailyBot.model.dailyFx.DailyFXStrategySystem;

public class DailyBotMain
{
    private static ArrayList<StrategySystem> systems;

    private static Class<?>[] systemClasses = { DailyFXStrategySystem.class };

    private static void loadStrategySystems()
    {
        systems = new ArrayList<StrategySystem>();
        for(Class<?> clazz : systemClasses)
        {
            try
            {
                systems.add((StrategySystem) (clazz.getConstructor(new Class<?>[0]).newInstance(new Object[0])));
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
        DailyThread SSIVixThread = new DailyThread(new DailyRunnable()
        {
            public boolean updateSSI()
            {
                DailyThreadInfo.registerThreadLoop("SSI updater");
                DailyThreadInfo.registerUpdate("SSI updater", "State", "loading SSI");
                try
                {
                    boolean answer = DailyFxServerConnection.loadSSI();
                    DailyThreadInfo.registerUpdate("SSI updater", "State", "SSI loaded: " + answer);
                    return answer;
                }
                catch(RuntimeException e)
                {
                    DailyThreadInfo.registerUpdate("SSI updater", "State", "exception loading: " + e + " " + e.getMessage());
                    throw e;
                }
                finally
                {
                    DailyThreadInfo.closeThreadLoop("SSI updater");
                }
            }

            public void run()
            {
                while(true)
                {
                    boolean tenHalf = false;
                    boolean nineteenHalf = false;
                    try
                    {
                        while(DailyBotMain.marketClosed())
                        {
                            DailyThread.sleep(300000L);
                            setLastUpdate(System.currentTimeMillis());
                        }
                        DailyThreadInfo.registerThreadLoop("SSI-VIX updater");
                        DailyThreadInfo.registerUpdate("SSI-VIX updater", "State", "checking time and updates");
                        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                        int hour = calendar.get(Calendar.HOUR_OF_DAY);
                        int minute = calendar.get(Calendar.MINUTE);
                        if((hour == 8 && minute >= 30) || (hour > 9 && hour < 17))
                        {
                            nineteenHalf = false;
                            if(!tenHalf)
                                tenHalf = updateSSI();
                        }
                        else
                        {
                            tenHalf = false;
                            if(!nineteenHalf)
                                nineteenHalf = updateSSI();
                        }
                        DailyThreadInfo.registerThreadLoop("VIX updater");
                        DailyThreadInfo.registerUpdate("VIX updater", "State", "loading VIX");
                        try
                        {
                            DailyFxServerConnection.loadVix();
                            DailyThreadInfo.registerUpdate("VIX updater", "State", "VIX loaded");
                        }
                        catch(RuntimeException e)
                        {
                            DailyThreadInfo.registerUpdate("VIX updater", "State",
                                    "error loading: " + e + " " + e.getMessage());
                            throw e;
                        }
                        finally
                        {
                            DailyThreadInfo.closeThreadLoop("VIX updater");
                            DailyThreadInfo.closeThreadLoop("SSI-VIX updater");
                        }
                        DailyThread.sleep(600000);
                    }
                    catch(Exception e)
                    {
                        DailyLog.logError("Error en el hilo monitor de ConexionServidor " + e + " " + e.getMessage());
                        DailyThreadInfo.registerUpdate("SSI-VIX updater", "State",
                                "error in loop: " + e + " " + e.getMessage());
                        DailyThreadInfo.closeThreadLoop("SSI-VIX updater");
                    }
                    setLastUpdate(System.currentTimeMillis());
                }
            }
        }, 12000000);
        SSIVixThread.setName("Monitor VIX-SSI");
        DailyThreadAdmin.addThread(SSIVixThread);
        DailyThread pairsThread = new DailyThread(new DailyRunnable()
        {
            public void run()
            {
                DailyThread.sleep(180000);
                while(true)
                {
                    try
                    {
                        DailyThread.sleep(30000);
                        DailyThreadInfo.registerThreadLoop("Pair high-low monitor");
                        for(Pair pair : Pair.values())
                        {
                            DailyThreadInfo.registerUpdate("Pair high-low monitor", "State", "updating pair: " + pair);
                            try
                            {
                                pair.processSignals();
                                DailyThreadInfo.registerUpdate("Pair high-low monitor", "State", "pair updated: " + pair);
                            }
                            catch(RuntimeException e)
                            {
                                DailyThreadInfo.registerUpdate("Pair high-low monitor", "State", "error updating pair: "
                                        + pair + ", " + e + " " + e.getMessage());
                                throw e;
                            }
                        }
                        DailyThreadInfo.registerUpdate("Pair high-low monitor", "State", "all pairs updated");
                    }
                    catch(Exception e)
                    {
                        DailyLog.logError("Error en el monitor de pares " + e + " " + e.getMessage());
                        DailyThreadInfo.registerUpdate("Pair high-low monitor", "State", "error updating pairs: " + e + " "
                                + e.getMessage());
                    }
                    finally
                    {
                        DailyThreadInfo.closeThreadLoop("Pair high-low monitor");
                    }
                    setLastUpdate(System.currentTimeMillis());
                }

            }
        }, 360000L);
        pairsThread.setName("Monitor pares");
        DailyThreadAdmin.addThread(pairsThread);
        DailyThread chatThread = new DailyThread(new DailyRunnable()
        {
            public void run()
            {
                DailyThread.sleep(30000);
                while(true)
                {
                    try
                    {
                        DailyThread.sleep(30000);
                        DailyThreadInfo.registerThreadLoop("Chat ping");
                        DailyThreadInfo.registerUpdate("Chat ping", "State", "sending ping");
                        ChatConnection.sendMessage("", false);
                        DailyThreadInfo.registerUpdate("Chat ping", "State", "ping sent");
                    }
                    catch(Exception e)
                    {
                        DailyLog.logError("Error en el monitor del chat " + e.getMessage());
                        DailyThreadInfo.registerUpdate("Chat ping", "State",
                                "error sending ping " + e + " " + e.getMessage());
                    }
                    finally
                    {
                        DailyThreadInfo.closeThreadLoop("Chat ping");
                    }
                    setLastUpdate(System.currentTimeMillis());
                }
            }
        }, 360000L);
        chatThread.setName("Monitor chat");
        DailyThreadAdmin.addThread(chatThread);
        for(StrategySystem system : systems)
            system.startThread();
    }

    private static void exit(int currentSystem)
    {
        if(currentSystem == systems.size())
        {
            for(SignalProviderId signalProviderId : SignalProviderId.values())
                signalProviderId.signalProvider().writePersistence();
            for(StrategyId strategyId : StrategyId.values())
                strategyId.strategy().writePersistency();
            System.exit(0);
        }
        else
        {
            systems.get(currentSystem).lockSystem();
            systems.get(currentSystem).writePersistence();
            systems.get(currentSystem).unlockSystem();
            exit(currentSystem + 1);
        }
    }

    public static void exit()
    {
        if(systems != null)
            exit(0);
        else
            System.exit(0);
    }

    public static void loadProperties()
    {
        System.setProperty("java.rmi.server.codebase",
                "file:" + DailyProperties.getProperty("dailyBot.control.DailyBotMain.dailyBotFolder") + "/bin/");
        System.setProperty("java.security.policy",
                "file:" + DailyProperties.getProperty("dailyBot.control.DailyBotMain.dailyBotFolder") + "/server.policy");
    }

    public static boolean marketClosed()
    {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return (day == Calendar.FRIDAY && hour > 16) || (day == Calendar.SATURDAY) || (day == Calendar.SUNDAY && hour <= 16);
    }

    public static volatile boolean inLinux = true;

    public static void main(String[] args) throws IOException
    {
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
        DailyThreadInfo.registerThreadLoop("initial thread");
        if(inLinux)
        {
            DailyThreadInfo.registerUpdate("initial thread", "Backup state", "making mysql backup");
            Runtime.getRuntime().exec(DailyProperties.getProperty("dailyBot.control.DailyBotMain.startCommand"));
            DailyThreadInfo.registerUpdate("initial thread", "Backup state", "backup ok");
        }
        Calendar current = Calendar.getInstance();
        DailyLog.acummulateLog();
        DailyLog.logInfo("Iniciando operaciones automaticamente: " + current.get(Calendar.DAY_OF_MONTH) + "/"
                + (current.get(Calendar.MONTH) + 1) + "/" + current.get(Calendar.YEAR) + " "
                + current.get(Calendar.HOUR_OF_DAY) + ":" + current.get(Calendar.MINUTE) + ":"
                + current.get(Calendar.SECOND) + "." + current.get(Calendar.MILLISECOND));
        loadProperties();
        if(inLinux)
        {
            try
            {
                if("true".equals(DailyProperties.getProperty("dailyBot.control.DailyBotMain.RMI")))
                {
                    DailyThreadInfo.registerUpdate("initial thread", "RMI state", "starting RMI connection");
                    String name = "Conexion";
                    RMIServerConnection connection = new RMIServerConnection();
                    RMIConnection stub = (RMIConnection) UnicastRemoteObject.exportObject(connection, 0);
                    Registry registry = LocateRegistry.getRegistry(DailyProperties
                            .getProperty("dailyBot.vista.VentanaPrincipal.direccionRMI"));
                    registry.rebind(name, stub);
                    DailyThreadInfo.registerUpdate("initial thread", "RMI state", "RMI connection ok");
                }
            }
            catch(Exception e)
            {
                DailyLog.logError(e.getMessage() + " Error haciendo la conexion RMI");
                DailyLog.tryInmediateReboot();
            }
        }
        DailyThreadInfo.registerUpdate("initial thread", "Strategies load", "loading strategies");
        loadStrategySystems();
        loadStrategies();
        DailyThreadInfo.registerUpdate("initial thread", "Strategies load", "strategies loaded correctly");
        DailyThreadInfo.registerUpdate("initial thread", "Executors load", "loading executors");
        loadSignalProviders();
        DailyThreadInfo.registerUpdate("initial thread", "Executors load", "executors loaded");
        DailyThreadInfo.registerUpdate("initial thread", "Monitor threads load", "loading monitor threads");
        startMonitorThreads();
        DailyThreadInfo.registerUpdate("initial thread", "Monitor threads load", "monitor threads loaded");
        DailyThread.sleep(300000);
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
        DailyThreadInfo.registerUpdate("initial thread", "SSI load", "loading first SSI");
        DailyThreadInfo.registerThreadLoop("SSI updater");
        DailyFxServerConnection.loadSSI();
        DailyThreadInfo.closeThreadLoop("SSI updater");
        DailyThreadInfo.registerUpdate("initial thread", "SSI load", "first SSI loaded");
        DailyThreadInfo.closeThreadLoop("initial thread");
    }
}