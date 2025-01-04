package models;

import org.example.Bind;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class Model3 implements ModelBase {
    private int LL; // # of years

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
    private double[] NetExports;
    private double[] shockFactor; // Random shocks for each year

    private List<Integer> lata;

    public Model3(int lata) {
    }

    private void initializeArrays() {
        PKB = new double[LL];
        NetExports = new double[LL];
        shockFactor = new double[LL];
    }

    public void setData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Input data is null or empty.");
        }

        List<Double> lataDoubles = (List<Double>) data.get("LATA");
        if (lataDoubles == null || lataDoubles.isEmpty()) {
            throw new IllegalArgumentException("The 'LATA' field is missing or empty in the input data.");
        }

        this.lata = lataDoubles.stream().map(Double::intValue).collect(Collectors.toList());
        this.LL = lata.size();
        initializeArrays();

        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Bind.class)) {
                Bind bind = field.getAnnotation(Bind.class);
                String key = bind.value();
                List<Double> values = (List<Double>) data.getOrDefault(key, Collections.emptyList());

                double[] initialValues = toDoubleArray(values);
                if (initialValues.length == 0) {
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

    private double[] extrapolate(double[] initialValues, int targetLength) {
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
        Random random = new Random();
        for (int t = 0; t < LL; t++) {
            shockFactor[t] = 1 + (random.nextDouble() * 0.1 - 0.05); // Random shock: +/- 5%

            if (t == 0) {
                PKB[t] = (KI[t] + KS[t] + INW[t] + EKS[t]) * shockFactor[t] - IMP[t];
            } else {
                KI[t] = twKI[t % twKI.length] * KI[t - 1] * 0.98;
                KS[t] = twKS[t % twKS.length] * KS[t - 1] * 0.97;
                INW[t] = twINW[t % twINW.length] * INW[t - 1];
                EKS[t] = (twEKS[t % twEKS.length] * EKS[t - 1]) + (0.05 * KI[t] + 0.03 * KS[t]);
                IMP[t] = twIMP[t % twIMP.length] * IMP[t - 1] * shockFactor[t]; // Random shock impact
                PKB[t] = (KI[t] + KS[t] + INW[t] + EKS[t]) * shockFactor[t] - IMP[t];
            }

        }
    }

    public Map<String, Object> getResults() {
        Map<String, Object> results = new HashMap<>();
        results.put("LATA", lata);
        results.put("PKB", Arrays.stream(PKB).boxed().collect(Collectors.toList()));
        results.put("SHOCK_FACTOR", Arrays.stream(shockFactor).boxed().collect(Collectors.toList())); // new shocks
        return results;
    }
}