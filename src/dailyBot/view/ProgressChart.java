package dailyBot.view;

import java.awt.BorderLayout;
import java.rmi.RemoteException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import dailyBot.model.Filter;
import dailyBot.model.SignalProvider.SignalProviderId;


public class ProgressChart extends JPanel
{
	private static final long serialVersionUID = 6174853826093522407L;
	
	private InfoPanel info;
	private JLabel label;
	private List <SignalHistoryRecord> records;
	private Filter filter;
	private SignalProviderId signalProviderId = null;

	public ProgressChart(Filter filter, List <SignalHistoryRecord> records, SignalProviderId signalProviderId)
	{
		this(filter, records);
		this.signalProviderId = signalProviderId;
		updateProgressChart();
	}
	
	public ProgressChart(Filter filter, List <SignalHistoryRecord> records)
	{
		this.filter = filter;
		this.records = records;
        label = new JLabel();
        info = new InfoPanel();
        setLayout(new BorderLayout());
        add(info, BorderLayout.CENTER);
        add(label, BorderLayout.EAST);
		updateProgressChart();
	}

	public void updateProgressChart()
	{
	    XYSeries series = new XYSeries("Serie ganancia");
	    double acum = 0;
	    int nTransacciones = 0;
	    int totalTransacciones = 0;
		long actual = System.currentTimeMillis();
	    for(SignalHistoryRecord r : records)
	    {
	    	try
	    	{
				if(signalProviderId != null && RMIClientMain.connection.getActiveSignalProvider(signalProviderId.ordinal(), r.id.ordinal(), r.pair.ordinal()) && ((actual - r.openDate) <= (12L * 30L * 24L * 60L * 60L * 1000L)))
					totalTransacciones++;
				else
					continue;
					
			} 
	    	catch (RemoteException e)
	    	{
			}
	    	if(filter.filter(r, false, "", signalProviderId))
	    	{
	    		nTransacciones++;
	    		acum += r.profit;
		        series.add(r.openDate, acum);
	    	}
	    }
	    double media = acum / nTransacciones;
	    double desviacionD = 0;
	    for(SignalHistoryRecord r : records)
	    	if(filter.filter(r, false, "", signalProviderId))
	    		desviacionD += (r.profit - media) * (r.profit - media);
	    desviacionD /= nTransacciones;
	    desviacionD = Math.sqrt(desviacionD);
	    info.profit.setText(acum + "");
	    NumberFormat df = DecimalFormat.getNumberInstance();
	    df.setMaximumFractionDigits(4);
	    info.pipsAverage.setText(df.format(media));
	    int porcentaje = (int) (((nTransacciones + 0.0d) / ((signalProviderId == null ? records.size() : totalTransacciones))) * 100);
	    String espacios = nTransacciones < 10 ? "    " : nTransacciones < 100 ? "   " : nTransacciones < 1000 ? "  " : " ";
	    String espaciosA = espacios;
	    espacios += "( " + (porcentaje == 100 ? "" : " ") + porcentaje + "%  )";
	    info.transactionNumber.setText(espaciosA + nTransacciones + " / " + totalTransacciones);
	    info.deviation.setText(df.format(desviacionD));
	    XYSeriesCollection xySeriesCollection = new XYSeriesCollection(series);
	    JFreeChart chart = ChartFactory.createXYAreaChart("Ganancia vs tiempo", "Ganancia", "Tiempo", xySeriesCollection, PlotOrientation.VERTICAL, false, false, false);
	    label.setIcon(new ImageIcon(chart.createBufferedImage(600, 350)));
	}
	
	public List <SignalHistoryRecord> getRecords()
	{
		return records;
	}
}