package dailyBot.control.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import dailyBot.control.DailyLog;

public class SocketConnection
{
    static class SocketHelper
    {
        private static final Map <String, SocketHelper> instanceMap = new ConcurrentHashMap <String, SocketHelper>();
        private static final ExecutorService executor = Executors.newSingleThreadExecutor();

        private static SocketHelper getInstance(String hostname, int port)
        {
            String key = hostname + " " + port;
            if(!instanceMap.containsKey(key))
                instanceMap.put(key, new SocketHelper(hostname, port));
            return instanceMap.get(key);
        }

        private final String hostname;
        private final int port;
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public SocketHelper(String hostname, int port)
        {
            this.hostname = hostname;
            this.port = port;
        }

        private synchronized void clearSocket()
        {
            try
            {
                if(socket != null)
                    socket.close();
            }
            catch(Exception e)
            {
            }
            socket = null;
        }

        private synchronized void ensureSocketOpen(int number)
        {
            if(number == 10)
                throw new RuntimeException("Conexion por socket imposible despues de 10 intentos: " + hostname + ":"
                    + port);
            if(socket == null || (!socket.isConnected()) || (socket.isClosed()))
            {
                clearSocket();
                try
                {
                    socket = new Socket(hostname, port);
                    socket.setSoTimeout(10000);
                    out = new PrintWriter(socket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                }
                catch(Exception e)
                {
                    clearSocket();
                }
                ensureSocketOpen(number + 1);
            }
            else
            {
                try
                {
                    out.println("test");
                    out.flush();
                    if(in.readLine().equals("oK"))
                        return;
                }
                catch(Exception e)
                {
                }
                clearSocket();
                ensureSocketOpen(number + 1);
            }
        }

        public synchronized String sendAndReceive(String... values) throws IOException
        {
            ensureSocketOpen(0);
            for(String value : values)
            {
                out.println(value);
                out.flush();
            }
            return in.readLine();
        }
    }

    public static String sendAndReceive(String hostname, int port, final String... values)
    {
        final SocketHelper helper = SocketHelper.getInstance(hostname, port);
        Future <String> future = SocketHelper.executor.submit(new Callable <String>()
        {
            @Override
            public String call() throws Exception
            {
                return helper.sendAndReceive(values);
            }
        });
        try
        {
            return future.get(120, TimeUnit.SECONDS);
        }
        catch(Exception e)
        {
            DailyLog.logError("Error en comunicacion por socket " + hostname + ":" + port + " " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}