package dailyBot.analysis.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        int filas = Pair.values().length;
        gridLayout.setRows(filas);
        gridLayout.setColumns(2);
        this.setLayout(gridLayout);
        for(Pair p : Pair.values())
        {
            if(p == Pair.ALL)
                continue;
            this.add(darBoton(p, true));
            this.add(darBoton(p, false));
        }
        this.add(darBotonActivo());
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
            		HistoricChart.currentUpdate.get().run();
                }
            });
        return nuevo;
    }

    private JCheckBox darBoton(Pair p, boolean b)
    {
        JCheckBox nuevo = new JCheckBox();
        nuevo.setText(p.toString() + (b ? "_T" : "_F"));
        nuevo.setSize(new Dimension(30, 30));
        nuevo.setSelected(Utils.getFilterSignalProvider(signalProviderId.ordinal()).hasActive(strategyId, p, b));
        configurar(p, b, nuevo);
        return nuevo;
    }

    private void configurar(final Pair par, final boolean b, JCheckBox box)
    {
        box.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean activar = ((AbstractButton) e.getSource()).isSelected();
                Utils.getFilterSignalProvider(signalProviderId.ordinal()).changeActive(strategyId,
                    par, b, activar);
        		HistoricChart.currentUpdate.get().run();
            }
        });
    }
}
