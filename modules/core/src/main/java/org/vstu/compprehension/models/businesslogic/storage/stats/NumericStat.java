package org.vstu.compprehension.models.businesslogic.storage.stats;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Collection;

@AllArgsConstructor
@Value
public class NumericStat {
    int count;
    double min;
    double mean;
    double max;

    public NumericStat(Collection<Double> evidenceSet) {
        if (evidenceSet.isEmpty())
        {
            this.min   = 0;
            this.max   = 1;
            this.mean  = 0.5;
            this.count = 0;
            return;
        }

        double min  = Double.MAX_VALUE;
        double max  = Double.MIN_VALUE;
        double mean = 0;
        for (Double value : evidenceSet) {
            if (value < min) {
                min = value;
            }
            if (value > max) {
                max = value;
            }
            mean += value;
        }
        mean /= evidenceSet.size();

        this.min   = min;
        this.max   = max;
        this.mean  = mean;
        this.count = evidenceSet.size();
    }

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
}
