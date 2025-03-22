package org.vstu.compprehension.models.businesslogic.storage.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Long.bitCount;

public class BitmaskStat extends CategoricalStat<Long> {

    public BitmaskStat(Collection<Long> evidenceSet) {
        super();

        for (Long key : evidenceSet) {
            int count = 0;
            if (items.containsKey(key)) {
                count = items.get(key);
            }
            items.put(key, count + 1);
        }
    }

    public List<Long> keysHavingAllBits(long requiredBits) {
        return items.keySet().stream()
                .filter(k -> ((k & requiredBits) == requiredBits))
                .collect(Collectors.toList());
    }

    public List<Long> keysHavingSomeBits(long maskBits, int minCommonBits) {
        return items.keySet().stream()
                .filter(k -> (bitCount(k & maskBits) >= minCommonBits))
                .collect(Collectors.toList());
    }

    public List<Long> keysWithBits(long requiredBits, long optionalBits, long minCommonBits, long forbiddenBits) {
        assert (requiredBits & forbiddenBits) == 0;
        return items.keySet().stream()
                .filter(k ->
                        ((k & forbiddenBits) == 0) &&
                        ((k & requiredBits) == requiredBits) &&
                        (bitCount(k & optionalBits) >= minCommonBits))
                .collect(Collectors.toList());
    }

    public List<Long> keysWithBits(long requiredBits, long optionalBits, int minCommonBits, long forbiddenBits, long unwantedOptionalBits, int minCommonUnwOptBits) {
        assert (requiredBits & forbiddenBits) == 0;
        return items.keySet().stream()
                .filter(k ->
                        ((k & forbiddenBits) == 0 &&
                                bitCount(k & unwantedOptionalBits) <= minCommonUnwOptBits) &&
                        ((k & requiredBits) == requiredBits) &&
                        (bitCount(k & optionalBits) >= minCommonBits))
                .collect(Collectors.toList());
    }
}
