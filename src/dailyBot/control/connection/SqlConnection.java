package dailyBot.control.connection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.analysis.StatisticsUtils.PairHistory;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyUtils;
import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySignal;

public class SqlConnection
{
    private static void close(ResultSet resultSet)
    {
        try
        {
            if(resultSet != null)
                resultSet.close();
        }
        catch(Exception e)
        {
            DailyLog.logError(e.getMessage() + " Error cerrando un result set");
        }
    }

    private static void close(Statement statement)
    {
        try
        {
            if(statement != null)
                statement.close();
        }
        catch(Exception e)
        {
            DailyLog.logError(e.getMessage() + " Error cerrando un statement");
        }
    }

    private static String formatDate(long date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        String dateString = "'" + calendar.get(Calendar.YEAR);
        dateString += "-" + (calendar.get(Calendar.MONTH) + 1);
        dateString += "-" + calendar.get(Calendar.DATE);
        dateString += " " + calendar.get(Calendar.HOUR_OF_DAY);
        dateString += ":" + calendar.get(Calendar.MINUTE);
        dateString += ":" + calendar.get(Calendar.SECOND);
        dateString += "'";
        return dateString;
    }

    private static String formatDate(Calendar calendar)
    {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day;
    }

    private static final String createTableSignalHistoryRecord = "CREATE TABLE IF NOT EXISTS SignalHistoryRecord ("
        + "Id int(11), " + "StrategyId tinyint(4) NOT NULL, " + "CloseDate datetime NOT NULL, "
        + "Pair tinyint(4) NOT NULL, " + "Profit smallint(6) NOT NULL, " + "VIX double NOT NULL, "
        + "SSI1 double NOT NULL, " + "SSI2 double NOT NULL, " + "IsBuy tinyint(1) NOT NULL DEFAULT '1', "
        + "OpenDate datetime NOT NULL, " + "High smallint(6) NOT NULL DEFAULT '-32768', "
        + "Low smallint(6) NOT NULL DEFAULT '32767', " + "PRIMARY KEY (Id)" + ")";

    private static final AtomicBoolean signalHistoryRecordCreated = new AtomicBoolean();
                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
    public static void addRecord(StrategyId strategyId, StrategySignal signal)
    {
        if(!signalHistoryRecordCreated.get())
        {
            synchronized(SqlConnection.class)
            {
                if(!signalHistoryRecordCreated.get())
                {
                    signalHistoryRecordCreated.set(true);
                    executeSql(createTableSignalHistoryRecord);
                }
            }
        }
        Connection connection = getConnection();
        try
        {
            long dateLong = System.currentTimeMillis();
            int profit = signal.currentProfit();
            if(profit > 2000)
            {
                DailyLog.logError("Entrada sospechosa: " + strategyId.name() + ", " + signal.getPair().name() + ", "
                    + dateLong + ", ganancia: " + profit + "?");
                return;
            }
            if(signal.isIgnoreSignal())
            {
            	DailyLog.logError("Ignorando senal " + signal.toString() + " fue marcada para ser ignorada");
            	return;
            }
            double VIX = signal.getVIX();
            double SSI1 = signal.getSSI1();
            double SSI2 = signal.getSSI2();
            Statement statement = null;
            try
            {
                statement = connection.createStatement();
                int id = 0;
                ResultSet rs = null;
                try
                {
                	rs = statement.executeQuery("SELECT Id FROM SignalHistoryRecord ORDER BY id DESC LIMIT 1");
                	if(rs.next())
                		id = rs.getInt(1);
                }
                catch(Exception e)
                {
                	close(rs);
                }
                id++;
                statement.setQueryTimeout(60);
                statement
                    .executeUpdate("INSERT INTO SignalHistoryRecord (Id,StrategyId,CloseDate,Pair,Profit,VIX,SSI1,SSI2,IsBuy,OpenDate,High,Low) VALUES("
                        + id + ","
                    	+ strategyId.ordinal()
                        + ","
                        + formatDate(dateLong)
                        + ","
                        + signal.getPair().ordinal()
                        + ","
                        + profit
                        + ","
                        + VIX
                        + ","
                        + SSI1
                        + ","
                        + SSI2
                        + ","
                        + (signal.isBuy() ? 1 : 0)
                        + ","
                        + formatDate(signal.getStartDate()) + "," + signal.getHigh() + "," + signal.getLow() + ")");
            }
            catch(SQLException s)
            {
                DailyLog.logError("Error escribiendo a la base de datos: " + strategyId.toString() + ", "
                    + signal.getPair().toString() + ", " + dateLong + ", " + profit);
                try
                {
                    connection.close();
                }
                catch(Exception e)
                {
                }
                connection = null;
            }
            finally
            {
                close(statement);
            }
        }
        finally
        {
            returnConnection(connection);
        }
    }

