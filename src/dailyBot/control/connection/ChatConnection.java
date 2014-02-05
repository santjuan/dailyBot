package dailyBot.control.connection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyExecutor;
import dailyBot.control.DailyLoopInfo;
import dailyBot.control.DailyUtils;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.StrategySignal;
import dailyBot.model.Utils;
import dailyBot.model.dailyFx.DailyFXStrategySystem;

public class ChatConnection
{
    private static final AtomicLong lastSentHelp = new AtomicLong();

    public static final MessageListener listener = new MessageListener()
    {
        @Override
        public void processMessage(Chat chat, Message message)
        {
            String body = message.getBody();
            try
            {
                boolean any = false;
                if(body != null)
                {
                    if(body.contains("chequear h"))
                    {
                        String answer = DailyExecutor.checkRunnables();
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("chequear t"))
                    {
                        Vector <StrategySignal> signals = DailyFXStrategySystem.lastRead.get();
                        ArrayList <StrategySignal> all = new ArrayList <StrategySignal>();
                        for(StrategyId strategyId : StrategyId.values())
                            all.addAll(strategyId.strategy().duplicateSignals());
                        String answer = "";
                        answer += "Ultima lectura hace: "
                            + (System.currentTimeMillis() - DailyFXStrategySystem.lastReadTime.get())
                            + " millisegundos\n";
                        if(signals != null)
                            for(StrategySignal inRead : signals)
                            {
                                try
                                {
                                    StrategySignal inStrategy = inRead.getStrategyId().strategy()
                                        .hasPair(inRead.getPair());
                                    answer += "DailyFX : " + inRead + "\n";
                                    answer += "DailyBot: " + inStrategy + "\n\n";
                                    all.remove(inStrategy);
                                }
                                catch(Exception e)
                                {
                                    answer += "Error buscando contraparte de: " + inRead + "\n";
                                }
                            }
                        else
                            answer += "Arreglo de lectura nulo\n";
                        for(StrategySignal signal : all)
                            answer += "Senal extra no encontrada en lectura: " + signal + "\n\n";
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("chequear z"))
                    {
                        String answer = "";
                        for(SignalProviderId id : SignalProviderId.values())
                            answer += id + "\n" + id.signalProvider().checkAllBrokers() + "\n\n";
                        for(SignalProviderId signalProviderId : SignalProviderId.values())
                            answer += Utils.checkSignals("zulutrade-" + signalProviderId.toString()) + "\n";
                        sendMessage(answer, true);
                        any = true;
                    }
                    for(StrategyId id : StrategyId.values())
                    {
                        String command = "chequear " + id;
                        if(body.contains(command))
                        {
                            String answer = Utils.checkSignals(id);
                            sendMessage(answer, true);
                            any = true;
                        }
                    }
                    if(body.contains("chequear ALL"))
                    {
                        String answer = Utils.checkSignals((StrategyId) null);
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("listar"))
                    {
                        String bodyThis = body.substring(body.indexOf("listar"));
                        String[] split = bodyThis.split(" ");
                        long minutes = 300;
                        try
                        {
                            minutes = Long.parseLong(split[1]);
                        }
                        catch(Exception e)
                        {
                        }
                        String answer = DailyLoopInfo.listLoops(minutes);
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("verbose"))
                    {
                        String bodyThis = body.substring(body.indexOf("verbose"));
                        String[] split = bodyThis.split(" ");
                        boolean verbose = false;
                        try
                        {
                            verbose = !split[1].equals("0");
                        }
                        catch(Exception e)
                        {
                        }
                        String answer = verbose + "";
                        DailyProperties.setVerbose(verbose);
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("filtro"))
                    {
                        String bodyThis = body.substring(body.indexOf("filtro"));
                        String[] split = bodyThis.split(" ");
                        boolean ok = true;
                        String messageB = "";
                        if(split.length >= 5)
                        {
                           try
                           {
                        	   SignalProviderId signalProviderId = null;
                        	   StrategyId strategy = null;
                        	   Pair pair = null;
                        	   String answer = split[4];
                        	   while(answer.length() > 1 && answer.charAt(0) == '0')
                        		   answer = answer.substring(1);
                        	   int newValue = Integer.parseInt(answer, 2);
                        	   for(SignalProviderId id : SignalProviderId.values())
                        		   if(id.toString().equals(split[1]))
                        			   signalProviderId = id;
                        	   for(StrategyId id : StrategyId.values())
                        		   if(id.toString().equals(split[2]))
                        			   strategy = id;
                        	   for(Pair id : Pair.values())
                        		   if(id.toString().equals(split[3]) && (id != Pair.ALL))
                        			   pair = id;
                        	   SignalProviderId[] providers = new SignalProviderId[0];
                        	   if(signalProviderId != null)
                        		   providers = new SignalProviderId[]{ signalProviderId };
                        	   else if(split[1].equals("ALL"))
                        		   providers = SignalProviderId.values();
                        	   StrategyId[] strategies = new StrategyId[0];
                        	   if(strategy != null)
                        		   strategies = new StrategyId[]{ strategy };
                        	   else if(split[2].equals("ALL"))
                        		   strategies = StrategyId.values();
                        	   Pair[] pairs = new Pair[0];
                        	   if(pair != null)
                        		   pairs = new Pair[]{ pair };
                        	   else if(split[3].equals("ALL"))
                        		   pairs = Pair.values();
                        	   ok = false;
                        	   messageB += Arrays.toString(providers) + " " + Arrays.toString(strategies) + " " + Arrays.toString(pairs);
                        	   for(SignalProviderId a : providers)
                        		   for(StrategyId b : strategies)
                        			   for(Pair c : pairs)
                        			   {
                        				   if(c == Pair.ALL)
                        					   continue;
                        				   ok = true;
                        				   a.signalProvider().changeActiveFilter(b, c, newValue);
                        			   }
                           }
                           catch(Exception e)
                           {
                        	   messageB += "error " + e + " " + e.getMessage();
                        	   ok = false;
                           }
                        }
                        else
                        {
                        	ok = false;
                        	messageB += "numero de argumentos invalido";
                        }
                        String answer = ok + " " + messageB;
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("consultar"))
                    {
                        String bodyThis = body.substring(body.indexOf("consultar"));
                        String[] split = bodyThis.split(" ");
                        long id = 0;
                        boolean current = false;
                        try
                        {
                            id = Long.parseLong(split[1]);
                            current = Long.parseLong(split[2]) != 0;
                        }
                        catch(Exception e)
                        {
                        }
                        String answer = DailyLoopInfo.getLastUpdateInfo(id, current);
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("cerrar"))
                    {
                        String bodyThis = body.substring(body.indexOf("cerrar"));
                        String[] split = bodyThis.split(" ");
                        StrategyId strategyId = null;
                        SignalProviderId signalProviderId = null;
                        Pair pair = null;
                        if(split.length >= 4)
                        {
                            for(StrategyId possibleId : StrategyId.values())
                                if(possibleId.toString().equals(split[1]))
                                    strategyId = possibleId;
                            for(SignalProviderId possibleId : SignalProviderId.values())
                                if(possibleId.toString().equals(split[2]))
                                    signalProviderId = possibleId;
                            for(Pair possiblePair : Pair.values())
                                if(possiblePair.toString().equals(split[3]))
                                    pair = possiblePair;
                        }
                        boolean closed = false;
                        boolean ok = true;
                        if(strategyId != null && signalProviderId != null && pair != null)
                            closed = signalProviderId.signalProvider().closeSignal(strategyId, pair);
                        else
                            ok = false;
                        String answer = closed + " " + ok;
                        if(!ok)
                            answer += " " + Arrays.toString(split);
                        sendMessage(answer, true);
                        any = true;
                    }
                    if(body.contains("manual"))
                    {
                        String bodyThis = body.substring(body.indexOf("manual"));
                        String[] split = bodyThis.split(" ");
                        if(split.length >= 5 && split[1].equals("open"))
                        {
                            String providerString = split[2];
                            String pairString = split[3];
                            String isBuy = split[4];
                            Pair pair = Pair.stringToPair(pairString);
                            boolean opened = false;
                            if(pair != null && (isBuy.equals("0") || isBuy.equals("1")))
                                for(SignalProviderId possibleId : SignalProviderId.values())
                                    if(possibleId.toString().equals(providerString))
                                    {
                                        opened = possibleId.signalProvider().openManualSignal(pair, isBuy.equals("1"));
                                        any = true;
                                    }
                            sendMessage(opened + "", true);
                        }
                        else if(split.length >= 4 && split[1].equals("close"))
                        {
                            String providerString = split[2];
                            String stringId = split[3];
                            long id = 0;
                            try
                            {
                                id = Long.parseLong(stringId);
                                any = true;
                            }
                            catch(Exception e)
                            {
                            }
                            boolean closed = false;
                            if(any)
                            {
                                any = false;
                                for(SignalProviderId possibleId : SignalProviderId.values())
                                    if(possibleId.toString().equals(providerString))
                                    {
                                        closed = possibleId.signalProvider().closeManualSignal(id);
                                        any = true;
                                    }
                            }
                            sendMessage(closed + "", true);
                        }
                    }
                    if(!any)
                    {
                        if(((System.currentTimeMillis() - lastSentHelp.get()) >= 60000L) || body.contains("help")
                            || body.contains("ayuda"))
                        {
                            String answer = "Comandos validos: chequear hilos\nchequear todos\nchequear zulutrade\nchequear IDESTRATEGIA\ncerrar IDESTRATEGIA IDPROVEEDOR PAR\nlistar MINUTOS\nfiltro PROVEEDOR ESTRATEGIA PAR VALOR\nconsultar IDUNICO ACTUAL\nmanual open IDPROVEEDOR PAIR ISBUY\nmanual close IDPROVEEDOR IDTRADE\nverbose VALOR";
                            sendMessage("Recibido: " + body + "\n" + answer, true);
                            lastSentHelp.set(System.currentTimeMillis());
                            any = true;
                        }
                    }
                }
            }
            catch(Exception e)
            {
            }
        }
    };

