package dailyBot.control;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class DailyProperties
{
    private static Properties properties = loadProperties();

    private static Properties loadProperties()
    {
        Properties properties = new Properties();
        try
        {
            if(new File("dailyBotRelease.conf").exists())
                properties.load(new FileInputStream("dailyBotRelease.conf"));
            else
                properties.load(new FileInputStream("dailyBot.conf"));
        }
        catch(Exception e)
        {
            DailyLog.logError(e.getMessage() + " error cargando las propiedades");
        }
        return properties;
    }

    public static String getProperty(String property)
    {
        return properties.getProperty(property);
    }

    private static final AtomicBoolean testing = new AtomicBoolean(false);

    public static boolean isTesting()
    {
        return testing.get();
    }

    public static void setTesting(boolean isTesting)
    {
        testing.set(isTesting);
    }
    
    private static final AtomicBoolean verbose = new AtomicBoolean(false);

    public static boolean isVerbose()
    {
        return verbose.get();
    }

    public static void setVerbose(boolean isVerbose)
    {
        verbose.set(isVerbose);
    }  
}