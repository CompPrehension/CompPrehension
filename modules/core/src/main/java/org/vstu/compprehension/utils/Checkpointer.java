package org.vstu.compprehension.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Checkpointer {
    private final Level logLevel;
    //        'Measures time between hits. Requires the `from timeit import default_timer as timer`'
    double first, last;
    Logger log;

    public Checkpointer(Logger log) {
        reset_now();
        this.log = log;
        this.logLevel = Level.INFO;
    }

    public Checkpointer(@NotNull Logger log, @NotNull Level logLevel) {
        reset_now();
        this.log = log;
        this.logLevel = logLevel;
    }

    public double timer() {
        return (double) (System.nanoTime() / 1000) / 1000000;
    }

    public void reset_now() {
        this.first = timer();
        this.last = this.first;
    }

    public double hit(@Nullable String label) {

        double now = timer();
        double delta = now - this.last;
        if (label != null)
            log.printf(logLevel, "%s: %.3fs", !label.isEmpty() ? label : "Checkpoint", delta);
        this.last = now;
        return delta;
    }

    public double since_start(@Nullable String label) {
        return since_start(label, false);
    }

    public double since_start(@Nullable String label, boolean hit) {
        double now = timer();
        double delta = now - this.first;
        if (label != null)
            log.printf(logLevel, "%s: %.3fs", !label.isEmpty() ? label : "Total", delta);
        if (hit)
            this.last = now;
        return delta;
    }

}
