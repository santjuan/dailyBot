package dailyBot.control.connection;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class XMLPersistentObject
{
    private static final Map<String, String> createdTables = new ConcurrentHashMap<String, String>();

    private static String getCreateTableStatement(String tableName)
    {
        return "CREATE TABLE IF NOT EXISTS " + tableName
            + " (Id INTEGER NOT NULL, Xml TEXT NOT NULL, PRIMARY KEY (Id))";
    }

    private static void tryCreateTable(String tableName)
    {
        if(createdTables.get(tableName) == null)
        {
            synchronized(XMLPersistentObject.class)
            {
                if(createdTables.put(tableName, tableName) == null)
                    MySqlConnection.executeSql(getCreateTableStatement(tableName));
            }
        }
    }

    private String getTableName()
    {
        return getClass().getSimpleName();
    }

    protected abstract int objectId();

    protected void writeObject()
    {
        tryCreateTable(getTableName());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLEncoder encoder = new XMLEncoder(baos);
        encoder.writeObject(this);
        encoder.close();
        String xml = new String(baos.toByteArray());
        String insertSql = "INSERT INTO " + getTableName() + "(Id, Xml) VALUES (" + objectId() + ", '" + xml
            + "') ON DUPLICATE KEY UPDATE Xml = '" + xml + "'";
        MySqlConnection.executeSql(insertSql);
    }

    protected static <E extends XMLPersistentObject> E readObject(Class<E> clazz, int id)
    {
        tryCreateTable(clazz.getSimpleName());
        String selectSql = "SELECT Xml FROM " + clazz.getSimpleName() + " WHERE Id = " + id;
        String xml = MySqlConnection.querySql(selectSql);
        if(xml.trim().isEmpty())
            return null;
        else
        {
            char[] xmlChar = xml.toCharArray();
            byte[] xmlByte = new byte[xmlChar.length];
            int i = 0;
            for(char c : xmlChar)
                xmlByte[i++] = (byte) c;
            XMLDecoder decoder = new XMLDecoder(new ByteArrayInputStream(xmlByte));
            @SuppressWarnings("unchecked")
            E answer = (E) (decoder.readObject());
            decoder.close();
            return answer;
        }
    }
}