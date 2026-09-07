package org.vstu.compprehension.services;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Random;

@Component
public class RandomProviderImpl implements RandomProvider {
    @Getter
    private Random random;

    public RandomProviderImpl() {
        random = new Random(new Date().getTime());
    }
    public RandomProviderImpl(int seed) {
        random = new Random(seed);
    }

    public void reset(int newSeed) {
        random.setSeed(newSeed);
    }
}
