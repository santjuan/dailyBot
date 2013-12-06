package dailyBot.view;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Calendar;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import dailyBot.analysis.SignalHistoryRecord;
import dailyBot.model.Filter;
import dailyBot.model.SignalProvider.SignalProviderId;

public class HistoricChart extends JFrame
{
    private static final long serialVersionUID = 8280180608366756689L;

    private JLabel pipsAverageGraph;
    private JLabel monthsGraph;
    private Filter filter;
    private List<SignalHistoryRecord> records;
    private SignalProviderId signalProviderId;

    public HistoricChart(Filter filter, List<SignalHistoryRecord> records, String title, SignalProviderId signalProviderId)
    {
        super(title);
        this.filter = filter;
        this.records = records;
        this.signalProviderId = signalProviderId;
        initialize();
        setPreferredSize(new Dimension(1237, 626));
        pack();
        setVisible(true);
        updateCharts();
    }

    private void initialize()
    {
        this.setSize(1237, 626);
        monthsGraph = new JLabel();
        monthsGraph.setBounds(new Rectangle(16, 286, 1198, 290));
        pipsAverageGraph = new JLabel();
        pipsAverageGraph.setBounds(new Rectangle(771, 7, 443, 267));
        JPanel jContentPane = new JPanel();
        jContentPane.add(pipsAverageGraph, null);
        jContentPane.add(monthsGraph, null);
        this.setContentPane(jContentPane);
    }

    public JFreeChart getMonthsChart()
    {
        DefaultCategoryDataset cdts = new DefaultCategoryDataset();
        Calendar fecha1 = Calendar.getInstance();
        fecha1.setTimeInMillis(records.isEmpty() ? 0 : records.get(0).openDate);
        Calendar actual = fecha1;
        int acumuladoActual = 0;
        Calendar temp = Calendar.getInstance();
        Calendar twoYears = Calendar.getInstance();
        twoYears.add(Calendar.YEAR, -2);
        int cuentaActual = 0;
        for(SignalHistoryRecord registro : records)
        {
            if(registro.openDate < twoYears.getTimeInMillis())
                continue;
            if(filter.filter(registro, false, "", signalProviderId))
            {
                temp.setTimeInMillis(registro.openDate);
                if(temp.get(Calendar.MONTH) != actual.get(Calendar.MONTH))
                {
                    if(cuentaActual != 0 && actual.get(Calendar.YEAR) >= 2012)
                        cdts.addValue(acumuladoActual, "", monthAsString(actual) + "-" + (actual.get(Calendar.YEAR) - 2000));
                    actual = Calendar.getInstance();
                    actual.setTimeInMillis(temp.getTimeInMillis());
                    acumuladoActual = registro.profit;
                    cuentaActual = 1;
                }
                else
                {
                    acumuladoActual += registro.profit;
                    cuentaActual++;
                }
            }
        }
        cdts.addValue(acumuladoActual, "", monthAsString(actual) + " " + actual.get(Calendar.YEAR));
        return ChartFactory.createBarChart("Ganancia atravez del tiempo", "Tiempo", "Ganancia", cdts,
                PlotOrientation.VERTICAL, false, false, false);
    }

    public JFreeChart getPipsAverageChart()
    {
        XYSeries series = new XYSeries("Serie pips");
        double ganancia = 0;
        int i = 0;
        for(SignalHistoryRecord registro : records)
        {
            if(filter.filter(registro, false, "", signalProviderId))
            {
                ganancia += registro.profit;
                i++;
                series.add(registro.closeDate, ganancia / i);
            }
        }
        XYSeriesCollection xySeriesCollection = new XYSeriesCollection(series);
        return ChartFactory.createXYAreaChart("Promedio Pips vs Tiempo", "Tiempo", "Promedio pips", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false);
    }

    public void updateCharts()
    {
        monthsGraph.setIcon(new ImageIcon(getMonthsChart().createBufferedImage(1198, 290)));
        pipsAverageGraph.setIcon(new ImageIcon(getPipsAverageChart().createBufferedImage(443, 267)));
    }

    public static String monthAsString(Calendar calendar)
    {
        switch(calendar.get(Calendar.MONTH))
        {
        case Calendar.JANUARY:
            return "Ene";
        case Calendar.FEBRUARY:
            return "Feb";
        case Calendar.MARCH:
            return "Mar";
        case Calendar.APRIL:
            return "Abr";
        case Calendar.MAY:
            return "May";
        case Calendar.JUNE:
            return "Jun";
        case Calendar.JULY:
            return "Jul";
        case Calendar.AUGUST:
            return "Ago";
        case Calendar.SEPTEMBER:
            return "Sep";
        case Calendar.OCTOBER:
            return "Oct";
        case Calendar.NOVEMBER:
            return "Nov";
        case Calendar.DECEMBER:
            return "Dic";
        default:
            return "ERROR";
        }
    }
}