import javax.swing.*;
import javax.swing.event.CaretListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.Timer;

public class AdvancedNotepad{

    private static int tabCount = 1;
    private static final HashMap<Component, UndoManager> undoManagers = new HashMap<>();
    private static final HashMap<Component, JTextArea> textAreas = new HashMap<>();
    private static boolean darkMode = false;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

            // Dark Theme Configuration
            UIManager.put("control", new Color(25, 25, 25)); // General background
            UIManager.put("info", new Color(25, 25, 25));
            UIManager.put("nimbusBase", new Color(20, 20, 20)); // Base for tabs, buttons, etc.
            UIManager.put("nimbusAlertYellow", new Color(255, 204, 0));
            UIManager.put("nimbusDisabledText", new Color(120, 120, 120));
            UIManager.put("nimbusFocus", new Color(100, 100, 255)); // Blue focus ring (subtle)
            UIManager.put("nimbusGreen", new Color(0, 200, 0));
            UIManager.put("nimbusInfoBlue", new Color(100, 150, 255));
            UIManager.put("nimbusLightBackground", new Color(25, 25, 25)); // Text area background
            UIManager.put("nimbusOrange", new Color(255, 100, 50));
            UIManager.put("nimbusRed", new Color(240, 80, 80));
            UIManager.put("nimbusSelectedText", new Color(255, 255, 255));
            UIManager.put("nimbusSelectionBackground", new Color(60, 60, 60)); // Selection area
            UIManager.put("text", new Color(240, 240, 240)); // Clean white text

            // Menu & Toolbar
            UIManager.put("menuText", new Color(230, 230, 230));
            UIManager.put("menu", new Color(35, 35, 35));
            UIManager.put("MenuBar.background", new Color(35, 35, 35));
            UIManager.put("Menu.selectionBackground", new Color(60, 60, 60));
            UIManager.put("MenuItem.background", new Color(35, 35, 35));
            UIManager.put("MenuItem.selectionBackground", new Color(70, 70, 70));

