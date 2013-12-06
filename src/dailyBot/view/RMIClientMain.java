package dailyBot.view;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import dailyBot.control.DailyBotMain;
import dailyBot.control.DailyLog;
import dailyBot.control.DailyProperties;
import dailyBot.control.connection.RMIConnection;
import dailyBot.control.connection.RMIServerConnection;
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

    private void initialize()
    {
        GridLayout gridLayout = new GridLayout();
        gridLayout.setRows(5);
        gridLayout.setColumns(2);
        setLayout(gridLayout);
        setSize(259, 290);
        for(StrategyId id : StrategyId.values())
            if(id != StrategyId.JOEL && id != StrategyId.TECHNICAL)
                this.add(getStrategyButton(id));
        for(SignalProviderId id : SignalProviderId.values())
            this.add(getSignalProviderButton(id));
        JButton salir = new JButton();
        salir.setText("Salir");
        salir.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                DailyBotMain.exit();
            }
        });
        add(salir);
        setSize(new Dimension(259, 244));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private JButton getStrategyButton(final StrategyId strategyId)
    {
        JButton botonNuevo = new JButton();
        botonNuevo.setText(strategyId.toString());
        botonNuevo.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                new DailyTable(strategyId);
                new GraphicStrategy(strategyId, true);
            }
        });
        return botonNuevo;
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
            String name = "Conexion";
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