package org.vstu.compprehension.models.businesslogic.storage.stats;

import lombok.Data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Data
public class CategoricalStat<T> {
    Map<T, Integer> items = null;

    public CategoricalStat() {
        items = new HashMap<>(2);
    }

    public int totalCount() {
        return items.values().stream().reduce(Integer::sum).orElse(0);
    }

    public int sumForKeys(Collection<? extends T> keys) {
        if (keys == null) {
            System.out.println("WARN: called CategoricalStat.sumForKeys(null) !!! ");
            return 0;
        }
        return keys.stream()
                .map(k -> items.getOrDefault(k, 0))
                .reduce(Integer::sum).orElse(0);
    }

//    public static CategoricalStat<Integer> fromIntCollection(Collection<Integer> items) {
//        // ...
//        return new CategoricalStat<Integer>();
//    }
}