            // TabbedPane (tabs area)
            UIManager.put("TabbedPane.background", new Color(20, 20, 20));
            UIManager.put("TabbedPane.foreground", new Color(230, 230, 230));

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Smart Notepad");
        frame.setSize(1000, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        ImageIcon image = new ImageIcon("Notepad logo1.png");
        frame.setIconImage(image.getImage());

        JTabbedPane tabbedPane = new JTabbedPane();
        frame.add(tabbedPane);

        // Right-click to rename tab
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex != -1) {
                        String currentTitle = tabbedPane.getTitleAt(tabIndex);
                        String newTitle = JOptionPane.showInputDialog(frame, "Rename Tab:", currentTitle);
                        if (newTitle != null && !newTitle.trim().isEmpty()) {
                            tabbedPane.setTitleAt(tabIndex, newTitle.trim());
                        }
                    }
                }
            }
        });

        JLabel statusBar = new JLabel("Line: 1 Column: 1");
        statusBar.setOpaque(true);
        frame.add(statusBar, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(40, 40, 40));

        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newTabItem = new JMenuItem("New Tab");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");

        newTabItem.addActionListener(e -> addNewTab(tabbedPane));
        openItem.addActionListener(e -> openFile(frame, tabbedPane));
        saveItem.addActionListener(e -> saveFile(frame, tabbedPane));
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(newTabItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        JMenuItem findReplaceItem = new JMenuItem("Find & Replace");

        undoItem.addActionListener(e -> {
            UndoManager um = undoManagers.get(tabbedPane.getSelectedComponent());
            if (um != null && um.canUndo())
                um.undo();
        });
        redoItem.addActionListener(e -> {
            UndoManager um = undoManagers.get(tabbedPane.getSelectedComponent());
            if (um != null && um.canRedo())
                um.redo();
        });
        cutItem.addActionListener(e -> getCurrentTextArea(tabbedPane).cut());
        copyItem.addActionListener(e -> getCurrentTextArea(tabbedPane).copy());
        pasteItem.addActionListener(e -> getCurrentTextArea(tabbedPane).paste());
        findReplaceItem.addActionListener(e -> showFindReplaceDialog(frame, getCurrentTextArea(tabbedPane)));

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(findReplaceItem);

        // Format Menu
        JMenu formatMenu = new JMenu("Format");

        // Font family chooser
        JMenuItem fontItem = new JMenuItem("Change Font");
        fontItem.addActionListener(e -> changeFontFamily(tabbedPane));

        // Font size chooser
        JMenuItem sizeItem = new JMenuItem("Change Font Size");
        sizeItem.addActionListener(e -> changeFontSize(tabbedPane));

        // Font style chooser (Bold/Italic)
        JMenuItem styleItem = new JMenuItem("Change Font Style");
        styleItem.addActionListener(e -> changeFontStyle(tabbedPane));

        // Text color chooser
        JMenuItem colorItem = new JMenuItem("Change Text Color");
        colorItem.addActionListener(e -> changeTextColor(tabbedPane));

        formatMenu.add(fontItem);
        formatMenu.add(sizeItem);
        formatMenu.add(styleItem);
        formatMenu.add(colorItem);
        menuBar.add(formatMenu);

        // View Menu
        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem darkModeToggle = new JCheckBoxMenuItem("Dark Mode");
        darkModeToggle.addItemListener(e -> {
            darkMode = darkModeToggle.isSelected();
            updateTheme(tabbedPane, menuBar, statusBar);
        });
        viewMenu.add(darkModeToggle);

        // Tools Menu
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem wordCountItem = new JMenuItem("Word/Character Count");
        wordCountItem.addActionListener(e -> {
            JTextArea current = getCurrentTextArea(tabbedPane);
            String text = current.getText();
            int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            int chars = text.length();
            JOptionPane.showMessageDialog(frame, "Words: " + words + "\nCharacters: " + chars);
        });
        toolsMenu.add(wordCountItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);

        frame.setJMenuBar(menuBar);
        menuBar.add(formatMenu);

        Timer autoSaveTimer = new Timer();
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    JTextArea current = getCurrentTextArea(tabbedPane);
                    if (current != null && !current.getText().isEmpty()) {
                        File temp = new File("autosave_tab" + tabbedPane.getSelectedIndex() + ".txt");
                        BufferedWriter writer = new BufferedWriter(new FileWriter(temp));
                        current.write(writer);
                        writer.close();
                    }
                } catch (Exception ignored) {
                }
            }
        }, 60000, 60000);

        addNewTab(tabbedPane);
        frame.setVisible(true);
    }

    private static void changeFontFamily(JTabbedPane tabbedPane) {
        JTextArea current = getCurrentTextArea(tabbedPane);
        if (current == null)
            return;

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String chosen = (String) JOptionPane.showInputDialog(null, "Choose Font:", "Font",
                JOptionPane.PLAIN_MESSAGE, null, fonts, current.getFont().getFamily());
        if (chosen != null) {
            Font oldFont = current.getFont();
            current.setFont(new Font(chosen, oldFont.getStyle(), oldFont.getSize()));
        }
    }

    private static void changeFontSize(JTabbedPane tabbedPane) {
        JTextArea current = getCurrentTextArea(tabbedPane);
        if (current == null)
            return;

        String sizeStr = JOptionPane.showInputDialog("Enter Font Size:", current.getFont().getSize());
        if (sizeStr != null && sizeStr.matches("\\d+")) {
            int newSize = Integer.parseInt(sizeStr);
            Font oldFont = current.getFont();
            current.setFont(new Font(oldFont.getFamily(), oldFont.getStyle(), newSize));
        }
    }

    private static void changeFontStyle(JTabbedPane tabbedPane) {
        JTextArea current = getCurrentTextArea(tabbedPane);
        if (current == null)
            return;

        String[] styles = { "Plain", "Bold", "Italic", "Bold Italic" };
        int style = JOptionPane.showOptionDialog(null, "Choose Style:", "Font Style",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, styles, styles[0]);

        if (style >= 0) {
            Font oldFont = current.getFont();
            int styleCode = switch (style) {
                case 0 -> Font.PLAIN;
                case 1 -> Font.BOLD;
                case 2 -> Font.ITALIC;
                case 3 -> Font.BOLD | Font.ITALIC;
                default -> oldFont.getStyle();
            };
            current.setFont(new Font(oldFont.getFamily(), styleCode, oldFont.getSize()));
        }
    }

    private static void changeTextColor(JTabbedPane tabbedPane) {
        JTextArea current = getCurrentTextArea(tabbedPane);
        if (current == null)
            return;

        Color newColor = JColorChooser.showDialog(null, "Choose Text Color", current.getForeground());
        if (newColor != null) {
            current.setForeground(newColor);
        }
    }

    private static void addNewTab(JTabbedPane tabbedPane) {
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 16));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);

        UndoManager undoManager = new UndoManager();
        textArea.getDocument().addUndoableEditListener(undoManager);
        undoManagers.put(scrollPane, undoManager);
        textAreas.put(scrollPane, textArea);

        addClosableTab(tabbedPane, "Untitled " + tabCount++, scrollPane);
    }

    private static JTextArea getCurrentTextArea(JTabbedPane tabbedPane) {
        JScrollPane scrollPane = (JScrollPane) tabbedPane.getSelectedComponent();
        return scrollPane != null ? textAreas.get(scrollPane) : null;
    }

    private static void openFile(JFrame frame, JTabbedPane tabbedPane) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader reader = new BufferedReader(new FileReader(chooser.getSelectedFile()))) {
                JTextArea textArea = new JTextArea();
                textArea.setFont(new Font("Consolas", Font.PLAIN, 16));

                // Important: Wrap settings here too!
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);

                textArea.read(reader, null);

                JScrollPane scrollPane = new JScrollPane(textArea);
                UndoManager undoManager = new UndoManager();
                textArea.getDocument().addUndoableEditListener(undoManager);
                undoManagers.put(scrollPane, undoManager);
                textAreas.put(scrollPane, textArea);

                addClosableTab(tabbedPane, chooser.getSelectedFile().getName(), scrollPane);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error opening file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void saveFile(JFrame frame, JTabbedPane tabbedPane) {
        JTextArea current = getCurrentTextArea(tabbedPane);
        if (current == null)
            return;

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        int result = chooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(chooser.getSelectedFile()))) {
                current.write(writer);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void updateTheme(JTabbedPane tabbedPane, JMenuBar menuBar, JLabel statusBar) {
        Color bg = darkMode ? Color.DARK_GRAY : Color.WHITE;
        Color fg = darkMode ? Color.WHITE : Color.BLACK;

        for (Component comp : textAreas.keySet()) {
            JTextArea area = textAreas.get(comp);
            area.setBackground(bg);
            area.setForeground(fg);
            area.setCaretColor(fg);
        }

        menuBar.setBackground(darkMode ? new Color(40, 40, 40) : UIManager.getColor("Menu.background"));
        for (MenuElement menuElement : menuBar.getSubElements()) {
            JMenu menu = (JMenu) menuElement.getComponent();
            menu.setForeground(fg);
            menu.setBackground(bg);
        }

        statusBar.setBackground(bg);
        statusBar.setForeground(fg);
    }

    private static void addClosableTab(JTabbedPane tabbedPane, String title, JScrollPane scrollPane) {
    tabbedPane.addTab(title, scrollPane);
    int index = tabbedPane.indexOfComponent(scrollPane);

    JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    tabPanel.setOpaque(false);

    JLabel tabTitle = new JLabel(title);
    JButton closeButton = new JButton("x");
    closeButton.setMargin(new Insets(0, 5, 0, 5));
    closeButton.setFocusable(false);
    closeButton.setFont(new Font("Arial", Font.BOLD, 12));
    closeButton.setForeground(Color.RED);
    closeButton.setBorder(null);
    closeButton.setContentAreaFilled(false);

    closeButton.addActionListener(e -> promptToSaveBeforeClose(tabbedPane, scrollPane, title));

    // Add right-click rename support to tabPanel
    tabPanel.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                renameTab(tabbedPane, scrollPane);
            }
        }
    });
    

    tabPanel.add(tabTitle);
    tabPanel.add(closeButton);
    tabbedPane.setTabComponentAt(index, tabPanel);
    tabbedPane.setSelectedComponent(scrollPane);
}
private static void renameTab(JTabbedPane tabbedPane, JScrollPane scrollPane) {
    String newTitle = JOptionPane.showInputDialog("Enter new tab name:");
    if (newTitle != null && !newTitle.trim().isEmpty()) {
        int index = tabbedPane.indexOfComponent(scrollPane);
        if (index != -1) {
            // Update custom tab component's label
            Component tabComponent = tabbedPane.getTabComponentAt(index);
            if (tabComponent instanceof JPanel) {
                for (Component comp : ((JPanel) tabComponent).getComponents()) {
                    if (comp instanceof JLabel) {
                        ((JLabel) comp).setText(newTitle); // ✅ Change visible label
                        break;
                    }
                }
            }
        }
    }
}


    private static void promptToSaveBeforeClose(JTabbedPane tabbedPane, JScrollPane scrollPane, String title) {
        JTextArea area = textAreas.get(scrollPane);
        if (area == null)
            return;

        if (!area.getText().isEmpty()) {
            int choice = JOptionPane.showConfirmDialog(
                    null,
                    "Do you want to save changes to " + title + "?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.CANCEL_OPTION)
                return;

            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
                int result = chooser.showSaveDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(chooser.getSelectedFile()))) {
                        area.write(writer);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "Error saving file", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } else {
                    return; // Cancelled file save dialog
                }
            }
        }

        undoManagers.remove(scrollPane);
        textAreas.remove(scrollPane);
        tabbedPane.remove(scrollPane);
    }

    private static void showFindReplaceDialog(JFrame frame, JTextArea area) {
        JDialog dialog = new JDialog(frame, "Find & Replace", false);
        dialog.setLayout(new GridLayout(3, 2));

        JTextField findField = new JTextField();
        JTextField replaceField = new JTextField();

        JButton findBtn = new JButton("Find");
        JButton replaceBtn = new JButton("Replace All");

        findBtn.addActionListener(e -> {
            String text = area.getText();
            String word = findField.getText();
            int index = text.indexOf(word);
            if (index != -1) {
                area.setSelectionStart(index);
                area.setSelectionEnd(index + word.length());
                area.requestFocus();
            } else {
                JOptionPane.showMessageDialog(frame, "Word not found");
            }
        });

        replaceBtn.addActionListener(e -> {
            String text = area.getText();
            String find = findField.getText();
            String replace = replaceField.getText();
            area.setText(text.replaceAll(find, replace));
        });

        dialog.add(new JLabel("Find:"));
        dialog.add(findField);
        dialog.add(new JLabel("Replace:"));
        dialog.add(replaceField);
        dialog.add(findBtn);
        dialog.add(replaceBtn);

        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }
} 