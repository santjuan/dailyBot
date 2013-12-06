package dailyBot.view;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class InfoPanel extends JPanel
{
    private static final long serialVersionUID = 3143214002376719420L;

    JLabel deviation;
    JLabel deviationLabel;
    JLabel profit;
    JLabel profitLabel;
    JLabel transactionNumber;
    JLabel transactionNumberLabel;
    JLabel pipsAverage;
    JLabel pipsAverageLabel;

    public InfoPanel()
    {
        profitLabel = new JLabel();
        pipsAverageLabel = new JLabel();
        transactionNumberLabel = new JLabel();
        deviationLabel = new JLabel();
        profit = new JLabel();
        pipsAverage = new JLabel();
        transactionNumber = new JLabel();
        deviation = new JLabel();
        profitLabel.setText("Ganancia:");
        pipsAverageLabel.setText("Promedio pips:");
        transactionNumberLabel.setText("Transacciones:");
        deviationLabel.setText("Desviacion:");
        profit.setText("jLabel1");
        pipsAverage.setText("jLabel2");
        transactionNumber.setText("jLabel3");
        deviation.setText("jLabel4");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(
                                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(deviation).addComponent(deviationLabel)
                                        .addComponent(transactionNumber).addComponent(transactionNumberLabel)
                                        .addComponent(pipsAverage).addComponent(pipsAverageLabel).addComponent(profit)
                                        .addComponent(profitLabel))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addGap(100, 100, 100).addComponent(profitLabel).addGap(12, 12, 12)
                        .addComponent(profit).addGap(18, 18, 18).addComponent(pipsAverageLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(pipsAverage)
                        .addGap(18, 18, 18).addComponent(transactionNumberLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(transactionNumber).addGap(18, 18, 18).addComponent(deviationLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(deviation)));
    }

    public static void main(String[] args)
    {
        JFrame jf = new JFrame();
        jf.add(new InfoPanel());
        jf.pack();
        jf.setVisible(true);
    }
}
