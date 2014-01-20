package dailyBot.view;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import dailyBot.control.DailyBotMain;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.connection.RMIConnection;
import dailyBot.control.connection.RMIServerConnection;
import dailyBot.model.Pair;
import dailyBot.model.SignalProvider.SignalProviderId;
import dailyBot.model.Strategy.StrategyId;

public class RMIClientMain extends JFrame
{
    private static final long serialVersionUID = 7878714258759106938L;

    public static RMIConnection connection;

    public RMIClientMain()
    {
        super("DailyBot");
        initialize();
    }

    private static Object[] addAllOption(Object[] values)
    {
    	Object[] answer = new Object[values.length + 1];
    	answer[answer.length - 1] = "ALL";
    	for(int i = 0; i < values.length; i++)
    		answer[i] = values[i];
    	return answer;
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
                Object o = JOptionPane.showInputDialog(RMIClientMain.this, "Seleccione el proveedor", "Proveedor", JOptionPane.PLAIN_MESSAGE, null, addAllOption(SignalProviderId.values()), addAllOption(SignalProviderId.values())[0]);
                if((o != null))
                {
                	SignalProviderId[] proveedores = new SignalProviderId[0];
                	if(o instanceof SignalProviderId)
                		proveedores = new SignalProviderId[]{ (SignalProviderId) o };
                	else
                		proveedores = SignalProviderId.values();
                	o = JOptionPane.showInputDialog(RMIClientMain.this, "Seleccione la estrategia", "Estrategia", JOptionPane.PLAIN_MESSAGE, null, addAllOption(StrategyId.values()), addAllOption(StrategyId.values())[0]);	
                	if(o != null)
                	{
                		StrategyId[] estrategias = new StrategyId[0];
                		if(o instanceof StrategyId)
                			estrategias = new StrategyId[]{ (StrategyId) o };
                		else
                			estrategias = StrategyId.values();
                		while(true)
                		{
                			o = JOptionPane.showInputDialog(RMIClientMain.this, "Seleccione el par", "Par", JOptionPane.PLAIN_MESSAGE, null, addAllOption(Pair.values()), addAllOption(Pair.values())[0]);	
                    		if(o == null)
                    			return;
                			Pair[] pares = new Pair[0];
                			if(o instanceof Pair)
                				pares = new Pair[]{ (Pair) o };
                			else
                				pares = Pair.values();
            				try
            				{
								connection.setActiveSignalProvider(proveedores[0].ordinal(), estrategias[0].ordinal(), pares[0].ordinal(), true, false);
							} 
            				catch (RemoteException e2)
            				{
							}
                			while(true)
                			{
                				JOptionPane optionPane = new JOptionPane("Ingrese el nuevo valor", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new String[]{"111", "110", "100", "010"}, "111");
                				JDialog dialog = optionPane.createDialog("Valor");
                				dialog.setVisible(true);
                				dialog.dispose();
                				String answer = (String) optionPane.getValue();
                				if(answer == null)
                				{
                					try 
                					{
										connection.setActiveSignalProvider(proveedores[0].ordinal(), estrategias[0].ordinal(), pares[0].ordinal(), false, false);
									}
                					catch (RemoteException e1)
                					{
									}
	                				break;
                				}
	                			try
	                			{
	                				while(answer.length() > 1 && answer.charAt(0) == '0')
	                					answer = answer.substring(1);
	                				int value = Integer.parseInt(answer, 2);
	                				for(SignalProviderId proveedor : proveedores)
	                					for(StrategyId estrategia : estrategias)
	                						for(Pair par : pares)
	                							if(par != Pair.ALL)
	                								connection.setActiveFilter(proveedor.ordinal(), estrategia.ordinal(), par.ordinal(), value);
	                			}
	                			catch(Exception e1)
	                			{
	                				try 
                					{
										connection.setActiveSignalProvider(proveedores[0].ordinal(), estrategias[0].ordinal(), pares[0].ordinal(), false, false);
									}
                					catch (RemoteException e2)
                					{
									}
	                				break;
	                			}
                			}
                		}
                	}   
                }
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

    public static boolean server = true;

    public static void start(boolean server1)
    {
        server = server1;
        if(server)
            System.setSecurityManager(new SecurityManager());
        try
        {
            String name = "Conexion"
                + ((JOptionPane.showConfirmDialog(null, "Desea conectarse al servicio de prueba") == JOptionPane.YES_OPTION) ? "_testing"
                    : "");
            Registry registry = LocateRegistry.getRegistry(DailyProperties
                .getProperty("dailyBot.view.RMIClientMain.RMIAddress"));
            connection = new RMIServerConnection.Local((RMIConnection) registry.lookup(name));
        }
        catch(Exception e)
        {
            if(server)
            {
                DailyLog.logError(e.getMessage() + " Error haciendo la conexion RMI");
                DailyLog.tryInmediateReboot();
            }
            else
            {
                DailyLog.logRMI(e.getMessage() + " Error haciendo la conexion RMI");
                System.exit(0);
            }
        }
        new RMIClientMain();
    }

    public static void main(String[] args)
    {
        if("true".equals(DailyProperties.getProperty("dailyBot.control.DailyBotMain.RMI")))
        {
            DailyBotMain.loadProperties();
            start(false);
        }
        else
            DailyLog.logRMI("RMI no esta activo en el servidor, abortando");
    }
}