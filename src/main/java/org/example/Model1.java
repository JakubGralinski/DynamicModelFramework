package org.example;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class Model1 {
    private int LL; // Number of years

    @Bind("twKI")
    private double[] twKI;

    @Bind("twKS")
    private double[] twKS;

    @Bind("twINW")
    private double[] twINW;

    @Bind("twEKS")
    private double[] twEKS;

    @Bind("twIMP")
    private double[] twIMP;

    @Bind("KI")
    private double[] KI;

    @Bind("KS")
    private double[] KS;

    @Bind("INW")
    private double[] INW;

    @Bind("EKS")
    private double[] EKS;

    @Bind("IMP")
    private double[] IMP;

    private double[] PKB;

    public Model1(int years) {
        this.LL = years;
        initializeArrays();
    }

    private void initializeArrays() {
        PKB = new double[LL];
    }

    public void setData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Input data is null or empty.");
        }

        System.out.println("Data received in setData: " + data); // Debugging log

        List<Double> lata = (List<Double>) data.get("LATA");
        if (lata == null || lata.isEmpty()) {
            throw new IllegalArgumentException("The 'LATA' field is missing or empty in the input data.");
        }
        this.LL = lata.size();
        initializeArrays();

        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Bind.class)) {
                Bind bind = field.getAnnotation(Bind.class);
                String key = bind.value();
                List<Double> values = (List<Double>) data.getOrDefault(key, Collections.emptyList());

                System.out.println("Key: " + key + ", Values: " + values); // Debugging log

                double[] initialValues = toDoubleArray(values);
                if (initialValues.length == 0) {
                    System.out.println("Warning: Field '" + key + "' is empty. Using default values.");
                    initialValues = new double[LL];
                    Arrays.fill(initialValues, 1.0);
                }

                try {
                    field.setAccessible(true);
                    field.set(this, extrapolate(initialValues, LL));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to set field value for " + key, e);
                }
            }
        }
    }

    protected double[] extrapolate(double[] initialValues, int targetLength) {
        if (initialValues == null || initialValues.length == 0) {
            throw new IllegalArgumentException("Initial values array is null or empty.");
        }

        double[] result = new double[targetLength];
        for (int i = 0; i < targetLength; i++) {
            result[i] = (i < initialValues.length) ? initialValues[i] : initialValues[initialValues.length - 1];
        }
        return result;
    }

    private double[] toDoubleArray(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    public void run() {
        PKB[0] = KI[0] + KS[0] + INW[0] + EKS[0] - IMP[0];

        for (int t = 1; t < LL; t++) {
            KI[t] = twKI[t % twKI.length] * KI[t - 1];
            KS[t] = twKS[t % twKS.length] * KS[t - 1];
            INW[t] = twINW[t % twINW.length] * INW[t - 1];
            EKS[t] = twEKS[t % twEKS.length] * EKS[t - 1];
            IMP[t] = twIMP[t % twIMP.length] * IMP[t - 1];
            PKB[t] = KI[t] + KS[t] + INW[t] + EKS[t] - IMP[t];
        }
    }

    public Map<String, Object> getResults() {
        Map<String, Object> results = new HashMap<>();

        // Instead of storing arrays like this:
        // results.put("LATA", generateYears().toArray(new Integer[0]));
        // store them as a List<Integer>:
        List<Integer> years = generateYears();
        results.put("LATA", years);

        // Instead of storing PKB, EKS as double[], store them as List<Double>:
        results.put("PKB", Arrays.stream(PKB).boxed().collect(Collectors.toList()));
        results.put("EKS", Arrays.stream(EKS).boxed().collect(Collectors.toList()));

        return results;
    }

    private List<Integer> generateYears() {
        List<Integer> years = new ArrayList<>();
        for (int i = 2015; i < 2015 + LL; i++) {
            years.add(i);
        }
        return years;
    }
}