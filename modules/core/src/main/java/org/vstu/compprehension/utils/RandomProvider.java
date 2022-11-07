package org.vstu.compprehension.utils;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Date;
import java.util.Random;

@Component @SessionScope
public class RandomProvider {
    @Getter
    private Random random;

    public RandomProvider() {
        random = new Random(new Date().getTime());
    }
    public RandomProvider(int seed) {
        random = new Random(seed);
    }

    public void reset(int newSeed) {
        random.setSeed(newSeed);
    }
}
