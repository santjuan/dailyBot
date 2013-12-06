package dailyBot.control.connection.dailyFx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;

import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyThread;
import dailyBot.control.DailyThreadInfo;
import dailyBot.control.connection.BasicConnection;
import dailyBot.model.Pair;

public class DailyFxServerConnection extends BasicConnection
{
    private static abstract class HttpAnswer implements Callable<String>
    {
        protected DefaultHttpClient httpClient;
        protected HttpGet getRequest;
        protected ExecutorService executor;
        protected StringBuilder sb;
        protected byte[] readBuffer;
        protected ReentrantLock lock = new ReentrantLock();

        @Override
        public String call() throws Exception
        {
            return readAnswer(httpClient.execute(getRequest));
        }

        public String readAnswer(HttpResponse answer) throws IllegalStateException, IOException
        {
            sb.setLength(0);
            HttpEntity httpEntity = answer.getEntity();
            answer.getStatusLine();
            if(httpEntity != null)
            {
                InputStream inputStream = httpEntity.getContent();
                int readCount;
                while((readCount = inputStream.read(readBuffer)) != -1)
                {
                    for(int i = 0; i < readCount; i++)
                        sb.append((char) readBuffer[i]);
                }
            }
            return sb.toString();
        }
    }

    private static class DailyFxHelper extends HttpAnswer
    {
        private static DailyFxHelper instance = new DailyFxHelper();

        private DailyFxHelper()
        {
            httpClient = new DefaultHttpClient();
            BasicClientCookie cookie0 = new BasicClientCookie("JSESSIONID", "292E82337F956A043C63CB80051101BF");
            BasicClientCookie cookie1 = new BasicClientCookie(" s_cc", "true");
            BasicClientCookie cookie2 = new BasicClientCookie("s_PVnumber", "4");
            BasicClientCookie cookie3 = new BasicClientCookie("s_sq", "%5B%5BB%5D%5D");
            BasicClientCookie cookie4 = new BasicClientCookie("JSESSIONIDSSO", "0E6DACB1E886BC4A0DD46EB443DAF7D9");
            cookie0.setVersion(0);
            cookie1.setVersion(0);
            cookie2.setVersion(0);
            cookie3.setVersion(0);
            cookie4.setVersion(0);
            cookie0.setDomain("plus.dailyfx.com");
            cookie1.setDomain("plus.dailyfx.com");
            cookie2.setDomain("plus.dailyfx.com");
            cookie3.setDomain("plus.dailyfx.com");
            cookie4.setDomain("plus.dailyfx.com");
            cookie0.setPath("/fxsignals");
            cookie1.setPath("/");
            cookie2.setPath("/");
            cookie3.setPath("/");
            cookie4.setPath("/");
            httpClient.getCookieStore().addCookie(cookie0);
            httpClient.getCookieStore().addCookie(cookie1);
            httpClient.getCookieStore().addCookie(cookie2);
            httpClient.getCookieStore().addCookie(cookie3);
            httpClient.getCookieStore().addCookie(cookie4);
            getRequest = new HttpGet("https://fxsignals.dailyfx.com/fxsignals-ds/json/all.do");
            executor = Executors.newSingleThreadExecutor();
            sb = new StringBuilder("");
            readBuffer = new byte[32768];
        }
    }

    public static String[] readDailyFxServer()
    {
        int j = 0;
        while(j++ <= 30)
        {
            DailyFxHelper.instance.lock.lock();
            try
            {
                Future<String> future = DailyFxHelper.instance.executor.submit(DailyFxHelper.instance);
                String answer;
                try
                {
                    answer = future.get(10, TimeUnit.SECONDS);
                }
                catch(Exception e)
                {
                    if(j == 31)
                    {
                        DailyLog
                            .logError("Error en lectura interna servidor DailyFX, reiniciando despues de 10 minutos");
                        DailyLog.tryReboot();
                    }
                    if(j % 10 == 0)
                    {
                        DailyFxHelper.instance.lock.unlock();
                        DailyFxHelper.instance = new DailyFxHelper();
                        DailyFxHelper.instance.lock.lock();
                        DailyThread.sleep(10000);
                    }
                    DailyThread.sleep(5000);
                    continue;
                }
                String[] result = new String[1];
                result[0] = answer;
                return result;
            }
            catch(Exception e)
            {
                DailyThread.sleep(10000);
            }
            finally
            {
                DailyFxHelper.instance.lock.unlock();
            }
        }
        return null;
    }

