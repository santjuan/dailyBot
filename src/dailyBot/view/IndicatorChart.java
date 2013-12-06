package dailyBot.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;

import dailyBot.analysis.Indicator;
import dailyBot.analysis.Ranges;
import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.analysis.Ranges.Range;

public class IndicatorChart extends JPanel
{
    private static final long serialVersionUID = 3829875524026705923L;

    private JLabel label;
    private List<SignalHistoryRecord> records;
    private Ranges ranges;
    private InfoPanel info;
    boolean unico;

    public IndicatorChart(Range range, List<SignalHistoryRecord> records, Indicator indicator, Ranges ranges)
    {
        this.records = records;
        label = new JLabel();
        info = new InfoPanel();
        unico = true;
        this.ranges = ranges;
        setLayout(new BorderLayout());
        add(info, BorderLayout.CENTER);
        add(label, BorderLayout.EAST);
        setVisible(true);
        updateChart(range, indicator);
    }

    public void updateChart(Range range, Indicator indicator)
    {
        int numero = (int) ranges.getRange(Indicator.BUY).getMinBuy();
        boolean compra = numero == 1;
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries seriesDentro = new XYSeries("Dentro " + indicator);
        XYSeries seriesFuera = new XYSeries("Fuera " + indicator);
        double acum = 0;
        int nTransacciones = 0;
        for(SignalHistoryRecord r : records)
        {
            if(indicator != Indicator.BUY && numero != 2 && r.buy != compra)
                continue;
            if(unico)
            {
                if(range.isInside(indicator.calculate(r), r.buy)
                        || (numero == 2 && range.isInside(indicator.calculate(r), !r.buy)))
                {
                    nTransacciones++;
                    acum += r.profit;
                    seriesDentro.add(indicator.calculate(r), r.profit);
                }
                else
                    seriesFuera.add(indicator.calculate(r), r.profit);
            }
            else
            {
                if(ranges.fulfills(r, false, ""))
                {
                    nTransacciones++;
                    acum += r.profit;
                    seriesDentro.add(indicator.calculate(r), r.profit);
                }
                else
                    seriesFuera.add(indicator.calculate(r), r.profit);
            }
        }
        double media = acum / nTransacciones;
        double desviacionD = 0;
        for(SignalHistoryRecord r : records)
            if(unico)
            {
                if(range.isInside(indicator.calculate(r), r.buy))
                    desviacionD += (r.profit - media) * (r.profit - media);
            }
            else
            {
                if(ranges.fulfills(r, false, ""))
                    desviacionD += (r.profit - media) * (r.profit - media);
            }
        desviacionD /= nTransacciones;
        desviacionD = Math.sqrt(desviacionD);
        info.profit.setText(acum + "");
        NumberFormat df = DecimalFormat.getNumberInstance();
        df.setMaximumFractionDigits(4);
        info.pipsAverage.setText(df.format(media));
        int porcentaje = (int) (((nTransacciones + 0.0d) / records.size()) * 100);
        String espacios = nTransacciones < 10 ? "    " : nTransacciones < 100 ? "   " : nTransacciones < 1000 ? "  " : " ";
        String espaciosA = espacios;
        espacios += "( " + (porcentaje == 100 ? "" : " ") + porcentaje + "%  )";
        info.transactionNumber.setText(espaciosA + nTransacciones + espacios);
        info.deviation.setText(df.format(desviacionD));
        dataset.addSeries(seriesDentro);
        dataset.addSeries(seriesFuera);
        JFreeChart chart = ChartFactory.createScatterPlot(indicator + " vs Ganancia ", indicator.toString(), "Ganancia",
                dataset, PlotOrientation.VERTICAL, false, false, false);
        XYPlot xyplot = chart.getXYPlot();
        Paint gradientpaint = new Color(0.0f, 0.0f, 0.0f, 1.0f);
        double delta = (indicator.getRange().getMaxBuy() - indicator.getRange().getMinBuy()) / 1000;
        double minimo = compra ? range.getMinBuy() : range.getMinSell();
        double maximo = compra ? range.getMaxBuy() : range.getMaxSell();
        XYBoxAnnotation x = new XYBoxAnnotation(minimo, -100000, minimo + delta, 100000, null, null, gradientpaint);
        xyplot.getRenderer().addAnnotation(x, Layer.BACKGROUND);
        xyplot.getRenderer().setSeriesPaint(0, Color.BLUE);
        x = new XYBoxAnnotation(maximo - delta, -100000, maximo, 100000, null, null, gradientpaint);
        xyplot.getRenderer().addAnnotation(x, Layer.BACKGROUND);
        label.setIcon(new ImageIcon(chart.createBufferedImage(600, 420)));
        this.setVisible(true);
    }
}
