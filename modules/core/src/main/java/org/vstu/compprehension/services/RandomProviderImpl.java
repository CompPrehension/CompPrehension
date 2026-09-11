package org.vstu.compprehension.services;

import org.springframework.stereotype.Component;

import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

@Component
public class RandomProviderImpl implements RandomProvider {
    private final ThreadLocal<RandomGenerator> random;

    public RandomProviderImpl() {
        random = ThreadLocal.withInitial(SplittableRandom::new);
    }

    public RandomProviderImpl(int seed) {
        random = ThreadLocal.withInitial(() -> new SplittableRandom(seed));
    }

    @Override
    public RandomGenerator getRandom() {
        return random.get();
    }

    @Override
    public void reset(int newSeed) {
        random.set(new SplittableRandom(newSeed));
    }
}
