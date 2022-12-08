package org.vstu.compprehension.utils;

import org.apache.logging.log4j.Logger;

public class Checkpointer {
    //        'Measures time between hits. Requires the `from timeit import default_timer as timer`'
    double first, last;
    Logger log;

    public Checkpointer() {
        reset_now();
        log = null;
    }

    public Checkpointer(Logger log) {
        reset_now();
        this.log = log;
    }

    void print(String s) {
        if (log != null)
            log.info(s);
        else
           System.out.println(s);
    }

    public double timer() {
        return (double) (System.nanoTime() / 1000) / 1000000;
    }

    public void reset_now() {
        this.first = timer();
        this.last = this.first;
    }

    public double hit(String label) {

        double now = timer();
        double delta = now - this.last;
        if (label != null)
            print((!label.isEmpty() ? label : "Checkpoint") + ": " + String.format("%.3f", delta) + "s");
        this.last = now;
        return delta;
    }

    public double since_start(String label) {
        return since_start(label, false);
    }

    public double since_start(String label, boolean hit) {
        double now = timer();
        double delta = now - this.first;
        if (label != null)
            print((!label.isEmpty() ? label : "Total") + ": " + String.format("%.3f", delta) + "s");
        if (hit)
            this.last = now;
        return delta;
    }

}
