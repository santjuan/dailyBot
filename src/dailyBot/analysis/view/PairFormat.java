package dailyBot.analysis.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dailyBot.analysis.Utils;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;

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
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
            	graficaProgreso.changeRecords(SignalProviderFormat.getCurrentRecords(signalProviderId, false), SignalProviderFormat.getCurrentRecordsSize(signalProviderId, false));
            }
        });
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
            	graficaHistorial.changeRecords(SignalProviderFormat.getCurrentRecords(signalProviderId, false));
            }
        });
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
        nuevo.setSelected(Utils.getFilterSignalProvider(signalProviderId.ordinal()).isActive());
        botonActivo = botonActivo == null ? nuevo : botonActivo;
        if(botonActivo != nuevo)
            nuevo.setEnabled(false);
        if(botonActivo.getChangeListeners().length == 1)
            botonActivo.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                	boolean activar = ((AbstractButton) e.getSource()).isSelected();
                	Utils.getFilterSignalProvider(signalProviderId.ordinal()).setActive(activar);
                }
            });
        return nuevo;
    }

    private JCheckBox darBoton(Pair p)
    {
        JCheckBox nuevo = new JCheckBox();
        nuevo.setText(p.toString());
        nuevo.setSize(new Dimension(30, 30));
        nuevo.setSelected(Utils.getFilterSignalProvider(signalProviderId.ordinal()).hasActive(strategyId, p));
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
                boolean activar = ((AbstractButton) e.getSource()).isSelected();
                Utils.getFilterSignalProvider(signalProviderId.ordinal()).changeActive(strategyId,
                    par, activar);
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                    	graficaProgreso.changeRecords(SignalProviderFormat.getCurrentRecords(signalProviderId, false), SignalProviderFormat.getCurrentRecordsSize(signalProviderId, false));
                    }
                });
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                    	graficaHistorial.changeRecords(SignalProviderFormat.getCurrentRecords(signalProviderId, false));
                    }
                });
            }
        });
    }
}
