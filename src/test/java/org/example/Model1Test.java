package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertArrayEquals;

public class Model1Test {

    @Test
    public void testExtrapolation() {
        Model1 model = new Model1(5);

        // Test extrapolation of shorter data
        double[] shortData = {1.0, 2.0};
        double[] result = model.extrapolate(shortData, 5); // Ensure extrapolate is accessible
        assertArrayEquals(new double[]{1.0, 2.0, 2.0, 2.0, 2.0}, result, 0.001);

        // Test no extrapolation when data matches length
        double[] exactData = {1.0, 2.0, 3.0, 4.0, 5.0};
        result = model.extrapolate(exactData, 5);
        assertArrayEquals(exactData, result, 0.001);

        // Test extrapolation when data is longer than needed
        double[] longData = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        result = model.extrapolate(longData, 5);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0, 5.0}, result, 0.001);
    }

}