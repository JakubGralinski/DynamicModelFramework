package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.*;

public class Controller {
    private final String scriptDir = "src/main/resources/"; // Directory for scripts
    private final String dataDir = "src/main/resources/";   // Directory for data

    /**
     * Reads JSON data from a file and returns it as a Map.
     * @param filePath the path of the JSON file
     * @return a Map representing the JSON data
     */
    public Map<String, Object> readJson(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            return gson.fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Saves data to a JSON file.
     * @param filePath the path to save the JSON file
     * @param data the data to be saved
     */
    public void saveJson(String filePath, Map<String, Object> data) {
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new Gson();
            gson.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a Jupyter notebook by calling nbconvert.
     * @param notebookPath the path to the notebook
     * @param inputPath the path to the input JSON file
     * @param outputPath the path to the output JSON file
     */
    public void runNotebook(String notebookPath, String inputPath, String outputPath) {
        try {
            // Ensure the input and output files exist or can be created
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                throw new FileNotFoundException("Input file not found: " + inputPath);
            }

            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs(); // Create directories if they don't exist

            // Prepare the process to execute the notebook
            ProcessBuilder pb = new ProcessBuilder(
                    "jupyter-nbconvert", "--to", "notebook", "--execute",
                    "--output", "output.ipynb",
                    "--ExecutePreprocessor.timeout=300", notebookPath
            );

            // Pass input and output paths as environment variables
            Map<String, String> env = pb.environment();
            env.put("INPUT_PATH", inputFile.getAbsolutePath());
            env.put("OUTPUT_PATH", outputFile.getAbsolutePath());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Log the notebook execution output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // Check if the notebook execution succeeded
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Notebook execution failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during notebook execution", e);
        }
    }

    /**
     * Dynamically selects a model (Model1 or Model2) and executes it with the provided data.
     * @param inputFile the path to the input JSON file
     * @param outputFile the path to the output JSON file
     * @param modelName the name of the model to use ("Model1" or "Model2")
     */
    public void runModel(String inputFile, String outputFile, String modelName) {
        // Step 1: Read the original input data
        Map<String, Object> inputData = readJson(inputFile);
        if (inputData == null) {
            System.err.println("Failed to read input data!");
            return;
        }

        // Step 2: Instantiate the chosen model dynamically
        ModelBase model;
        if ("Model1".equalsIgnoreCase(modelName)) {
            model = new Model1(((List<?>) inputData.get("LATA")).size());
        } else if ("Model2".equalsIgnoreCase(modelName)) {
            model = new Model2(((List<?>) inputData.get("LATA")).size());
        } else {
            throw new IllegalArgumentException("Unknown model: " + modelName);
        }

        // Step 3: Execute the model
        model.setData(inputData);
        model.run();

        // Step 4: Get intermediate results from the model
        Map<String, Object> intermediateResults = model.getResults();

        // Step 5: Merge intermediate results back into the original input data
        inputData.putAll(intermediateResults);

        // Step 6: Save merged data to an intermediate file
        String intermediateFile = new File(dataDir + "intermediate.json").getAbsolutePath();
        saveJson(intermediateFile, inputData);

        // Step 7: Run the notebook script
        runNotebook("src/main/resources/script1.ipynb", intermediateFile, outputFile);

        // Step 8: Verify and ensure ZDEKS is added to the final JSON
        Map<String, Object> finalResults = readJson(outputFile);
        if (finalResults != null) {
            inputData.putAll(finalResults); // Add any fields created in the notebook
            saveJson(outputFile, inputData); // Save the complete dataset back to the JSON file
        }

        System.out.println("Results saved to " + outputFile);
    }

    /**
     * Reads data from a file and validates its structure.
     * @param fname the path to the data file
     */
    public void readDataFrom(String fname) {
        Map<String, Object> data = readJson(fname);
        if (data == null) {
            throw new IllegalArgumentException("Failed to read data from file: " + fname);
        }
        System.out.println("Data loaded successfully from: " + fname);
    }

    /**
     * Runs a Python script provided as a string.
     * @param script the Python script to execute
     */
    public void runScript(String script) {
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
}