    private static class VIXLoadHelper extends HttpAnswer
    {
        private static final VIXLoadHelper instance = new VIXLoadHelper();
        private final AtomicLong VIX = new AtomicLong(Double.doubleToLongBits(20.0));

        private VIXLoadHelper()
        {
            httpClient = new DefaultHttpClient();
            getRequest = new HttpGet("http://finance.yahoo.com/q?s=%5EVIX");
            BasicHttpParams params = new BasicHttpParams();
            params.setParameter(CookieSpecPNames.DATE_PATTERNS,
                Arrays.asList("EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss z"));
            getRequest.setParams(params);
            executor = Executors.newSingleThreadExecutor();
            sb = new StringBuilder("");
            readBuffer = new byte[2048];
        }
    }

    public static void loadVix()
    {
        DailyThreadInfo.registerUpdate("VIX updater", "VIX loading state", "loading VIX");
        String error = "";
        for(int j = 0; j < 100; j++)
        {
            try
            {
                Future<String> future = VIXLoadHelper.instance.executor.submit(VIXLoadHelper.instance);
                String output = future.get(100, TimeUnit.SECONDS);
                DailyThreadInfo.registerUpdate("VIX updater", "VIX loading state", "loaded page " + output);
                Pattern pattern = Pattern.compile("yfs_l10_");
                Pattern pattern2 = Pattern.compile("\\d+.\\d+<");
                Matcher matcher = pattern.matcher(output);
                if(matcher.find())
                    output = output.substring(matcher.end());
                Matcher matcher2 = pattern2.matcher(output);
                if(matcher2.find())
                {
                    String temp = matcher2.group();
                    temp = temp.substring(0, temp.length() - 1);
                    DailyThreadInfo.registerUpdate("VIX updater", "VIX loaded value",
                        "loaded " + Double.parseDouble(temp));
                    VIXLoadHelper.instance.VIX.getAndSet(Double.doubleToLongBits(Double.parseDouble(temp)));
                    return;
                }
            }
            catch(Exception e)
            {
                error += " " + e.toString() + " " + e.getMessage();
                DailyThreadInfo.registerUpdate("VIX updater", "VIX loading state",
                    "error loading VIX " + e + " " + e.getMessage());
                DailyThread.sleep(6000);
            }
        }
        DailyLog.logError(error + " Imposible leer el VIX");
    }

    public static double getVIX()
    {
        return Double.longBitsToDouble(VIXLoadHelper.instance.VIX.getAndAdd(0));
    }

    private static class SSILoadHelper extends HttpAnswer
    {
        private static SSILoadHelper instance = new SSILoadHelper();
        private String cacheSSI;

        private SSILoadHelper()
        {
            httpClient = new DefaultHttpClient();
            executor = Executors.newSingleThreadExecutor();
            sb = new StringBuilder("");
            readBuffer = new byte[2048];
        }

        private void login()
        {
            for(int i = 0; i < 100; i++)
            {
                try
                {
                    // TODO improve security
                    String login = "j_password="
                        + DailyProperties
                            .getProperty("dailyBot.control.connection.dailyFx.DailyFxServerConnection.dailyfxPassword")
                        + "&j_username="
                        + DailyProperties
                            .getProperty("dailyBot.control.connection.dailyFx.DailyFxServerConnection.dailyfxUsername")
                        + "&submit=Sign";
                    getRequest = new HttpGet("https://plus.dailyfx.com/login/loginForm.jsp");
                    Future<String> future = executor.submit(this);
                    future.get(60, TimeUnit.SECONDS);
                    getRequest.abort();
                    getRequest = new HttpGet("https://plus.dailyfx.com/login/j_security_check?" + login);
                    future = executor.submit(this);
                    future.get(60, TimeUnit.SECONDS);
                    getRequest.abort();
                    return;
                }
                catch(Exception e)
                {
                    DailyThread.sleep(30000);
                }
            }
            DailyLog.logError("Error logeando a DailyFX despues de 100 intentos, reiniciando");
            DailyLog.tryReboot();
        }

