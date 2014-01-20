package dailyBot.view;

import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.analysis.Utils;
import dailyBot.model.MultiFilter;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;

public class SignalProviderFormat extends JFrame
{
    private static final long serialVersionUID = 2552180741753628128L;

    public static synchronized List<SignalHistoryRecord> getCurrentRecords(SignalProviderId signalProviderId)
    {
    	try
    	{
	    	List<SignalHistoryRecord> all = Utils.getRecords();
	    	MultiFilter filter = RMIClientMain.connection.getFilterSignalProvider(signalProviderId.ordinal());
	    	long actual = System.currentTimeMillis();
	    	ArrayList<SignalHistoryRecord> selected = new ArrayList<SignalHistoryRecord> ();
	    	for(SignalHistoryRecord r : all)
		    	try
		        {
		            if(signalProviderId != null
		                && RMIClientMain.connection.getActiveSignalProvider(signalProviderId.ordinal(), r.id.ordinal(),
		                    r.pair.ordinal()) && ((actual - r.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L))
		                && filter.filter(r, false))
		            	selected.add(r);
		        }
		        catch(RemoteException e)
		        {
		        }
	    	return selected;
    	}
    	catch(Exception e)
    	{
    		throw new RuntimeException(e);
    	}
    }
    
    public static int getCurrentRecordsSize(SignalProviderId signalProviderId)
    {
    	List<SignalHistoryRecord> all = Utils.getRecords();
    	long actual = System.currentTimeMillis();
    	int count = 0;
    	for(SignalHistoryRecord r : all)
	    	try
	        {
	            if(signalProviderId != null
	                && RMIClientMain.connection.getActiveSignalProvider(signalProviderId.ordinal(), r.id.ordinal(),
	                    r.pair.ordinal()) && ((actual - r.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L)))
	            	count++;
	        }
	        catch(RemoteException e)
	        {
	        }
    	return count;
    }
    
    public SignalProviderFormat(SignalProviderId signalProviderId)
    {
        super(signalProviderId.toString());
        ProgressChart graficaProgreso = new ProgressChart();
        graficaProgreso.changeRecords(getCurrentRecords(signalProviderId), getCurrentRecordsSize(signalProviderId));
        HistoricChart graficaHistorial = new HistoricChart(signalProviderId.toString(), graficaProgreso);
        graficaHistorial.changeRecords(getCurrentRecords(signalProviderId));
        JTabbedPane jtp = new JTabbedPane();
        JCheckBox botonActivo = null;
        for(StrategyId id1 : StrategyId.values())
        {
            PairFormat actual = new PairFormat(signalProviderId, id1, graficaProgreso, graficaHistorial, botonActivo);
            if(id1.equals(StrategyId.values()[0]))
                botonActivo = (JCheckBox) actual.darBotonActivo();
            jtp.addTab(id1.toString(), actual);
        }
        jtp.setVisible(true);
        setMinimumSize(new Dimension(259, 244));
        setSize(259, 244);
        add(jtp);
        pack();
        setVisible(true);
    }
}