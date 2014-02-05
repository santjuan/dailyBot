package dailyBot.analysis.view;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;

import dailyBot.analysis.Utils;
import dailyBot.model.MultiFilter;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;

public class AnalysisFormat extends JFrame
{
	private static final long serialVersionUID = 7082375872262280863L;

	private static void load(JComboBox <StrategyId> comboBoxStrategy, JComboBox <Pair> comboBoxPair, SignalProviderId id, JCheckBox[] filterCheckBoxes, JCheckBox andCheckBox)
	{
		MultiFilter filter = Utils.getFilterSignalProvider(id.ordinal(), true);
		filter.setActive(true);
		for(StrategyId strategyId : StrategyId.values())
			for(Pair pair : Pair.values())
				filter.changeActive(strategyId, pair, false);
		filter.changeActive((StrategyId) comboBoxStrategy.getSelectedItem(), (Pair) comboBoxPair.getSelectedItem(), true);
		int i = Utils.getFilterSignalProvider(id.ordinal()).getActiveFilters()[((StrategyId) comboBoxStrategy.getSelectedItem()).ordinal()][((Pair) comboBoxPair.getSelectedItem()).ordinal()];
		filter.changeActiveFilter((StrategyId) comboBoxStrategy.getSelectedItem(), (Pair) comboBoxPair.getSelectedItem(), i);
		if((i & 1) == 1)
			andCheckBox.setSelected(false);
		else
			andCheckBox.setSelected(true);
		i >>= 1;
		for(int j = 0; j < filterCheckBoxes.length; j++, i >>= 1)
			if((i & 1) == 1)
				filterCheckBoxes[j].setSelected(true);
			else
				filterCheckBoxes[j].setSelected(false);
	}
	
	private static void save(JComboBox <StrategyId> comboBoxStrategy, JComboBox <Pair> comboBoxPair, SignalProviderId id, JCheckBox[] filterCheckBoxes, JCheckBox andCheckBox)
	{
		MultiFilter filter = Utils.getFilterSignalProvider(id.ordinal(), true);
		filter.setActive(true);
		for(StrategyId strategyId : StrategyId.values())
			for(Pair pair : Pair.values())
				filter.changeActive(strategyId, pair, false);
		filter.changeActive((StrategyId) comboBoxStrategy.getSelectedItem(), (Pair) comboBoxPair.getSelectedItem(), true);
		int i = 0;
		if(!andCheckBox.isSelected())
			i |= 1 << 0;
		for(int j = 0; j < filterCheckBoxes.length; j++)
			if(filterCheckBoxes[j].isSelected())
				i |= 1 << (j + 1);
		Utils.getFilterSignalProvider(id.ordinal()).changeActiveFilter((StrategyId) comboBoxStrategy.getSelectedItem(), (Pair) comboBoxPair.getSelectedItem(), i);
		filter.changeActiveFilter((StrategyId) comboBoxStrategy.getSelectedItem(), (Pair) comboBoxPair.getSelectedItem(), i);
	}
	
	public AnalysisFormat(final SignalProviderId id) 
	{
		new HistoricChart(id.toString() + "", new ProgressChart(), id, true);
		final MultiFilter filter = Utils.getFilterSignalProvider(id.ordinal(), true);
		final JComboBox <StrategyId> comboBoxStrategy = new JComboBox <StrategyId> (StrategyId.values());
		final JComboBox <Pair> comboBoxPair = new JComboBox <Pair> (Pair.values());
		int size = filter.filters().length + 1;
		setLayout(new GridLayout(1, 2 + size));
		add(comboBoxStrategy);
		add(comboBoxPair);
		final JCheckBox[] filterCheckBoxes = new JCheckBox[filter.filters().length];
		for(int i = 0; i < filter.filters().length; i++)
		{
			filterCheckBoxes[i] = new JCheckBox(Utils.getFilterNameSignalProvider(id, i));
			add(filterCheckBoxes[i]);
		}
		final JCheckBox andCheckBox = new JCheckBox("and");
		add(andCheckBox);
		ActionListener listenerSave = new ActionListener() 
		{	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				save(comboBoxStrategy, comboBoxPair, id, filterCheckBoxes, andCheckBox);
			}
		};
		ActionListener listenerLoad = new ActionListener() 
		{	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				load(comboBoxStrategy, comboBoxPair, id, filterCheckBoxes, andCheckBox);
			}
		};
		comboBoxStrategy.addActionListener(listenerLoad);
		comboBoxPair.addActionListener(listenerLoad);
		for(JCheckBox checkBox : filterCheckBoxes)
			checkBox.addActionListener(listenerSave);
		andCheckBox.addActionListener(listenerSave);
		listenerLoad.actionPerformed(null);
		setSize(300, 300);
        pack();
        setVisible(true);
	}
}