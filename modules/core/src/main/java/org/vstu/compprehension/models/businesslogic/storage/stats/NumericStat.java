package org.vstu.compprehension.models.businesslogic.storage.stats;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@Data
public class NumericStat {

    private int count;
    private double min;
    private double mean;
    private double max;
    Set<Double> distinctValues = null;

    public NumericStat(Collection<Double> evidenceSet, boolean rememberValuesGiven) {
        super();

        acceptItems(evidenceSet);
        if (rememberValuesGiven)
            distinctValues = new HashSet<>(evidenceSet);
    }

/*
    public NumericStat(Collection<Integer> evidenceSet, boolean rememberValuesGiven) {
        super();
        // convert
        List<Double> evidenceSetDouble =
                evidenceSet.stream().map(Double::valueOf).collect(Collectors.toList());
        acceptItems(evidenceSetDouble);
        if (rememberValuesGiven)
            distinctValues = new HashSet<>(evidenceSetDouble);
    }
*/

    public double rescaleExternalValue(double value, double rangeMin, double rangeMax) {
        double valueFactor = (value - rangeMin) / (rangeMax - rangeMin);
        return min + valueFactor * (max - min);
    }

    public double rescaleExternalValueViaMean(double value, double rangeMin, double rangeMax) {
        double rangeMean = (rangeMin + rangeMax) / 2;
        double desiredValue = mean;
        if (value <= rangeMean) {
            desiredValue = (
                    min + (value / rangeMean) * (mean - min)
            );  // min + weight * (avg - min)
        }
        else /*if (value > rangeMean)*/ {
            desiredValue = (
                    mean + ((value - rangeMean) / rangeMean) * (max - mean)
            );  // avg + weight * (max - avg)
        }
        return desiredValue;
    }

    private void acceptItems(Collection<Double> evidenceSet) {
        int count = 0;
        min = evidenceSet.stream().min(Double::compare).orElse(0.);
        max = evidenceSet.stream().max(Double::compare).orElse(0.);
        if (!evidenceSet.isEmpty())
            mean = evidenceSet.stream().reduce(Double::sum).orElse(0.) / evidenceSet.size();
        else
            mean = 0;
    }

}
