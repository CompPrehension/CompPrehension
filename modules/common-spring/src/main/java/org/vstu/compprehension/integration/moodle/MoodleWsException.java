package org.vstu.compprehension.integration.moodle;

/**
 * Сигнализирует о сбое вызова Moodle WS, который НЕ должен трактоваться как
 * «пустой результат»: транспортная ошибка, нераспарсиваемый ответ или
 * объект-исключение Moodle ({@code {"exception": ...}}). Используется там, где
 * caller обязан различать «WS отдал валидный (возможно пустой) результат» и
 * «вызов не удался» — например, чтобы не выполнять деструктивный diff ролей на
 * основании неудавшегося запроса.
 */
public class MoodleWsException extends RuntimeException {
    public MoodleWsException(String message) {
        super(message);
    }

    public MoodleWsException(String message, Throwable cause) {
        super(message, cause);
    }
}
