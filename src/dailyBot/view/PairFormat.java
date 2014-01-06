package dailyBot.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dailyBot.control.DailyLog;
import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;

public class PairFormat extends JPanel
{
    private static final long serialVersionUID = 1L;
    private SignalProviderId signalProviderId;
    private StrategyId strategyId;
    private ProgressChart graficaProgreso;
    private HistoricChart graficaHistorial;
    private JCheckBox botonActivo;

    public PairFormat(SignalProviderId i, StrategyId ii, ProgressChart gP, HistoricChart gH, JCheckBox bA)
    {
        super();
        signalProviderId = i;
        strategyId = ii;
        graficaProgreso = gP;
        graficaHistorial = gH;
        botonActivo = bA;
        initialize();
    }

    private void initialize()
    {
        GridLayout gridLayout = new GridLayout();
        int filas = (Pair.values().length - 1) % 2 == 0 ? (Pair.values().length - 1) / 2
            : (Pair.values().length - 1) / 2 + 1;
        gridLayout.setRows(filas);
        gridLayout.setColumns(2);
        this.setLayout(gridLayout);
        LinkedList<Pair> paresA = new LinkedList<Pair>();
        LinkedList<Pair> paresB = new LinkedList<Pair>();
        int cuenta = 0;
        boolean enA = false;
        for(Pair p : Pair.values())
        {
            if(p == Pair.ALL)
                continue;
            if(cuenta++ == filas)
                enA = true;
            if(enA)
                paresA.add(p);
            else
                paresB.add(p);
        }
        boolean parA = false;
        this.add(darBotonActivo());
        while(!paresA.isEmpty())
        {
            if(parA)
                this.add(darBoton(paresA.pollFirst()));
            else
                this.add(darBoton(paresB.pollFirst()));
            parA = !parA;
        }
        this.setVisible(true);
    }

    public Component darBotonActivo()
    {
        if(botonActivo != null && strategyId.equals(StrategyId.values()[0]))
            return botonActivo;
        final JCheckBox nuevo = new JCheckBox();
        nuevo.setText("Activo");
        nuevo.setSize(new Dimension(30, 30));
        try
        {
            nuevo.setSelected(RMIClientMain.connection.getActive(signalProviderId.ordinal()));
        }
        catch(RemoteException e)
        {
            DailyLog.logRMI(e.getMessage() + " Error haciendo la conexion RMI");
            System.exit(0);
        }
        botonActivo = botonActivo == null ? nuevo : botonActivo;
        if(botonActivo != nuevo)
            nuevo.setEnabled(false);
        if(botonActivo.getChangeListeners().length == 1)
            botonActivo.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                    try
                    {
                        boolean activar = ((AbstractButton) e.getSource()).isSelected();
                        RMIClientMain.connection.setActive(signalProviderId.ordinal(), activar);
                    }
                    catch(RemoteException e1)
                    {
                        DailyLog.logRMI(e1.getMessage() + " Error haciendo la conexion RMI");
                        System.exit(0);
                    }
                }
            });
        return nuevo;
    }

    private JCheckBox darBoton(Pair p)
    {
        JCheckBox nuevo = new JCheckBox();
        nuevo.setText(p.toString());
        nuevo.setSize(new Dimension(30, 30));
        try
        {
            nuevo.setSelected(RMIClientMain.connection.getActiveSignalProvider(signalProviderId.ordinal(),
                strategyId.ordinal(), p.ordinal()));
        }
        catch(RemoteException e)
        {
            DailyLog.logRMI(e.getMessage() + " Error haciendo la conexion RMI");
            System.exit(0);
        }
        configurar(p, nuevo);
        return nuevo;
    }

    private void configurar(final Pair par, JCheckBox box)
    {
        box.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    boolean activar = ((AbstractButton) e.getSource()).isSelected();
                    RMIClientMain.connection.setActiveSignalProvider(signalProviderId.ordinal(), strategyId.ordinal(),
                        par.ordinal(), activar, false);
                    if(activar
                        && RMIClientMain.connection.getOpenSignalProvider(signalProviderId.ordinal(),
                            strategyId.ordinal(), par.ordinal()))
                    {
                        int a = JOptionPane.showConfirmDialog(null,
                            "La senal estaba abierta, desea abrirla nuevamente?", "Confirmacion",
                            JOptionPane.YES_NO_OPTION);
                        boolean abrir = a == JOptionPane.YES_OPTION;
                        RMIClientMain.connection.setActiveSignalProvider(signalProviderId.ordinal(),
                            strategyId.ordinal(), par.ordinal(), activar, abrir);
                    }
                }
                catch(RemoteException e1)
                {
                    DailyLog.logRMI(e1.getMessage() + " Error haciendo la conexion RMI");
                    System.exit(0);
                }
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        graficaProgreso.updateProgressChart();
                    }
                });
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        graficaHistorial.updateCharts();
                    }
                });
            }
        });
    }
}
