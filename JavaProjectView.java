package javaprojectview;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javaprojectview.parser.ClassInfo;
import javaprojectview.parser.JavaParser;
import javaprojectview.uml.JavaClassDiagram;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;

public class JavaProjectView extends JFrame {
    
    private static final String PROGRAM_TITLE = "Java Project View";
    
    private final FileNameExtensionFilter pngImageFilenameFilter;
    private final JFileChooser fileChooser;
    
    private JavaClassDiagram classDiagram;
    
    public JavaProjectView() {
        pngImageFilenameFilter = new FileNameExtensionFilter("PNG Image", "png");
        fileChooser = new JFileChooser();
        initComponents();
        chooseFilesActionPerformed();
    }
    
    private void initComponents() {
        Settings settings = Settings.getInstance();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem chooseFiles = new JMenuItem("Choose files...");
        chooseFiles.addActionListener((ActionEvent e) -> {
            chooseFilesActionPerformed();
        });
        chooseFiles.setMnemonic('C');
        chooseFiles.setAccelerator(KeyStroke.getKeyStroke('C', KeyEvent.CTRL_DOWN_MASK));
        JMenuItem exportPng = new JMenuItem("Export PNG...");
        exportPng.addActionListener((ActionEvent e) -> {
            exportPngActionPerformed();
        });
        exportPng.setMnemonic('E');
        exportPng.setAccelerator(KeyStroke.getKeyStroke('E', KeyEvent.CTRL_DOWN_MASK));
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener((ActionEvent e) -> {
            exitActionPerformed();
        });
        fileMenu.add(chooseFiles);
        fileMenu.add(exportPng);
        fileMenu.add(exit);
        JMenu helpMenu = new JMenu("Help");
        JMenuItem showUsage = new JMenuItem("Show usage help");
        showUsage.addActionListener((ActionEvent e) -> {
            showUsageActionPerformed();
        });
        showUsage.setMnemonic('U');
        helpMenu.add(showUsage);
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setJMenuBar(menuBar);
        setSize(640, 480);
        setTitle(PROGRAM_TITLE);
    }
    
    private void chooseFilesActionPerformed() {
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setFileFilter(null);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            JavaParser parser = new JavaParser();
            try {
                for (File file : fileChooser.getSelectedFiles()) {
                    // Parse all the selected files.
                    parser.parseFile(file);
                }
            } catch (IOException ex) {
                Logger.getLogger(JavaProjectView.class.getName()).log(Level.SEVERE, null, ex);
            }
            // Remove the old diagram.
            if (classDiagram != null) {
                remove(classDiagram);
            }
            ClassInfo[] classes = parser.getClasses();
            classDiagram = new JavaClassDiagram(classes);
            classDiagram.autoSort();
            add(classDiagram);
            Dimension size = getSize();
            pack();
            setSize(size);
        }
    }
    
    private void exportPngActionPerformed() {
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(pngImageFilenameFilter);
        if (classDiagram == null) {
            showErrorMessage("Cannot export without class diagram.");
        } else if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File outputFile = fileChooser.getSelectedFile();
                if (!outputFile.getName().toLowerCase().endsWith(".png")) {
                    outputFile = new File(outputFile.getPath() + ".png");
                }
                if (outputFile.exists()) {
                    if (JOptionPane.showConfirmDialog(
                            this,
                            "A file with that name already exists. Do you want to overwrite it?",
                            PROGRAM_TITLE + " - Overwrite?",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                if (classDiagram.exportPng(outputFile)) {
                    showMessage("The diagram has been successfully exported as a PNG image.\nNote that the font size in the exported image depends on the current font size (zoom in to increase).", "Export Successful");
                } else {
                    showErrorMessage("Cannot export empty diagram.");
                }
            } catch (IOException ex) {
                showErrorMessage("An error occured while saving the image.");
            }
        }
    }
    
    private void exitActionPerformed() {
        System.exit(0);
    }
    
    private void showUsageActionPerformed() {
        showMessage("Use 'File > Choose files...' to select a directory containing .java files.\n\n"
                  + "Click and drag inside the window to view different parts of the diagram.\n"
                  + "Use the mouse wheel to zoom in or out.\n\n"
                  + "Hold shift while dragging with your mouse on a panel to move it.\n"
                  + "Use 'File > Export PNG...' to export the entire diagram as an image.\n\n"
                  + "Tip 1: Zoom out and drag panels around to sort them to your liking.\n"
                  + "Tip 2: Zoom in before using 'Export PNG' for a higher resolution!\n", "Usage Help");
    }
    
    // Show info message.
    private void showMessage(String message, String title) {
        JOptionPane.showMessageDialog(this, message, PROGRAM_TITLE + " - " + title, JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Show error message.
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this, message, PROGRAM_TITLE + " - Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            Logger.getLogger(JavaProjectView.class.getName()).log(Level.SEVERE, null, ex);
        }
        EventQueue.invokeLater(() -> {
            JavaProjectView window = new JavaProjectView();
            window.setVisible(true);
        });
    }
}
