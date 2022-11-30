package org.vstu.compprehension.models.businesslogic.storage.stats;

import lombok.Data;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class NumericStat {
    int count;
    double min;
    double mean;
    double max;
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
