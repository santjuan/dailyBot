package dailyBot.analysis.view;

import java.awt.BorderLayout;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import dailyBot.analysis.SignalHistoryRecord;

public class ProgressChart extends JPanel
{
    private static final long serialVersionUID = 6174853826093522407L;

    private InfoPanel info;
    private JLabel label;
    private List<SignalHistoryRecord> records = new ArrayList <SignalHistoryRecord>();
    private double originalSize;

    public ProgressChart()
    {
        label = new JLabel();
        info = new InfoPanel();
        setLayout(new BorderLayout());
        add(info, BorderLayout.CENTER);
        add(label, BorderLayout.EAST);
    }

    public void updateProgressChart()
    {
        XYSeries series = new XYSeries("Serie ganancia");
        double acum = 0;
        int nTransacciones = 0;
        int totalTransacciones = (int) originalSize;
        for(SignalHistoryRecord r : records)
        {
        	nTransacciones++;
            acum += r.profit;
            series.add(r.openDate, acum);
        }
        double media = acum / nTransacciones;
        double desviacionD = 0;
        for(SignalHistoryRecord r : records)
        	desviacionD += (r.profit - media) * (r.profit - media);
        desviacionD /= nTransacciones;
        desviacionD = Math.sqrt(desviacionD);
        info.profit.setText(acum + "");
        NumberFormat df = DecimalFormat.getNumberInstance();
        df.setMaximumFractionDigits(4);
        info.pipsAverage.setText(df.format(media));
        int porcentaje = (int) (((nTransacciones + 0.0d) / (originalSize)) * 100);
        String espacios = nTransacciones < 10 ? "    " : nTransacciones < 100 ? "   " : nTransacciones < 1000 ? "  "
            : " ";
        String espaciosA = espacios;
        espacios += "( " + (porcentaje == 100 ? "" : " ") + porcentaje + "%  )";
        info.transactionNumber.setText(espaciosA + nTransacciones + " / " + totalTransacciones);
        info.deviation.setText(df.format(desviacionD));
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYAreaChart("Ganancia vs tiempo", "Ganancia", "Tiempo",
            xySeriesCollection, PlotOrientation.VERTICAL, false, false, false);
        label.setIcon(new ImageIcon(chart.createBufferedImage(600, 350)));
    }

    public void changeRecords(List<SignalHistoryRecord> records, int originalSize)
    {
    	this.records = new ArrayList<SignalHistoryRecord> (records);
    	this.originalSize = originalSize;
    	updateProgressChart();
    }
    
    public List<SignalHistoryRecord> getRecords()
    {
        return records;
    }
}