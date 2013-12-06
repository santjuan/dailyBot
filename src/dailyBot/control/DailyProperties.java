package dailyBot.control;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

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
}