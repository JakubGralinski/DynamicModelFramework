package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.*;
import models.ModelBase;

import java.io.*;
import java.util.*;

public class Controller {
    private final String dataDir = "src/main/resources/";   // Directory for data

    public Map<String, Object> readJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveJson(String filePath, Map<String, Object> data) {
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new Gson();
            gson.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void runNotebook(String notebookPath, String inputPath, String outputPath) {
        try {
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                throw new FileNotFoundException("Input file not found: " + inputPath);
            }

            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs(); // Create directories if they don't exist

            // prepare the process to notebook exec
            ProcessBuilder pb = new ProcessBuilder(
                    "jupyter-nbconvert", "--to", "notebook", "--execute",
                    "--output", "output.ipynb",
                    "--ExecutePreprocessor.timeout=300", notebookPath
            );

            Map<String, String> env = pb.environment();
            env.put("INPUT_PATH", inputFile.getAbsolutePath());
            env.put("OUTPUT_PATH", outputFile.getAbsolutePath());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // log the notebook exec output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Check if the executuioon was success
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Notebook execution failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during notebook execution", e);
        }
    }

    public void runModel(String inputFile, String outputFile, String modelName) {
        //  read the original input data
        Map<String, Object> inputData = readJson(inputFile);
        if (inputData == null) {
            System.err.println("Failed to read input data!");
            return;
        }

        // Instantiate the chosen model version
        ModelBase model;
        if ("Model1".equalsIgnoreCase(modelName)) {
            model = new Model1(((List<?>) inputData.get("LATA")).size());
        } else if ("Model2".equalsIgnoreCase(modelName)) {
            model = new Model2(((List<?>) inputData.get("LATA")).size());
        } else if ("Model3".equalsIgnoreCase(modelName)) {
            model = new Model3(((List<?>) inputData.get("LATA")).size());
        } else {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }

        model.setData(inputData);
        model.run();

        Map<String, Object> intermediateResults = model.getResults();

        // Merge intermediate results with input data
        inputData.putAll(intermediateResults);

        // Save merged data
        String intermediateFile = new File(dataDir + "intermediate.json").getAbsolutePath();
        saveJson(intermediateFile, inputData);

        runNotebook("src/main/resources/script1.ipynb", intermediateFile, outputFile);

        // ensure ZDEKS is added to the final JSON
        Map<String, Object> finalResults = readJson(outputFile);
        if (finalResults != null) {
            inputData.putAll(finalResults); // Add any fields created in the notebook
            saveJson(outputFile, inputData); // Save the complete dataset back to the JSON file
        }

        System.out.println("Results saved to " + outputFile);
    }

    public void readDataFrom(String fname) {
        Map<String, Object> data = readJson(fname);
        if (data == null) {
            throw new IllegalArgumentException("Failed to read data from file: " + fname);
        }
        System.out.println("Data loaded successfully from: " + fname);
    }


    public void runScript(String script) { // Run script from string
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", script);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getResultsFromFile(String resultsFilePath) throws IOException {
        try (Reader reader = new FileReader(resultsFilePath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
        }
    }

    public void getResultsAsTsv(Map<String, Object> results, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Extract cols
            List<String> columns = results.keySet().stream().toList();

            // Write the header row
            writer.write(String.join("\t", columns) + "\n");

            int rowCount = ((List<?>) results.get(columns.get(0))).size();

            for (int i = 0; i < rowCount; i++) {
                StringBuilder row = new StringBuilder();
                for (String column : columns) {
                    List<?> values = (List<?>) results.get(column);
                    row.append(values.get(i)).append("\t");
                }
                writer.write(row.substring(0, row.length() - 1) + "\n"); // Remove trailing tab
            }

            System.out.println("TSV file saved to: " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing TSV: " + e.getMessage());
        }
    }
}