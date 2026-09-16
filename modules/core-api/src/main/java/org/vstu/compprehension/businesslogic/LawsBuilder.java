package org.vstu.compprehension.businesslogic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LawsBuilder {
    private final LinkedHashMap<String, PositiveLaw> positive = new LinkedHashMap<>();
    private final LinkedHashMap<String, NegativeLaw> negative = new LinkedHashMap<>();
    private final Map<String, Long> bits = new HashMap<>();

    public LawsBuilder add(Law law) {
        Law previous = law instanceof PositiveLaw p
                ? positive.putIfAbsent(p.getName(), p)
                : negative.putIfAbsent(law.getName(), (NegativeLaw) law);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate law: " + law.getName());
        }
        return this;
    }

    public LawsBuilder addAll(Collection<? extends Law> laws) {
        laws.forEach(this::add);
        return this;
    }

    public LawsBuilder bits(Map<String, Long> nameToBit) {
        bits.putAll(nameToBit);
        return this;
    }

    public Laws build() {
        for (Law law : positive.values()) {
            law.setLawsImplied(resolve(law.getImpliesLaws(), positive));
        }
        for (Law law : negative.values()) {
            law.setLawsImplied(resolve(law.getImpliesLaws(), negative));
        }
        Map<Law, Set<Law>> children = new HashMap<>();
        Stream.concat(positive.values().stream(), negative.values().stream()).forEach(law -> {
            Long bit = bits.get(law.getName());
            if (bit != null) {
                law.setBitmask(bit);
            }
            for (Law base : law.getLawsImplied()) {
                children.computeIfAbsent(base, k -> new HashSet<>()).add(law);
            }
        });
        Stream.concat(positive.values().stream(), negative.values().stream())
                .forEach(law -> law.setChildLaws(children.getOrDefault(law, Set.of())));
        return new Laws(
                Collections.unmodifiableMap(new LinkedHashMap<>(positive)),
                Collections.unmodifiableMap(new LinkedHashMap<>(negative)));
    }

    private static Set<Law> resolve(List<String> names, Map<String, ? extends Law> pool) {
        if (names == null) {
            return Set.of();
        }
        return names.stream().map(pool::get).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}
