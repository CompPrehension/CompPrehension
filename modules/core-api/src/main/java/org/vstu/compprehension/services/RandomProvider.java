package org.vstu.compprehension.services;

import java.util.Random;

public interface RandomProvider {
    Random getRandom();
    void reset(int seed);
}
