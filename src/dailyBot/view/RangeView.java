package dailyBot.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dailyBot.analysis.Indicator;
import dailyBot.analysis.Ranges;
import dailyBot.analysis.Ranges.Range;
import dailyBot.view.RangesView.SignalProviderRanges;

public class RangeView extends JPanel
{
    private static final long serialVersionUID = -7783062359910099807L;

    private Ranges ranges;
    private JCheckBox inverted;
    private Indicator indicator;
    private JSlider maximum;
    private JSlider minimum;
    private JLabel name;

    private Hashtable<Integer, JLabel> getJLabels(Hashtable<Integer, Object> map)
    {
        Hashtable<Integer, JLabel> answer = new Hashtable<Integer, JLabel>();
        for(int i : map.keySet())
            answer.put(i, new JLabel(String.valueOf(map.get(i))));
        return answer;
    }

    public RangeView(final SignalProviderRanges rangesView, Range original, final Ranges ranges,
        final ProgressChart progressChart, final IndicatorChart indicatorChart, final HistoricChart historicChart,
        final Indicator indicator)
    {
        this.ranges = ranges;
        this.indicator = indicator;
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints;
        name = new JLabel(indicator.toString());
        name.setFont(new java.awt.Font("DejaVu Sans", 0, 18));
        inverted = new javax.swing.JCheckBox();
        inverted.setText("invertido");
        minimum = new JSlider((int) original.getMin(ranges), (int) original.getMax(ranges));
        Range rango = ranges.getRange(indicator);
        if(indicator == Indicator.BUY)
            minimum.setValue(2);
        else
            minimum.setValue((int) rango.getMin(ranges));
        minimum.setPreferredSize(new Dimension(400, 39));
        minimum.setMinorTickSpacing(Math.min(1, indicator.getSpaced()));
        minimum.setMajorTickSpacing(indicator.getSpaced());
        minimum.setPaintTicks(true);
        minimum.setSnapToTicks(true);
        minimum.setPaintLabels(true);
        if(indicator.hasLabels())
            minimum.setLabelTable(getJLabels(indicator.getLabels()));
        else
            minimum.setLabelTable(minimum.createStandardLabels(Math.max(indicator.getSpaced(), 4)));
        minimum.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                int compra = (int) ranges.getRange(Indicator.BUY).getMinBuy();
                final Range rango = ranges.getRange(indicator);
                if(indicator != Indicator.BUY && compra == 2)
                    minimum.setValue((int) rango.getMin(ranges));
                if(indicator != Indicator.BUY && minimum.getValue() > rango.getMax(ranges))
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            minimum.setValue((int) rango.getMax(ranges));
                            rango.setMin(minimum.getValue(), ranges);
                        }
                    });
                }
                else
                {
                    if(indicator == Indicator.BUY)
                    {
                        if(minimum.getValue() != ranges.getRange(Indicator.BUY).getMinBuy())
                        {
                            ranges.getRange(Indicator.BUY).setMinBuy(minimum.getValue());
                            rangesView.updateAll();
                        }
                    }
                    else
                        rango.setMin(minimum.getValue(), ranges);
                }
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        progressChart.updateProgressChart();
                    }
                });
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        indicatorChart.updateChart(rango, indicator);
                    }
                });
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        historicChart.updateCharts();
                    }
                });
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        add(minimum, gridBagConstraints);
        add(name, new GridBagConstraints());
        maximum = new JSlider((int) original.getMinBuy(), (int) original.getMaxBuy());
        maximum.setValue((int) rango.getMax(ranges));
        maximum.setPreferredSize(new Dimension(400, 39));
        maximum.setMinorTickSpacing(Math.min(1, indicator.getSpaced()));
        maximum.setMajorTickSpacing(indicator.getSpaced());
        maximum.setPaintTicks(true);
        maximum.setSnapToTicks(true);
        maximum.setPaintLabels(true);
        if(indicator.hasLabels())
            maximum.setLabelTable(getJLabels(indicator.getLabels()));
        else
            maximum.setLabelTable(maximum.createStandardLabels(Math.max(indicator.getSpaced(), 4)));
        maximum.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                int compra = (int) ranges.getRange(Indicator.BUY).getMinBuy();
                final Range rango = ranges.getRange(indicator);
                if(indicator != Indicator.BUY && compra == 2)
                    maximum.setValue((int) rango.getMax(ranges));
                if(indicator != Indicator.BUY && maximum.getValue() < rango.getMin(ranges))
                {
                    SwingUtilities.invokeLater(new Runnable()
                    {
                        public void run()
                        {
                            maximum.setValue((int) rango.getMin(ranges));
                            rango.setMax(maximum.getValue(), ranges);
                        }
                    });
                }
                else
                    rango.setMax(maximum.getValue(), ranges);
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        progressChart.updateProgressChart();
                    }
                });
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        indicatorChart.updateChart(rango, indicator);
                    }
                });
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        historicChart.updateCharts();
                    }
                });
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        if(indicator != Indicator.BUY)
            add(maximum, gridBagConstraints);
        inverted.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final Range rango = ranges.getRange(indicator);
                int compra = (int) ranges.getRange(Indicator.BUY).getMinBuy();
                if(compra == 0)
                    rango.setInvertedSell(inverted.isSelected());
                if(compra == 1)
                    rango.setInvertedBuy(inverted.isSelected());
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        progressChart.updateProgressChart();
                        indicatorChart.updateChart(rango, indicator);
                    }
                });
            }
        });
        inverted.setText("invertido");
        int compra = (int) ranges.getRange(Indicator.BUY).getMinBuy();
        inverted.setSelected(compra != 1 ? rango.isInvertedSell() : rango.isInvertedBuy());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        add(inverted, gridBagConstraints);
    }

    public void actualizar()
    {
        if(indicator != Indicator.BUY)
        {
            Range rango = ranges.getRange(indicator);
            minimum.setValue((int) rango.getMin(ranges));
            maximum.setValue((int) rango.getMax(ranges));
            inverted.setSelected(rango.isInverted(ranges));
        }
    }
}