    private static ConnectionConfiguration connectionConfig = new ConnectionConfiguration("talk.google.com", 5222,
        "gmail.com");
    private static final AtomicReference <XMPPConnection> xMPPConnection = new AtomicReference <XMPPConnection>();
    private static final String toAddress = DailyProperties
        .getProperty("dailyBot.control.connection.ChatConnection.emailTo");
    private static Chat currentChat;
    private static final String username = DailyProperties.isTesting() ? DailyProperties
        .getProperty("dailyBot.control.connection.ChatConnection.emailFrom_testing") : DailyProperties
        .getProperty("dailyBot.control.connection.ChatConnection.emailFrom");
    private static final AtomicLong connectionCount = new AtomicLong();
    private static final AtomicLong lastConnection = new AtomicLong();

    public static synchronized void sendMessage(String message, boolean send)
    {
        if(DailyProperties.isTesting())
            message = "TESTING\n" + message;
        message += "\nMESSAGE END\n";
        try
        {
            if((System.currentTimeMillis() - lastConnection.get() >= 60L * 60L * 1000L) || xMPPConnection.get() == null
                || !xMPPConnection.get().isConnected() || ((connectionCount.incrementAndGet() % 120) == 0))
            {
                lastConnection.set(System.currentTimeMillis());
                if(xMPPConnection.get() != null)
                {
                    try
                    {
                        xMPPConnection.get().disconnect();
                        DailyUtils.sleep(30000);
                        xMPPConnection.set(null);
                    }
                    catch(Exception e)
                    {
                    }
                }
                xMPPConnection.set(new XMPPConnection(connectionConfig));
                xMPPConnection.get().connect();
                xMPPConnection.get().login(username,
                    DailyProperties.getProperty("dailyBot.control.connection.ChatConnection.emailFromPassword"));
                xMPPConnection.get().getChatManager().addChatListener(new ChatManagerListener()
                {
                    @Override
                    public void chatCreated(Chat chat, boolean ignored)
                    {
                        chat.addMessageListener(listener);
                    }
                });
            }
        }
        catch(Exception e)
        {
            DailyLog.logErrorToDisk("Error conectandose al chat " + e.getMessage());
        }
        try
        {
            currentChat = xMPPConnection.get().getChatManager().createChat(toAddress, listener);
            if(message.length() > 30000)
                message = message.substring(0, 30000) + "\ncortado";
            if(send)
                currentChat.sendMessage(message);
        }
        catch(Exception e)
        {
            DailyLog.logErrorToDisk("Error conectandose al chat " + e.getMessage());
        }
    }
}