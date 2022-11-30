package org.vstu.compprehension.models.businesslogic.storage.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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
                .map(this.items::get)
                .collect(Collectors.toList());
    }

    public List<Integer> keysHavingSomeBits(int maskBits, int minCommonBits) {
        return items.keySet().stream()
                .filter(k -> (countBits(k & maskBits) >= minCommonBits))
                .map(this.items::get)
                .collect(Collectors.toList());
    }

    public List<Integer> keysWithBits(int requiredBits, int optionalBits, int minCommonBits, int forbiddenBits) {
        assert (requiredBits & forbiddenBits) == 0;
        return items.keySet().stream()
                .filter(k ->
                        ((k & forbiddenBits) == 0) &&
                        ((k & requiredBits) == requiredBits) &&
                        (countBits(k & optionalBits) >= minCommonBits))
                .map(this.items::get)
                .collect(Collectors.toList());
    }

     public static int countBits(int i) {
         int count = 0;
         while (i != 0) {
             count += i & 0x1;
             i >>= 1;
         }
         return count;
     }

     public static int countCommonBits(int i1, int i2) {
         return 0;
     }
}