    private static final String createTableATR = "CREATE TABLE IF NOT EXISTS ATR (" + "Pair tinyint(4) NOT NULL, "
        + "Date date NOT NULL, " + "Open double NOT NULL, " + "Close double NOT NULL, " + "Low double NOT NULL, "
        + "High double NOT NULL, " + "PRIMARY KEY (Pair, Date))";

    private static final AtomicBoolean ATRCreated = new AtomicBoolean();

    public static void addPairData(Pair pair, String date, double open, double close, double low, double high)
    {
        if(!ATRCreated.get())
        {
            synchronized(SqlConnection.class)
            {
                if(!ATRCreated.get())
                {
                    ATRCreated.set(true);
                    executeSql(createTableATR);
                }
            }
        }
        Connection connection = getConnection();
        Statement statement = null;
        try
        {
            statement = connection.createStatement();
            statement.setQueryTimeout(60);
            statement.executeUpdate("delete from ATR where Pair=" + pair.ordinal() + " and Date='" + date + "'");
            statement.executeUpdate("INSERT INTO ATR (Pair,Date,Open,Close,Low,High) VALUES(" + pair.ordinal() + ",'" + date
                + "'," + open + "," + close + "," + low + "," + high + ")");
        }
        catch(SQLException s)
        {
            DailyLog.logError(s.getMessage() + " Error agregando datos par");
            try
            {
                connection.close();
            }
            catch(Exception e)
            {
            }
            connection = null;
        }
        finally
        {
            close(statement);
            returnConnection(connection);
        }
    }

    public static void addPairData(Pair pair, double open, double close, double low, double high)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        String date = formatDate(calendar);
        if(calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
            && calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
            addPairData(pair, date, open, close, low, high);
    }

    public static double[] getPairData(Pair pair, Calendar date)
    {
        if(!ATRCreated.get())
        {
            synchronized(SqlConnection.class)
            {
                if(!ATRCreated.get())
                {
                    ATRCreated.set(true);
                    executeSql(createTableATR);
                }
            }
        }
        String dateString = formatDate(date);
        Connection connection = getConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        try
        {
            statement = connection.createStatement();
            statement.setQueryTimeout(60);
            resultSet = statement.executeQuery("select * from ATR where Pair=" + pair.ordinal() + " and Date='"
                + dateString + "'");
            if(resultSet.next())
                return new double[] { resultSet.getDouble("Low"), resultSet.getDouble("High"),
                    resultSet.getDouble("Open"), resultSet.getDouble("Close") };
            else
                return new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                    Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
        }
        catch(SQLException s)
        {
            DailyLog.logError(s.getMessage() + ": error cargando datos par " + pair.toString() + ", dia " + dateString);
            try
            {
                connection.close();
            }
            catch(Exception e)
            {
            }
            connection = null;
            return null;
        }
        finally
        {
            close(resultSet);
            close(statement);
            returnConnection(connection);
        }
    }

    static class PairHistoryCache
    {
        private static EnumMap <Pair, TreeMap <Date, PairHistory>> cache = getCache();
        private static AtomicLong lastLoadTime = new AtomicLong();
        
