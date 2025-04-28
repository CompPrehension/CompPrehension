package org.vstu.compprehension.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
@Slf4j
public class PythonLauncher implements DisposableBean {

    private Process process;

    @PostConstruct
    void start() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-u", "python/bkt_service.py");
            pb.redirectErrorStream(true);
            this.process = pb.start();

            // читаем stdout и логируем:
            new Thread(() -> {
                try (var reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    reader.lines().forEach(log::info);
                } catch (IOException ignored) {}
            }).start();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start python server", e);
        }
    }

    @Override public void destroy() {
        if (process != null && process.isAlive()) process.destroy();
    }
}
