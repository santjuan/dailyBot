package dailyBot.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import dailyBot.analysis.Indicator;
import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.control.DailyLog;
import dailyBot.model.Filter;
import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.dailyFx.RangeFilter;

public class RangesView extends JFrame
{
	private static final long serialVersionUID = -3243368885519444393L;
	
	public RangesView(final GraphicStrategy father, final Filter[] filters, List <SignalHistoryRecord> records, final StrategyId strategyId, final Pair pair, String title)
	{
		super(title);
		final JFrame este = this;
		JTabbedPane panel = new JTabbedPane();
		for(int i = 0; i < filters.length; i++)
		{
			JComponent actual = new SignalProviderRanges((RangeFilter) filters[i], records, strategyId, pair, title, SignalProviderId.values()[i]);
			panel.addTab(SignalProviderId.values()[i].toString(), actual);
		}
		add(panel);
	    addWindowListener(new WindowAdapter() 
	    {
	    	@Override
	        public void windowClosing(WindowEvent e)
	        {
	    		try
	    		{
	    			if(strategyId != null && JOptionPane.showConfirmDialog(este, "Guardar el rango?", "Guardar " + strategyId + " " + pair, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
	    			{
//	    				for(int i = 0; i < filtros.length; i++)
//	    					padre.cambiarFiltrosProveedor(i, filtros[i]);
	    			}
				}
	            catch (Exception e1)
	            {        	
	            	DailyLog.logRMI(e1.getMessage() + " Error haciendo la conexion RMI");
	            	System.exit(0);
	            }
	        }
	    });
		setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
		pack();
		setVisible(true);
	}
	
	class SignalProviderRanges extends JPanel
	{
		private static final long serialVersionUID = -6260738056337041545L;
		
		private ArrayList <RangeView> rangeViews = new ArrayList <RangeView> ();
		
		public SignalProviderRanges(final RangeFilter filter, List <SignalHistoryRecord> records, final StrategyId strategyId, final Pair pair, String title, SignalProviderId signalProviderId)
		{
			ProgressChart graficaProgreso = new ProgressChart(filter, records);
			final IndicatorChart graficaIndicador = new IndicatorChart(Indicator.VIX.getRange().duplicate(), records, Indicator.VIX, filter.getRanges(pair, strategyId));
			HistoricChart graficaHistorial = new HistoricChart(filter, records, signalProviderId.toString(), signalProviderId);
			JPanel panelRangos = new JPanel();
			GridLayout gridLayout = new GridLayout();
			gridLayout.setRows(6);
			gridLayout.setColumns(1);
			panelRangos.setLayout(gridLayout);
			panelRangos.setSize(350, 600);
			for(Indicator i : Indicator.values())
			{
				RangeView r = new RangeView(this, i.getRange().duplicate(), filter.getRanges(pair, strategyId), graficaProgreso, graficaIndicador, graficaHistorial, i);
				rangeViews.add(r);
				panelRangos.add(r);
			}
			graficaIndicador.unico = false;
			JPanel panelGraficas = new JPanel();
			panelGraficas.setLayout(new BorderLayout());
			panelGraficas.add(graficaProgreso, BorderLayout.SOUTH);
			panelGraficas.add(graficaIndicador, BorderLayout.NORTH);
			setLayout(new BorderLayout());
			add(panelRangos, BorderLayout.WEST);
			add(panelGraficas, BorderLayout.EAST);
		}
	
		public void updateAll()
		{
			for(RangeView r : rangeViews)
				r.actualizar();
		}
	}
}