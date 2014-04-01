package dailyBot.control.connection;

import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;

public class EmailConnection
{
    private static final String SMTP_HOST_NAME = "gmail-smtp.l.google.com";
    private static final String username = DailyProperties
        .getProperty("dailyBot.control.connection.EmailConnection.emailFrom");
    private static final String emailFromAddress = DailyProperties
        .getProperty("dailyBot.control.connection.EmailConnection.emailFrom");
    private static final String[] emailListWatchers = DailyProperties
        .getProperty("dailyBot.control.connection.EmailConnection.emailToWatchers").split(",");
    private static final String[] emailListAdmins = DailyProperties
            .getProperty("dailyBot.control.connection.EmailConnection.emailToAdmins").split(",");
    private static final String[] emailListSuperadmins = DailyProperties
            .getProperty("dailyBot.control.connection.EmailConnection.emailToSuperadmins").split(",");
    private static final Session session = loadSession();
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);

    private static Session loadSession()
    {
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", SMTP_HOST_NAME);
        properties.put("mail.smtp.auth", "true");
        Session session = Session.getDefaultInstance(properties, new Authenticator()
        {
            @Override
            public PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(username, DailyProperties
                    .getProperty("dailyBot.control.connection.EmailConnection.emailFromPassword"));
            }
        });
        return session;
    }
    
    public static final int WATCHERS = 1;
    public static final int ADMINS = 2;
    public static final int SUPERADMINS = 4;

    private static class MessageSenderHelper implements Runnable
    {
        String subject;
        String message;
        int mask;

        private MessageSenderHelper(String subject, String message, int mask)
        {
            this.subject = subject;
            this.message = message;
            this.mask = mask;
        }

		public void run()
        {
            try
            {
                session.setDebug(false);
                Message mimeMessage = new MimeMessage(session);
                InternetAddress addressFrom = new InternetAddress(emailFromAddress);
                mimeMessage.setFrom(addressFrom);
                ArrayList <String> emailsToSend = new ArrayList <String> ();
                if((mask & WATCHERS) != 0)
                	for(String email : emailListWatchers)
                		emailsToSend.add(email);
                if((mask & ADMINS) != 0)
                	for(String email : emailListAdmins)
                		emailsToSend.add(email);
                if((mask & SUPERADMINS) != 0)
                	for(String email : emailListSuperadmins)
                		emailsToSend.add(email);
                InternetAddress[] addressTo = new InternetAddress[emailsToSend.size()];
                for(int i = 0; i < addressTo.length; i++)
                    addressTo[i] = new InternetAddress(emailsToSend.get(i));
                mimeMessage.setRecipients(Message.RecipientType.TO, addressTo);
                mimeMessage.setSubject(subject);
                mimeMessage.setContent(message, message.contains("<html>") ? "text/html" : "text/plain");
                Transport.send(mimeMessage);
            }
            catch(MessagingException e)
            {
                DailyLog.logErrorToDisk("Error al enviar el correo " + e.getMessage() + ". Titulo: " + subject + ", contenido: " + message);
            }
        }
    }
    
    public static void sendEmail(String subject, String message, int mask1)
    {
        if(DailyProperties.isTesting())
            message = "TESTING\n" + message;
        executor.submit(new MessageSenderHelper(subject, message, mask1));
    }
}