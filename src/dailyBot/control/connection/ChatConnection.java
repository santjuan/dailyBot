package dailyBot.control.connection;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import dailyBot.control.DailyThreadAdmin;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyThread;
import dailyBot.control.DailyProperties;
import dailyBot.control.DailyThreadInfo;
import dailyBot.model.Pair;
import dailyBot.model.StrategySignal;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.dailyFx.DailyFXStrategySystem;

public class ChatConnection 
{
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
                    	 String answer = DailyThreadAdmin.checkThreads();
                    	 sendMessage(answer, true);
                    	 any = true;
                     }
                     if(body.contains("chequear t"))
                     {
                    	 Vector <StrategySignal> signals = DailyFXStrategySystem.lastRead.get();
                    	 ArrayList <StrategySignal> all = new ArrayList <StrategySignal> ();
                    	 for(StrategyId strategyId : StrategyId.values())
                    		 all.addAll(strategyId.strategy().duplicateSignals());
                    	 String answer = "";
                    	 answer += "Ultima lectura hace: " + (System.currentTimeMillis() - DailyFXStrategySystem.lastReadTime.get()) + " millisegundos\n";
                    	 if(signals != null)
	                    	 for(StrategySignal inRead : signals)
	                    	 {
	                    		try
	                    		{
		                    		StrategySignal inStrategy = inRead.getStategyId().strategy().hasPair(inRead.getPair());
		                    		answer += "DailyFx : " + inRead + "\n";
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
                    	 String answer = DailyThreadInfo.listThreads(minutes);
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
                    	 String answer = DailyThreadInfo.getLastUpdateInfo(id, current);
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
                    	 if(strategyId != null && signalProviderId != null && pair != null)
                    	 	 closed = signalProviderId.signalProvider().closeSignal(strategyId, pair);
                    	 sendMessage(closed + "", true);
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
                    	 String answer = "Comandos validos: chequear hilos\nchequear todos\nchequear zulutrade\ncerrar IDESTRATEGIA IDPROVEEDOR PAR\nlistar MINUTOS\nconsultar IDUNICO ACTUAL\nmanual open IDPROVEEDOR PAIR ISBUY\nmanual close IDPROVEEDOR IDTRADE";
                    	 sendMessage(answer, true);
                    	 any = true;
                     }
                 }
             }
             catch(Exception e)
             {
             }
         }
     };
     
     private static ConnectionConfiguration connectionConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
     private static final AtomicReference <XMPPConnection> xMPPConnection = new AtomicReference <XMPPConnection >();
     private static final String toAddress = DailyProperties.getProperty("dailyBot.control.connection.ChatConnection.emailTo");
     private static Chat currentChat;
 	 private static final String username = DailyProperties.getProperty("dailyBot.control.connection.ChatConnection.emailFrom");
 	 private static final AtomicLong connectionCount = new AtomicLong();
 	 private static final AtomicLong lastConnection = new AtomicLong();
 	 
     public static void sendMessage(String message, boolean send) 
     {
    	 message += "\nMESSAGE END\n";
         try 
         {
        	 if((System.currentTimeMillis() - lastConnection.get() >= 60L * 60L * 1000L) || xMPPConnection.get() == null || !xMPPConnection.get().isConnected() || ((connectionCount.incrementAndGet() % 120) == 0))
        	 {
        		 lastConnection.set(System.currentTimeMillis());
        		 if(xMPPConnection.get() != null)
        		 {
        			 try
        			 {
	        			 xMPPConnection.get().disconnect();
	        			 DailyThread.sleep(30000);
	        			 xMPPConnection.set(null);
        			 }
        			 catch(Exception e)
        			 {
        			 }
        		 }
	             xMPPConnection.set(new XMPPConnection(connectionConfig));       
	             xMPPConnection.get().connect();
	             xMPPConnection.get().login(username, DailyProperties.getProperty("dailyBot.control.connection.ChatConnection.emailFromPassword"));
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
         catch (Exception e) 
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
         catch (Exception e) 
         {
        	 DailyLog.logErrorToDisk("Error conectandose al chat " + e.getMessage());
         }
     }
}