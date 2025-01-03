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
    private String dataFilePath = null;

    // All columns that must be displayed in the table
    private static final String[] TABLE_COLUMNS = {
            "Year", "twKI", "twKS", "twINW", "twEKS", "twIMP",
            "KI", "KS", "INW", "EKS", "IMP", "PKB", "ZDEKS"
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

    // Action for "Load Data" button
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

    // Action for "Run Model" button
    private class RunModelAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (dataFilePath == null || dataFilePath.isEmpty()) {
                statusLabel.setText("Status: No file selected!");
                return;
            }

            try {
                controller.run(dataFilePath, "src/main/resources/results.json");
                Map<String, Object> results = controller.readJson("src/main/resources/results.json");
                populateTable(results, false); // Do not show ZDEKS yet
                statusLabel.setText("Status: Model executed successfully.");
            } catch (Exception ex) {
                statusLabel.setText("Status: Failed to run model.");
                ex.printStackTrace();
            }
        }
    }

    // Action for "Execute Script" button
    private class ExecuteScriptAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String script = "import os\n"
                        + "import json\n"
                        + "input_path = os.getenv('INPUT_PATH')\n"
                        + "output_path = os.getenv('OUTPUT_PATH')\n"
                        + "with open(input_path, 'r') as f:\n"
                        + "    data = json.load(f)\n"
                        + "PKB = data.get('PKB', [])\n"
                        + "EKS = data.get('EKS', [])\n"
                        + "ZDEKS = [e / p if p != 0 else 0 for e, p in zip(EKS, PKB)]\n"
                        + "data['ZDEKS'] = ZDEKS\n"
                        + "with open(output_path, 'w') as f:\n"
                        + "    json.dump(data, f, indent=4)";
                controller.runScript(script);

                Map<String, Object> results = controller.readJson("src/main/resources/results.json");
                populateTable(results, true); // Show ZDEKS after execution
                statusLabel.setText("Status: Script executed successfully.");
            } catch (Exception ex) {
                statusLabel.setText("Status: Failed to execute script.");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Populates the table with all fields, optionally including ZDEKS.
     *
     * @param data       The JSON data as a Map.
     * @param includeZdeks Whether to include the ZDEKS column.
     */
    private void populateTable(Map<String, Object> data, boolean includeZdeks) {
        DefaultTableModel tableModel = new DefaultTableModel();
        for (String column : TABLE_COLUMNS) {
            if (!includeZdeks && column.equals("ZDEKS")) {
                continue; // Skip ZDEKS if not requested
            }
            tableModel.addColumn(column);
        }

        // Ensure all columns have data
        List<Double> years = (List<Double>) data.getOrDefault("LATA", Collections.emptyList());
        int rowCount = years.size();

        for (int i = 0; i < rowCount; i++) {
            Object[] row = new Object[TABLE_COLUMNS.length];
            row[0] = years.get(i).intValue(); // Year column

            // Populate each field dynamically
            for (int col = 1; col < TABLE_COLUMNS.length; col++) {
                String columnName = TABLE_COLUMNS[col];
                if (!includeZdeks && columnName.equals("ZDEKS")) {
                    row[col] = null; // Skip ZDEKS if not available
                    continue;
                }

                List<?> values = (List<?>) data.getOrDefault(columnName, Collections.emptyList());
                row[col] = (i < values.size()) ? values.get(i) : null;
            }
            tableModel.addRow(row);
        }

        resultsTable.setModel(tableModel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModelSimulationGUI::new);
    }
}