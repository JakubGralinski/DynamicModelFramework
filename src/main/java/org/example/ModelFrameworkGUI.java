package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelFrameworkGUI extends JFrame {
    private Controller controller;
    private JTable resultsTable;
    private JLabel statusLabel;
    private JComboBox<String> modelSelector;
    private String dataFilePath = null;
    private JPanel plotPanel;

    // All cols
    private static final String[] TABLE_COLUMNS = {
            "Year", "twKI", "twKS", "twINW", "twEKS", "twIMP",
            "KI", "KS", "INW", "EKS", "IMP", "PKB", "NET_EXPORTS", "SHOCK_FACTOR", "ZDEKS"
    };

    public ModelFrameworkGUI() {
        controller = new Controller();

        setTitle("Model Framework GUI");
        setSize(1600, 1200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = createTopPanel();
        JPanel centerPanel = createCenterPanel();
        JPanel bottomPanel = createBottomPanel();

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadFileButton = new JButton("Load Data");
        loadFileButton.addActionListener(new LoadFileAction());
        panel.add(loadFileButton);

        // Dropdown to select the model
        modelSelector = new JComboBox<>(new String[]{"Model1", "Model2", "Model3"});
        panel.add(modelSelector);

        JButton runModelButton = new JButton("Run Model");
        runModelButton.addActionListener(new RunModelAction());
        panel.add(runModelButton);

        JButton executeScriptButton = new JButton("Execute Script");
        executeScriptButton.addActionListener(new ExecuteScriptAction());
        panel.add(executeScriptButton);

        JButton adhocScriptButton = new JButton("Create and Run Ad-hoc Script");
        adhocScriptButton.addActionListener(new CreateAdhocScriptAction());
        panel.add(adhocScriptButton);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1));

        // Results table
        resultsTable = new JTable(new DefaultTableModel(TABLE_COLUMNS, 0));
        JScrollPane tableScrollPane = new JScrollPane(resultsTable);
        panel.add(tableScrollPane);

        // Plot panel
        plotPanel = new JPanel();
        plotPanel.setLayout(new GridLayout(1, 3, 10, 10)); // Displays 3 plots side by side
        panel.add(plotPanel);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Status: Ready");
        panel.add(statusLabel, BorderLayout.WEST);
        return panel;
    }

    private class LoadFileAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(ModelFrameworkGUI.this);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                dataFilePath = selectedFile.getAbsolutePath();

                try {
                    controller.readDataFrom(dataFilePath);
                    statusLabel.setText("Status: Data loaded from " + dataFilePath);

                    // Load the initial data into the table
                    Map<String, Object> data = controller.readJson(dataFilePath);
                    setTable(data, false);
                } catch (Exception ex) {
                    statusLabel.setText("Status: Failed to load data.");
                    ex.printStackTrace();
                }
            }
        }
    }

    private class RunModelAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (dataFilePath == null || dataFilePath.isEmpty()) {
                statusLabel.setText("Status: No file selected!");
                return;
            }

            String selectedModel = (String) modelSelector.getSelectedItem();
            try {
                controller.runModel(dataFilePath, "src/main/resources/results.json", selectedModel);
                Map<String, Object> results = controller.readJson("src/main/resources/results.json");
                setTable(results, false); // Do not show ZDEKS yet
                controller.getResultsAsTsv(results, "src/main/resources/results.tsv");
                statusLabel.setText("Status: " + selectedModel + " executed successfully.");
            } catch (Exception ex) {
                statusLabel.setText("Status: Failed to run " + selectedModel + ".");
                ex.printStackTrace();
            }
        }
    }

    private class ExecuteScriptAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File("src/main/resources"));
            int returnValue = fileChooser.showOpenDialog(ModelFrameworkGUI.this);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                dataFilePath = selectedFile.getAbsolutePath();
                try {
                    String notebookPath = dataFilePath;
                    String inputPath = "src/main/resources/intermediate.json";
                    String outputPath = "src/main/resources/results.json";

                    // Run the Jupyter Notebook using nbconvert
                    controller.runNotebook(notebookPath, inputPath, outputPath);

                    // Results after notebook execution
                    Map<String, Object> results = controller.readJson(outputPath);

                    // set the table
                    setTable(results, true);

                    controller.getResultsAsTsv(results, "src/main/resources/results.tsv");

                    loadPlots();

                    statusLabel.setText("Status: Notebook executed successfully.");
                } catch (Exception ex) {
                    statusLabel.setText("Status: Failed to execute notebook.");
                    ex.printStackTrace();
                }
            }
        }
    }

    private void setTable(Map<String, Object> data, boolean includeZdeks) {
        DefaultTableModel tableModel = new DefaultTableModel();

        // Always include all columns, including "ZDEKS"
        for (String column : TABLE_COLUMNS) {
            tableModel.addColumn(column);
        }

        List<Double> years = (List<Double>) data.getOrDefault("LATA", Collections.emptyList());
        int rowCount = years.size();

        for (int i = 0; i < rowCount; i++) {
            Object[] row = new Object[TABLE_COLUMNS.length];
            row[0] = years.get(i).intValue(); // Year column

            for (int col = 1; col < TABLE_COLUMNS.length; col++) {
                String columnName = TABLE_COLUMNS[col];

                // Fill ZDEKS column with empty values unless includeZdeks is true
                if (columnName.equals("ZDEKS")) {
                    row[col] = includeZdeks ? getValueFromData(data, columnName, i) : null;
                    continue;
                }

                row[col] = getValueFromData(data, columnName, i);
            }
            tableModel.addRow(row);
        }

        resultsTable.setModel(tableModel);
    }

    private Object getValueFromData(Map<String, Object> data, String key, int index) {
        List<?> values = (List<?>) data.getOrDefault(key, Collections.emptyList());
        return (index < values.size()) ? values.get(index) : null;
    }

    private class CreateAdhocScriptAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea scriptArea = new JTextArea(20, 50);
            scriptArea.setLineWrap(true);
            scriptArea.setWrapStyleWord(true);

            JScrollPane scrollPane = new JScrollPane(scriptArea);
            int result = JOptionPane.showConfirmDialog(
                    ModelFrameworkGUI.this,
                    scrollPane,
                    "Enter Your Ad-hoc Script",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String script = scriptArea.getText();
                if (script.isEmpty()) {
                    JOptionPane.showMessageDialog(ModelFrameworkGUI.this, "Script cannot be empty!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    controller.runScript(script);

                    // Fetch results from results.json
                    String resultsFilePath = "src/main/resources/results.json";
                    Map<String, Object> updatedResults = controller.getResultsFromFile(resultsFilePath);

                    // set the table with updated results
                    setTable(updatedResults, true);

                    // Do not load plots when executing an ad-hoc script
                    plotPanel.removeAll();
                    plotPanel.revalidate();
                    plotPanel.repaint();

                    statusLabel.setText("Status: Ad-hoc script executed successfully.");
                } catch (Exception ex) {
                    statusLabel.setText("Status: Failed to execute ad-hoc script.");
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ModelFrameworkGUI.this, "Error executing script:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void loadPlots() {
        plotPanel.removeAll(); // Clear any existing plots

        // plot imgs paths
        String[] plotPaths = {
                "src/main/resources/large_indices_plot.png",
                "src/main/resources/small_indices_plot.png",
                "src/main/resources/zdeks_plot.png"
        };

        for (String path : plotPaths) {
            File file = new File(path);
            if (file.exists()) {
                ImageIcon icon = new ImageIcon(path);

                // Scale the image to fit the panel size
                Image scaledImage = icon.getImage().getScaledInstance(
                        plotPanel.getWidth() / plotPaths.length,
                        plotPanel.getHeight(),
                        Image.SCALE_SMOOTH
                );

                // Set the scaled image in the label
                JLabel label = new JLabel(new ImageIcon(scaledImage));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                plotPanel.add(label);
            } else {
                JLabel errorLabel = new JLabel("Plot not found: " + path);
                errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                plotPanel.add(errorLabel);
            }
        }

        plotPanel.revalidate();
        plotPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModelFrameworkGUI::new);
    }
}