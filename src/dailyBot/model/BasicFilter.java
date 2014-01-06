package dailyBot.model;

import java.rmi.RemoteException;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.control.DailyLog;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.view.RMIClientMain;

public class BasicFilter extends Filter
{
    private static final long serialVersionUID = 766214723267275304L;

    SignalProviderId id;

    @Override
    public void startFilter(SignalProviderId id)
    {
        setId(id);
    }

    @Override
    public boolean filter(SignalHistoryRecord record, Object... parameters)
    {
        try
        {
            if(RMIClientMain.connection != null
                && !RMIClientMain.connection.getActiveSignalProvider(id.ordinal(), record.id.ordinal(),
                    record.pair.ordinal()))
                return false;
            if(RMIClientMain.connection == null && !isActive(record.id, record.pair))
                return false;
        }
        catch(RemoteException e)
        {
            DailyLog.logRMI("Error preguntando si el proveedor esta activo " + e.getMessage());
        }
        return true;
    }

    public synchronized SignalProviderId getId()
    {
        return id;
    }

    public synchronized void setId(SignalProviderId id)
    {
        this.id = id;
    }
}