        private String url(String contents)
        {
            Pattern pattern = Pattern.compile("href='.*\\.html'");
            Matcher matcher = pattern.matcher(contents);
            matcher.find();
            String found = matcher.group();
            return found.substring(6, found.length() - 1);
        }

        public String readPage(String url)
        {
            try
            {
                getRequest = new HttpGet(url);
                Future<String> future = executor.submit(this);
                String output = future.get(120, TimeUnit.SECONDS);
                getRequest.abort();
                return output;
            }
            catch(Exception e)
            {
                DailyLog.logError(e.getMessage() + " Error al leer SSI en lectura de la pagina");
                return null;
            }
        }

        private String readSSI(String pageContent)
        {
            String result = "";
            try
            {
                String[] pairs = { "EURUSD", "GBPUSD", "GBPJPY", "USDJPY", "USDCHF", "USDCAD", "AUDUSD", "NZDUSD" };
                for(int i = 0; i < pairs.length; i++)
                {
                    Pattern pattern = Pattern.compile(pairs[i] + " stands at -?\\d+.\\d+");
                    Matcher matcher = pattern.matcher(pageContent);
                    if(matcher.find())
                    {
                        Pair current = Pair.stringToPair(pairs[i]);
                        double SSI = Double.parseDouble(matcher.group().substring(17));
                        if(SSI > 0)
                        {
                            SSI -= 1;
                            if(Math.abs(SSI) < 0.02)
                                SSI = 0.01;
                        }
                        else
                        {
                            SSI += 1;
                            if(Math.abs(SSI) < 0.02)
                                SSI = -0.01;
                        }
                        current.changeSSI(SSI);
                        result += current + " -> " + SSI + "\n";
                    }
                }
            }
            catch(Exception e)
            {
                DailyLog.logError(e.getMessage() + " Error al leer SSI en el parse");
            }
            return result;
        }
    }

    public static boolean loadSSI()
    {
        String error = "";
        DailyThreadInfo.registerUpdate("SSI updater", "SSI loading state", "starting load");
        for(int i = 0; i < 100; i++)
        {
            SSILoadHelper.instance.lock.lock();
            try
            {
                SSILoadHelper.instance.login();
                String page = SSILoadHelper.instance.readPage("https://plus.dailyfx.com/fxcmideas/intraday-list.do");
                String address = "https://plus.dailyfx.com/fxcmideas/" + SSILoadHelper.instance.url(page);
                if(address.equals(SSILoadHelper.instance.cacheSSI))
                {
                    DailyThreadInfo.registerUpdate("SSI updater", "SSI loading state",
                        "no new page, current page already in cache, not loading");
                    return false;
                }
                String page2 = SSILoadHelper.instance.readPage(address);
                DailyThreadInfo.registerUpdate("SSI updater", "SSI loading state", "loaded SSI page " + page2);
                String result = SSILoadHelper.instance.readSSI(page2);
                DailyThreadInfo.registerThreadLoop("SSI last load");
                DailyThreadInfo.registerUpdate("SSI last load", "Loaded page", "loaded SSI page " + page2);
                DailyThreadInfo.registerUpdate("SSI last load", "Loaded info", "loaded SSI info " + result);
                DailyThreadInfo.closeThreadLoop("SSI last load");
                SSILoadHelper.instance.cacheSSI = address;
                try
                {
                    DailyThreadInfo.registerThreadLoop("VIX updater");
                    loadVix();
                    DailyThreadInfo.closeThreadLoop("VIX updater");
                }
                catch(Exception e)
                {
                }
                DailyLog.logInfo("SSI cargado:\n" + result + "\nVix actual: " + getVIX());
                return true;
            }
            catch(Exception e)
            {
                DailyThreadInfo.registerUpdate("SSI updater", "SSI loading state",
                    "error loading SSI " + e + " " + e.getMessage());
                error += " " + e.getMessage();
            }
            finally
            {
                SSILoadHelper.instance.lock.unlock();
            }
        }
        DailyLog.logError(error + " Error al leer SSI en cargarSSI");
        return true;
    }
}