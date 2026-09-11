package org.vstu.compprehension.moodle;

/**
 * Фабрика {@link MoodleClient}.
 */
public interface MoodleClientFactory {
    MoodleClient create(String baseUrl, String wsToken);
}