        static synchronized EnumMap <Pair, TreeMap <Date, PairHistory>> getCache()
        {
        	DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            EnumMap <Pair, TreeMap <Date, PairHistory>> cache = new EnumMap <Pair, TreeMap <Date, PairHistory>>(
                Pair.class);
            for(Pair pair : Pair.values())
                cache.put(pair, new TreeMap <Date, PairHistory>());
            Connection connection = getConnection();
            Statement statement = null;
            ResultSet resultSet = null;
            try
            {
                statement = connection.createStatement();
                statement.setQueryTimeout(60);
                resultSet = statement.executeQuery("select * from ATR");
                while(resultSet.next())
                {
                    Date date = df.parse(resultSet.getString("Date"));
                    Calendar temp = Calendar.getInstance();
                    temp.setTime(date);
                    temp.set(Calendar.HOUR_OF_DAY, 19);
                    cache.get(Pair.values()[resultSet.getInt("Pair")]).put(
                        temp.getTime(),
                        new PairHistory(resultSet.getDate("Date"), resultSet.getDouble("Low"), resultSet
                            .getDouble("High"), resultSet.getDouble("Open"), resultSet.getDouble("Close")));
                }
            }
            catch(Exception s)
            {
                DailyLog.logError(s.getMessage() + ": error cargando la cache en CacheHistoriaPares");
                try
                {
                    connection.close();
                }
                catch(Exception e)
                {
                }
                connection = null;
            }
            finally
            {
                close(resultSet);
                close(statement);
                returnConnection(connection);
            }
            return cache;
        }

        static synchronized SortedMap <Date, PairHistory> getUpUntil(Pair pair, Date until)
        {
            if((cache.get(pair).tailMap(until).isEmpty()) && ((System.currentTimeMillis() - lastLoadTime.get()) >= 3600000L))
            {
            	cache = getCache();
            	lastLoadTime.set(System.currentTimeMillis());
            }
            Calendar temp = Calendar.getInstance();
            temp.setTimeInMillis(until.getTime());
            temp.set(Calendar.HOUR_OF_DAY, 20);
            temp.add(Calendar.DAY_OF_MONTH, -1);
            return cache.get(pair).subMap(java.sql.Date.valueOf("1900-01-01"), until);
        }
    }

    public static SortedMap <Date, PairHistory> getPairHistory(Pair pair, Date date)
    {
        return PairHistoryCache.getUpUntil(pair, date);
    }

    static boolean executeSql(String sql)
    {
        Connection connection = getConnection();
        try
        {
            for(int i = 0; i < 10; i++)
            {
                Statement statement = null;
                try
                {
                    statement = connection.createStatement();
                    statement.setQueryTimeout(60);
                    return statement.execute(sql);
                }
                catch(SQLException e)
                {
                    try
                    {
                        connection.close();
                    }
                    catch(Exception e1)
                    {
                    }
                    connection = null;
                    returnConnection(connection);
                    connection = getConnection();
                }
                finally
                {
                    close(statement);
                }
            }
            return false;
        }
        finally
        {
            returnConnection(connection);
        }
    }

    static String querySql(String selectSql)
    {
        Connection connection = getConnection();
        try
        {
            for(int i = 0; i < 100; i++)
            {
                Statement statement = null;
                ResultSet resultSet = null;
                try
                {
                    statement = connection.createStatement();
                    statement.setQueryTimeout(60);
                    resultSet = statement.executeQuery(selectSql);
                    if(resultSet.next())
                        return resultSet.getString(1);
                    else
                        return "";
                }
                catch(SQLException e)
                {
                    DailyLog.logError("Error ejecutando query " + selectSql);
                    DailyUtils.sleep(6000L);
                    try
                    {
                        connection.close();
                    }
                    catch(Exception e1)
                    {
                    }
                    connection = null;
                    returnConnection(connection);
                    connection = getConnection();
                }
                finally
                {
                    close(resultSet);
                    close(statement);
                }
            }
            DailyLog.tryInmediateReboot();
            return null;
        }
        finally
        {
            returnConnection(connection);
        }
    }

