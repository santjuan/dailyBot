package dailyBot.analysis.view;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JFrame;

public class DailyWindow extends JFrame
{
    private static final long serialVersionUID = -8975030219203093917L;

    public DailyWindow(String name, Dimension size, Object[][] components)
    {
        super(name);
        setSize(size);
        for(Object[] o : components)
            add((Component) o[0], o[1]);
        pack();
        setVisible(true);
    }
}