package dailyBot.view;

import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import dailyBot.analysis.Utils;
import dailyBot.control.DailyLog;
import dailyBot.model.Filter;
import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.dailyFx.RangeFilter;

public class GraphicStrategy extends JFrame
{
	private static final long serialVersionUID = 7878714258759106938L;

	StrategyId strategyId;
	boolean online;
	
	public GraphicStrategy(StrategyId strategyId, boolean online) 
	{	
		super(strategyId.toString());
		this.strategyId = strategyId;
		this.online = online;
		initialize();
	}
	
	private void initialize()
	{
		GridLayout gridLayout = new GridLayout(0, 2);
		this.setLayout(gridLayout);
		this.setSize(259, 490);
		for(Pair par : Pair.values())
			if(par != Pair.ALL)
				this.add(getPairButton(par));
		this.add(getAllPairButton());
		this.add(getCurrentButton());
		pack();
		setVisible(true);
	}

	private JButton getPairButton(final Pair pair) 
	{
		JButton botonNuevo = new JButton();
		botonNuevo.setText(pair.toString());
		botonNuevo.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				try
				{
					Filter[] filtros = new Filter[SignalProviderId.values().length];
					if(online)
						for(int i = 0; i < filtros.length; i++)
							filtros[i] = RMIClientMain.connection.getFilterSignalProvider(i);
					else
						for(int i = 0; i < filtros.length; i++)
							filtros[i] = new RangeFilter();
					new RangesView(GraphicStrategy.this, filtros, Utils.getStrategyRecords(strategyId, pair), strategyId, pair, strategyId + " " + pair);
				}
				catch(Exception e1)
				{
		        	DailyLog.logRMI(e1.getMessage() + " Error haciendo la conexion RMI");
		        	System.exit(0);
				}
			}
		});
		return botonNuevo;
	}
	
	private JButton getAllPairButton() 
	{
		JButton botonNuevo = new JButton();
		botonNuevo.setText("TODOS");
		botonNuevo.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				try
				{
					Filter[] filtros = new Filter[SignalProviderId.values().length];
						for(int i = 0; i < filtros.length; i++)
							filtros[i] = new RangeFilter();
					new RangesView(GraphicStrategy.this, filtros, Utils.getStrategyRecords(strategyId, Pair.ALL), null, null, strategyId + " " + Pair.ALL);
				}
				catch(Exception e1)
				{
		        	DailyLog.logRMI(e1.getMessage() + " Error haciendo la conexion RMI");
		        	System.exit(0);
				}
			}
		});
		return botonNuevo;
	}
	
	private JButton getCurrentButton() 
	{
		JButton botonNuevo = new JButton();
		botonNuevo.setText("ACTUAL");
		botonNuevo.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				try
				{ 
					JFrame frame = new JFrame(strategyId.toString());
					frame.add(new ProgressChart(new RangeFilter(), Utils.getStrategyRecords(strategyId, Pair.ALL)));
					frame.setSize(700, 600);
					frame.pack();
					frame.setVisible(true);
					new HistoricChart(new RangeFilter(), Utils.getStrategyRecords(strategyId, Pair.ALL), strategyId.toString(), null);
				}
				catch(Exception e1)
				{
		        	DailyLog.logRMI(e1.getMessage() + " Error haciendo la conexion RMI");
		        	System.exit(0);
				}
			}
		});
		return botonNuevo;
	}
	
	public static void main(String[] args)
	{
		new GraphicStrategy(StrategyId.values()[((StrategyId) JOptionPane.showInputDialog(null, "Escoja la estrategia", "Analisis grafico", JOptionPane.QUESTION_MESSAGE, null, StrategyId.values(), StrategyId.BREAKOUT1)).ordinal()], false);
	}
}
