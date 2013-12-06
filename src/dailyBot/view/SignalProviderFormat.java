package dailyBot.view;

import java.awt.Dimension;
import java.rmi.RemoteException;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import dailyBot.analysis.Utils;
import dailyBot.model.Filter;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;

public class SignalProviderFormat extends JFrame
{
    private static final long serialVersionUID = 2552180741753628128L;

    public SignalProviderFormat(SignalProviderId signalProviderId)
    {
        super(signalProviderId.toString());
        Filter filtro;
        try
        {
            filtro = RMIClientMain.connection.getFilterSignalProvider(signalProviderId.ordinal());
        }
        catch(RemoteException e)
        {
            filtro = null;
        }
        ProgressChart graficaProgreso = new ProgressChart(filtro, Utils.getRecords(), signalProviderId);
        new DailyWindow(signalProviderId.toString(), new Dimension(700, 600), new Object[][] { { graficaProgreso, null } });
        HistoricChart graficaHistorial = new HistoricChart(filtro, Utils.getRecords(), signalProviderId.toString(),
                signalProviderId);
        JTabbedPane jtp = new JTabbedPane();
        JCheckBox botonActivo = null;
        for(StrategyId id1 : StrategyId.values())
        {
            if(id1 != StrategyId.JOEL && id1 != StrategyId.TECHNICAL)
            {
                PairFormat actual = new PairFormat(signalProviderId, id1, graficaProgreso, graficaHistorial, botonActivo);
                if(id1.equals(StrategyId.values()[0]))
                    botonActivo = (JCheckBox) actual.darBotonActivo();
                jtp.addTab(id1.toString(), actual);
            }
        }
        jtp.setVisible(true);
        setMinimumSize(new Dimension(259, 244));
        setSize(259, 244);
        add(jtp);
        pack();
        setVisible(true);
    }
}