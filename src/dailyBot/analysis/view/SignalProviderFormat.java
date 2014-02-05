package dailyBot.analysis.view;

import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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

    public static synchronized List<SignalHistoryRecord> getCurrentRecords(SignalProviderId signalProviderId, boolean allActive)
    {
    	try
    	{
	    	List<SignalHistoryRecord> all = Utils.getRecords();
	    	MultiFilter filter = Utils.getFilterSignalProvider(signalProviderId.ordinal(), allActive);
	    	long actual = System.currentTimeMillis();
	    	ArrayList<SignalHistoryRecord> selected = new ArrayList<SignalHistoryRecord> ();
	    	for(SignalHistoryRecord r : all)
	            if(signalProviderId != null
	                && filter.hasActive(r.id, r.pair) 
	                && ((actual - r.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L))
	                && filter.filter(r, false, 0.0))
	            	selected.add(r);
	    	return selected;
    	}
    	catch(Exception e)
    	{
    		throw new RuntimeException(e);
    	}
    }
    
    public static int getCurrentRecordsSize(SignalProviderId signalProviderId, boolean allActive)
    {
    	List<SignalHistoryRecord> all = Utils.getRecords();
    	long actual = System.currentTimeMillis();
    	int count = 0;
    	MultiFilter filter = Utils.getFilterSignalProvider(signalProviderId.ordinal(), allActive);
    	for(SignalHistoryRecord r : all)
    		if(signalProviderId != null
    		&& filter.hasActive(r.id,
    				r.pair) && ((actual - r.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L)))
    			count++;
    	return count;
    }
    
    public SignalProviderFormat(final SignalProviderId signalProviderId)
    {
        super(signalProviderId.toString());
        addWindowListener(new WindowListener() 
        {
			@Override
			public void windowOpened(WindowEvent e)
			{
			}
			
			@Override
			public void windowIconified(WindowEvent e) 
			{
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) 
			{
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) 
			{
			}
			
			@Override
			public void windowClosing(WindowEvent e) 
			{
			}
			
			@Override
			public void windowClosed(WindowEvent e) 
			{
				RMIClientMain.attemptSave(signalProviderId);
			}
			
			@Override
			public void windowActivated(WindowEvent e) 
			{
			}
		});
        boolean allActive = false;
        ProgressChart graficaProgreso = new ProgressChart();
        graficaProgreso.changeRecords(getCurrentRecords(signalProviderId, allActive), getCurrentRecordsSize(signalProviderId, allActive));
        HistoricChart graficaHistorial = new HistoricChart(signalProviderId.toString(), graficaProgreso, signalProviderId, allActive);
        graficaHistorial.changeRecords(getCurrentRecords(signalProviderId, allActive));
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