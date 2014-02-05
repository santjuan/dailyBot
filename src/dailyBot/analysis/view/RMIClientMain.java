package dailyBot.analysis.view;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import dailyBot.analysis.Utils;
import dailyBot.control.DailyProperties;
import dailyBot.model.SignalProvider.SignalProviderId;

public class RMIClientMain extends JFrame
{
    private static final long serialVersionUID = 7878714258759106938L;

    public RMIClientMain()
    {
        super("DailyBot");
        initialize();
    }
    
    public static void attemptSave(SignalProviderId only)
    {
    	for(SignalProviderId id : (only == null ? SignalProviderId.values() : new SignalProviderId[]{only}))
    		if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, "Desea guardar " + id + "?"))
    			Utils.getFilterSignalProvider(id.ordinal()).writePersistence();
    }
    
    private void initialize()
    {
        GridLayout gridLayout = new GridLayout();
        gridLayout.setRows(5);
        gridLayout.setColumns(2);
        setLayout(gridLayout);
        setSize(259, 290);
        JButton activo = new JButton("Cambiar activo");
        activo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
            	new AnalysisFormat((SignalProviderId) JOptionPane.showInputDialog(null, "Seleccione el proveedor", "Proveedor", JOptionPane.QUESTION_MESSAGE, null, SignalProviderId.values(), SignalProviderId.values()[0]));
            }
        });
        add(activo);
        for(SignalProviderId id : SignalProviderId.values())
            this.add(getSignalProviderButton(id));
        JButton salir = new JButton();
        salir.setText("Salir");
        salir.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
            	attemptSave(null);
            	System.exit(0);
            }
        });
        add(salir);
        setSize(new Dimension(259, 244));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private JButton getSignalProviderButton(final SignalProviderId signalProviderId)
    {
        JButton botonNuevo = new JButton();
        botonNuevo.setText(signalProviderId.toString());
        botonNuevo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                new DailyTable(signalProviderId);
                new SignalProviderFormat(signalProviderId);
            }
        });
        return botonNuevo;
    }

    public static void logRMI(String error)
	{
	    JOptionPane.showMessageDialog(null, error);
	}

    public static void main(String[] args)
    {
		DailyProperties.setAnalysis(true);
        new RMIClientMain();
    }
}