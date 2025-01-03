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

public class ModelSimulationGUI extends JFrame {
    private Controller controller;
    private JTable resultsTable;
    private JLabel statusLabel;
    private JComboBox<String> modelSelector; // Dropdown for model selection
    private String dataFilePath = null;

    // All columns that must be displayed in the table
    private static final String[] TABLE_COLUMNS = {
            "Year", "twKI", "twKS", "twINW", "twEKS", "twIMP",
            "KI", "KS", "INW", "EKS", "IMP", "PKB", "NET_EXPORTS", "ZDEKS"
    };

    public ModelSimulationGUI() {
        controller = new Controller();

        setTitle("Model Simulation GUI");
        setSize(1200, 800);
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
        modelSelector = new JComboBox<>(new String[]{"Model1", "Model2"});
        panel.add(modelSelector);

        JButton runModelButton = new JButton("Run Model");
        runModelButton.addActionListener(new RunModelAction());
        panel.add(runModelButton);

        JButton executeScriptButton = new JButton("Execute Script");
        executeScriptButton.addActionListener(new ExecuteScriptAction());
        panel.add(executeScriptButton);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Results table with predefined columns
        resultsTable = new JTable(new DefaultTableModel(TABLE_COLUMNS, 0));
        JScrollPane tableScrollPane = new JScrollPane(resultsTable);
        panel.add(tableScrollPane, BorderLayout.CENTER);

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
            int returnValue = fileChooser.showOpenDialog(ModelSimulationGUI.this);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                dataFilePath = selectedFile.getAbsolutePath();

                try {
                    controller.readDataFrom(dataFilePath);
                    statusLabel.setText("Status: Data loaded from " + dataFilePath);

                    // Load the initial data into the table
                    Map<String, Object> data = controller.readJson(dataFilePath);
                    populateTable(data, false);
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
                populateTable(results, false); // Do not show ZDEKS yet
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
            int returnValue = fileChooser.showOpenDialog(ModelSimulationGUI.this);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                dataFilePath = selectedFile.getAbsolutePath();
                try {
                    String notebookPath = dataFilePath;
                    String inputPath = "src/main/resources/intermediate.json";
                    String outputPath = "src/main/resources/results.json";

                    // Run the Jupyter Notebook using nbconvert
                    controller.runNotebook(notebookPath, inputPath, outputPath);

                    // Read the results after notebook execution
                    Map<String, Object> results = controller.readJson(outputPath);

                    // Populate the table and include ZDEKS values
                    populateTable(results, true); // Now include ZDEKS values
                    statusLabel.setText("Status: Notebook executed successfully.");
                } catch (Exception ex) {
                    statusLabel.setText("Status: Failed to execute notebook.");
                    ex.printStackTrace();
                }
            }
        }
    }

    private void populateTable(Map<String, Object> data, boolean includeZdeks) {
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModelSimulationGUI::new);
    }
}