    public static List <SignalHistoryRecord> getRecords()
    {
        if(!signalHistoryRecordCreated.get())
        {
            synchronized(SqlConnection.class)
            {
                if(!signalHistoryRecordCreated.get())
                {
                    signalHistoryRecordCreated.set(true);
                    executeSql(createTableSignalHistoryRecord);
                }
            }
        }
        Connection connection = getExclusiveConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        try
        {
            statement = connection.createStatement();
            statement.setQueryTimeout(60);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            resultSet = statement.executeQuery("select * from SignalHistoryRecord");
            LinkedList <SignalHistoryRecord> newEntries = new LinkedList <SignalHistoryRecord>();
            while(resultSet.next())
                newEntries.add(new SignalHistoryRecord(StrategyId.values()[resultSet.getInt("StrategyId")], Pair
                    .values()[resultSet.getInt("Pair")], resultSet.getInt("IsBuy") == 1, dateFormat.parse(resultSet.getString(
                    "OpenDate")).getTime(), dateFormat.parse(resultSet.getString("CloseDate")).getTime(), resultSet.getInt("Profit"),
                    resultSet.getDouble("VIX"), resultSet.getDouble("SSI1"), resultSet.getDouble("SSI2"), resultSet
                        .getInt("Low"), resultSet.getInt("High")));
            Collections.sort(newEntries);
            return newEntries;
        }
        catch(Exception e)
        {
            DailyLog.logError("Error haciendo la lectura de la base de datos");
            try
            {
                connection.close();
            }
            catch(Exception e1)
            {
            }
            connection = null;
            return new LinkedList <SignalHistoryRecord>();
        }
        finally
        {
            close(resultSet);
            close(statement);
            returnConnection(connection);
        }
    }
    
    public static void backupDatabase()
    {
    	Connection connection = getConnection();
        Statement statement = null;
        try
        {
            statement = connection.createStatement();
            statement.setQueryTimeout(60);
            statement.execute("BEGIN EXCLUSIVE TRANSACTION");
            Path sourceFile = Paths.get(getDatabaseFilename());
            Path targetFile = Paths.get(getBackupDatabaseFilename(false));
            try
            {
            	Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            	if(!new File(getBackupDatabaseFilename(true)).exists())
            	{
            		targetFile = Paths.get(getBackupDatabaseFilename(true));
                	Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            	}
            }
            finally
            {
            	statement.execute("ROLLBACK TRANSACTION");
            }
        }
        catch(Exception e)
        {
            DailyLog.logError("Error making backup " + e.getMessage());
            try
            {
                connection.close();
            }
            catch(Exception e1)
            {
            }
            connection = null;
        }
        finally
        {
            close(statement);
            returnConnection(connection);
        }
    }
    
    private static String getBackupDatabaseFilename(boolean permanent)
    {
    	String name = "";
    	if(DailyProperties.isTesting())
    		name = "dailybotTesting_backup";
    	else
    		name = "dailybot_backup";
    	if(permanent)
    	{
    		Calendar calendar = Calendar.getInstance();
    		name += calendar.get(Calendar.YEAR) + "" + (calendar.get(Calendar.MONTH) + 1) + "" + calendar.get(Calendar.DATE);
    		name = "backups/" + name;
    	}
    	return name + ".sqlite";
    }
    
    private static String getDatabaseFilename()
    {
    	if(DailyProperties.isAnalysis())
    		return getBackupDatabaseFilename(false);
    	else
    	{
	    	if(DailyProperties.isTesting())
	    		return "dailybotTesting.sqlite";
	    	else
	    		return "dailybot.sqlite";
    	}
    }

    private static Connection getExclusiveConnection()
    {
    	try
    	{
    		Class.forName("org.sqlite.JDBC");
    		Connection connection = DriverManager.getConnection("jdbc:sqlite:" + getDatabaseFilename());
    		return connection;
    	}
    	catch(Exception e)
    	{
    		DailyLog.logError("Error opening connection: " + e);
    		throw new RuntimeException(e);
    	}
    }
    
    private static final AtomicReference <Connection> connectionInstance = new AtomicReference <Connection> ();
    
    private static Connection getConnection()
    {
       if(connectionInstance.get() == null)
       {
    	   synchronized(SqlConnection.class)
    	   {
    		   if(connectionInstance.get() == null)
    		   {
    			   try
    			   {
	    			   Class.forName("org.sqlite.JDBC");
	    			   Connection connection = DriverManager.getConnection("jdbc:sqlite:" + getDatabaseFilename());
	    			   connectionInstance.set(connection);
    			   }
    			   catch(Exception e)
    			   {
    				   DailyLog.logError("Error opening connection: " + e);
    				   throw new RuntimeException(e);
    			   }
    		   }
    	   }
       }
       return connectionInstance.get();
    }

    private static void returnConnection(Connection connection)
    {
    }
}