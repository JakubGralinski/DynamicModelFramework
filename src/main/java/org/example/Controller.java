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
            ProcessBuilder pb = new ProcessBuilder(
                    "jupyter-nbconvert", "--to", "notebook", "--execute",
                    "--output", "output.ipynb",
                    "--ExecutePreprocessor.timeout=300",
                    notebookPath
            );
            pb.redirectErrorStream(true);

            Map<String, String> env = pb.environment();
            env.put("INPUT_PATH", inputPath);
            env.put("OUTPUT_PATH", outputPath);

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

    public void run(String inputFile, String outputFile) {
        Map<String, Object> inputData = readJson(inputFile);
        if (inputData == null) {
            System.err.println("Failed to read input data!");
            return;
        }

        Model1 model = new Model1(((List<?>) inputData.get("LATA")).size());
        model.setData(inputData);
        model.run();

        Map<String, Object> intermediateResults = model.getResults();
        String intermediateFile = new File(dataDir + "intermediate.json").getAbsolutePath();
        saveJson(intermediateFile, intermediateResults);

        outputFile = new File(dataDir + "results.json").getAbsolutePath();
        String notebookPath = scriptDir + "script1.ipynb";

        runNotebook(notebookPath, intermediateFile, outputFile);

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