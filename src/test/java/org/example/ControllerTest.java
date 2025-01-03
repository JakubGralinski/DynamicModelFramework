package org.example;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerTest {

    private final String testInputPath = "src/main/resources/data.json";
    private final String testOutputPath = "src/main/resources/results.json";

    @Test
    public void testReadJson() throws Exception {
        Controller controller = new Controller();

        // Write a sample JSON file for testing
        Files.writeString(new File(testInputPath).toPath(), "{\"LATA\": [2015, 2016, 2017]}");

        // Test reading the JSON
        Map<String, Object> data = controller.readJson(testInputPath);
        assertNotNull(data);
        assertEquals(3, ((List<?>) data.get("LATA")).size());
    }

    @Test
    public void testSaveJson() throws Exception {
        Controller controller = new Controller();
        Map<String, Object> data = Map.of("LATA", List.of(2015, 2016, 2017));

        // Save data to a file
        controller.saveJson(testOutputPath, data);

        // Verify the file content
        String content = Files.readString(new File(testOutputPath).toPath());
        assertTrue(content.contains("2015"));
        assertTrue(content.contains("2017"));
    }


    @Test
    public void testLargeDataset() throws Exception {
        Controller controller = new Controller();

        // Generate large JSON
        StringBuilder largeJson = new StringBuilder("{\"LATA\": [");
        for (int i = 0; i < 10000; i++) {
            largeJson.append(2015 + i).append(i < 9999 ? "," : "");
        }
        largeJson.append("]}");

        // Ensure the directory exists
        Path testResourcesPath = Paths.get("src/test/resources");
        Files.createDirectories(testResourcesPath);

        String testInputPath = "src/test/resources/testLargeDataset.json";
        Files.writeString(new File(testInputPath).toPath(), largeJson.toString());

        // Read large dataset
        Map<String, Object> data = controller.readJson(testInputPath);

        // Validate large dataset handling
        assertNotNull(data);
        assertEquals(10000, ((List<?>) data.get("LATA")).size());
    }

    @Test
    public void testHandleMissingJsonFields() throws Exception {
        Controller controller = new Controller();

        // JSON with missing growth rates
        String missingFieldsJson = "{\"LATA\": [2015, 2016, 2017]}";

        // Ensure the directory exists
        Path testResourcesPath = Paths.get("src/test/resources");
        Files.createDirectories(testResourcesPath);

        String testInputPath = "src/test/resources/testMissingFields.json";
        Files.writeString(new File(testInputPath).toPath(), missingFieldsJson);

        Map<String, Object> data = controller.readJson(testInputPath);

        // Validate handling of missing fields
        assertNotNull(data);
        assertNotNull(data.get("LATA"));
        assertNull(data.get("twKI")); // twKI is missing
    }
}