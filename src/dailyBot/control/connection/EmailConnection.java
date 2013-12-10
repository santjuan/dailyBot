package dailyBot.control.connection;

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
    private static final String[] emailList = { DailyProperties
        .getProperty("dailyBot.control.connection.EmailConnection.emailTo") };
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

    private static class MessageSenderHelper implements Runnable
    {
        String subject;
        String message;

        private MessageSenderHelper(String subject, String message)
        {
            this.subject = subject;
            this.message = message;
        }

        public void run()
        {
            try
            {
                session.setDebug(false);
                Message mimeMessage = new MimeMessage(session);
                InternetAddress addressFrom = new InternetAddress(emailFromAddress);
                mimeMessage.setFrom(addressFrom);
                InternetAddress[] addressTo = new InternetAddress[emailList.length];
                for(int i = 0; i < emailList.length; i++)
                    addressTo[i] = new InternetAddress(emailList[i]);
                mimeMessage.setRecipients(Message.RecipientType.TO, addressTo);
                mimeMessage.setSubject(subject);
                mimeMessage.setContent(message, message.contains("<html>") ? "text/html" : "text/plain");
                Transport.send(mimeMessage);
            }
            catch(MessagingException e)
            {
                DailyLog.logErrorToDisk("Error al enviar el correo " + e.getMessage());
            }
        }
    }

    public static void sendEmail(String subject, String message)
    {
        executor.submit(new MessageSenderHelper(subject, message));
    }
}