package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.Model1;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class Controller {
    private final String scriptDir = "src/main/resources/"; // Directory for scripts
    private final String dataDir = "src/main/resources/";   // Directory for data

    // Existing Methods
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

    public void run(String inputFile, String outputFile) {
        // Step 1: Read the original input data
        Map<String, Object> inputData = readJson(inputFile);
        if (inputData == null) {
            System.err.println("Failed to read input data!");
            return;
        }

        // Step 2: Execute the model
        Model1 model = new Model1(((List<?>) inputData.get("LATA")).size());
        model.setData(inputData);
        model.run();

        // Step 3: Get intermediate results from the model
        Map<String, Object> intermediateResults = model.getResults();

        // Step 4: Merge intermediate results back into the original input data
        inputData.putAll(intermediateResults);

        // Step 5: Save merged data to an intermediate file
        String intermediateFile = new File(dataDir + "intermediate.json").getAbsolutePath();
        saveJson(intermediateFile, inputData);

        // Step 6: Run the notebook script
        runNotebook("src/main/resources/script1.ipynb", intermediateFile, outputFile);

        // Step 7: Verify and ensure ZDEKS is added to the final JSON
        Map<String, Object> finalResults = readJson(outputFile);
        if (finalResults != null) {
            inputData.putAll(finalResults); // Add any fields created in the notebook
            saveJson(outputFile, inputData); // Save the complete dataset back to the JSON file
        }

        System.out.println("Results saved to " + outputFile);
    }

    // New Methods
    public void readDataFrom(String fname) {
        Map<String, Object> data = readJson(fname);
        if (data == null) {
            throw new IllegalArgumentException("Failed to read data from file: " + fname);
        }
        System.out.println("Data loaded successfully from: " + fname);
    }

    public void runScriptFromFile(String fname) {
        try {
            String script = new String(Files.readAllBytes(new File(fname).toPath()));
            runScript(script);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    public String getResultsAsTsv(Map<String, Object> results) {
        List<Integer> years = (List<Integer>) results.get("LATA");
        double[] pkb = (double[]) results.get("PKB");
        double[] eks = (double[]) results.get("EKS");

        StringBuilder tsv = new StringBuilder("Year\tPKB\tEKS\n");
        for (int i = 0; i < years.size(); i++) {
            tsv.append(years.get(i))
                    .append("\t")
                    .append(pkb[i])
                    .append("\t")
                    .append(eks[i])
                    .append("\n");
        }

        return tsv.toString();
    }
}