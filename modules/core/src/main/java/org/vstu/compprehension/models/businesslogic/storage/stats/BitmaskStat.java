package org.vstu.compprehension.models.businesslogic.storage.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Long.bitCount;

public class BitmaskStat extends CategoricalStat<Integer> {

    public BitmaskStat(Collection<Integer> evidenceSet) {
        super();

        for (Integer key : evidenceSet) {
            int count = 0;
            if (items.containsKey(key)) {
                count = items.get(key) + 1;
            }
            items.put(key, count);
        }
    }

    public List<Integer> keysHavingAllBits(int requiredBits) {
        return items.keySet().stream()
                .filter(k -> ((k & requiredBits) == requiredBits))
                .collect(Collectors.toList());
    }

    public List<Integer> keysHavingSomeBits(int maskBits, int minCommonBits) {
        return items.keySet().stream()
                .filter(k -> (bitCount(k & maskBits) >= minCommonBits))
                .collect(Collectors.toList());
    }

    public List<Integer> keysWithBits(int requiredBits, int optionalBits, int minCommonBits, int forbiddenBits) {
        assert (requiredBits & forbiddenBits) == 0;
        return items.keySet().stream()
                .filter(k ->
                        ((k & forbiddenBits) == 0) &&
                        ((k & requiredBits) == requiredBits) &&
                        (bitCount(k & optionalBits) >= minCommonBits))
                .collect(Collectors.toList());
    }
}
