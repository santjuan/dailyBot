package dailyBot.analysis.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import dailyBot.analysis.Utils;
import dailyBot.model.StrategySignal;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;

public class DailyTable extends JFrame
{
    private static final long serialVersionUID = -1585479366228991191L;

    StrategyId strategyId;
    final static String columnNames[] = { "Estrategia", "Compra", "Par", "Precio de entrada", "Ganancia" };
    JTable table;
    Object[][] toShow;

    public DailyTable(StrategyId strategyId)
    {
        super(strategyId.toString());
        fillShowArray(strategyId);
        createTable();
    }

    public DailyTable(SignalProviderId signalProviderId)
    {
        super(signalProviderId.toString());
        fillShowArray(signalProviderId);
        createTable();
    }

    private void fillShowArray(StrategyId strategyId)
    {
        DecimalFormat df = new DecimalFormat("0.0000");
        List<StrategySignal> listaE = null;
        try
        {
            listaE = Utils.getStrategySignals(strategyId.ordinal());
        }
        catch(Exception e)
        {
            RMIClientMain.logRMI(e.getMessage() + " Error haciendo la conexion RMI");
            System.exit(0);
        }
        toShow = new Object[listaE.size()][5];
        for(int i = 0; i < listaE.size(); i++)
        {
            toShow[i][0] = listaE.get(i).getStrategyId().toString();
            toShow[i][1] = listaE.get(i).isBuy() + " toco: " + listaE.get(i).isStopTouched();
            toShow[i][2] = listaE.get(i).getPair().toString();
            toShow[i][3] = df.format(listaE.get(i).getEntryPrice()) + " " + df.format(listaE.get(i).stopDaily()) + " "
                + df.format(listaE.get(i).getStop());
            try
            {
                toShow[i][4] = "Not available";
            }
            catch(Exception e)
            {
                RMIClientMain.logRMI(e.getMessage() + " Error haciendo la conexion RMI");
                System.exit(0);
            }
        }
    }

    private void fillShowArray(SignalProviderId signalProviderId)
    {
        DecimalFormat df = new DecimalFormat("0.0000");
        List<StrategySignal> listaE = null;
        try
        {
            listaE = Utils.getSignalProviderSignals(signalProviderId.ordinal());
        }
        catch(Exception e)
        {
            RMIClientMain.logRMI(e.getMessage() + " Error haciendo la conexion RMI");
            System.exit(0);
        }
        toShow = new Object[listaE.size()][5];
        for(int i = 0; i < listaE.size(); i++)
        {
            toShow[i][0] = listaE.get(i).getStrategyId().toString();
            try
            {
                StrategySignal esta = Utils.getStrategySignal(listaE.get(i).getStrategyId()
                    .ordinal(), listaE.get(i).getPair().ordinal());
                if(esta == null)
                {
                    toShow[i][1] = "Error: no existe en Estrategia";
                    toShow[i][2] = listaE.get(i).getPair().toString();
                    continue;
                }
                toShow[i][1] = listaE.get(i).isBuy() + " toco: " + esta.isStopTouched();
                toShow[i][2] = listaE.get(i).getPair().toString();
                toShow[i][3] = df.format(esta.getEntryPrice()) + " " + df.format(esta.stopDaily()) + " "
                    + df.format(esta.getStop());
                toShow[i][4] = "Not available"
                    + " "
                    + listaE.get(i).getUniqueId("zulutrade-" + signalProviderId.toString());
            }
            catch(Exception e)
            {
                RMIClientMain.logRMI(e.getMessage() + " Error haciendo la conexion RMI");
                System.exit(0);
            }
        }
    }

    private void createTable()
    {
        table = new JTable(toShow, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(1000, 97));
        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        ListSelectionModel listMod = table.getSelectionModel();
        listMod.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listMod.addListSelectionListener(table);
        table.setModel(new AbstractTableModel()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public int getColumnCount()
            {
                return 5;
            }

            @Override
            public int getRowCount()
            {
                return toShow.length;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex)
            {
                try
                {
                    return toShow[rowIndex][columnIndex];
                }
                catch(Exception e)
                {
                    return "";
                }
            }

            @Override
            public boolean isCellEditable(int a, int b)
            {
                return false;
            }

        });
        pack();
        setVisible(true);
    }
}