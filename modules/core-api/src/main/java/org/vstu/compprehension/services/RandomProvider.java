package org.vstu.compprehension.services;

import java.util.random.RandomGenerator;

public interface RandomProvider {
    RandomGenerator getRandom();
    void reset(int seed);
}
