package org.example;

import org.example.Controller;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

public class ModelSimulationGUI extends JFrame {
    private Controller controller;
    private JTextArea scriptArea;
    private JTable resultsTable;
    private JLabel statusLabel;

    // Keep track of which file user chose:
    private String dataFilePath = null;

    public ModelSimulationGUI() {
        controller = new Controller();

        setTitle("Model Simulation GUI");
        setSize(800, 600);
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

        // Script input area
        scriptArea = new JTextArea(10, 50);
        JScrollPane scriptScrollPane = new JScrollPane(scriptArea);
        panel.add(scriptScrollPane, BorderLayout.NORTH);

        // Results table
        resultsTable = new JTable(new DefaultTableModel(new Object[]{"Year", "PKB", "EKS"}, 0));
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
                dataFilePath = selectedFile.getAbsolutePath();  // Store path here!

                try {
                    // Optionally read or validate the data
                    controller.readDataFrom(dataFilePath);
                    statusLabel.setText("Status: Data loaded from " + dataFilePath);
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
            // Use the file path you captured earlier:
            if (dataFilePath == null || dataFilePath.isEmpty()) {
                statusLabel.setText("Status: No file selected!");
                return;
            }

            try {
                // Pass the chosen file to your model.
                // In your original code, you used something like:
                // controller.run("src/main/resources/intermediate.json", "src/main/resources/results.json");
                // but we want the *actual* file chosen by user (dataFilePath):
                controller.run(dataFilePath, "src/main/resources/results.json");

                // Now read the results from results.json and populate the table:
                Map<String, Object> results = controller.readJson("src/main/resources/results.json");

                DefaultTableModel tableModel = (DefaultTableModel) resultsTable.getModel();
                tableModel.setRowCount(0); // Clear table

                // Convert objects from Map back to Lists
                // (Adjust types or keys to whatever your JSON actually has)
                java.util.List<Double> yearsList = (java.util.List<Double>) results.get("LATA");
                java.util.List<Double> pkbList   = (java.util.List<Double>) results.get("PKB");
                java.util.List<Double> eksList   = (java.util.List<Double>) results.get("EKS");

                // If you need int years:
                java.util.List<Integer> years = yearsList.stream()
                        .map(Double::intValue)
                        .toList();

                double[] pkb = pkbList.stream().mapToDouble(Double::doubleValue).toArray();
                double[] eks = eksList.stream().mapToDouble(Double::doubleValue).toArray();

                for (int i = 0; i < years.size(); i++) {
                    tableModel.addRow(new Object[]{years.get(i), pkb[i], eks[i]});
                }

                statusLabel.setText("Status: Model run successfully using " + dataFilePath);
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
            String script = scriptArea.getText();
            try {
                controller.runScript(script);
                statusLabel.setText("Status: Script executed successfully.");
            } catch (Exception ex) {
                statusLabel.setText("Status: Failed to execute script.");
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModelSimulationGUI::new);
    }
}