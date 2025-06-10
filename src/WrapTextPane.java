import javax.swing.*;
import javax.swing.text.*;

public class WrapTextPane extends JTextPane {
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getUI().getPreferredSize(this).width <= getParent().getSize().width;
    }
}
