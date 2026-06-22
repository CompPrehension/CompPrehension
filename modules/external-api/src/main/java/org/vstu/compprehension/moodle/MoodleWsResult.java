package org.vstu.compprehension.moodle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Function;

/**
 * Результат вызова Moodle WS REST: {@link Success} с типизированной полезной нагрузкой
 * или {@link Failure} с причиной ошибки. Возвращается публичными методами {@link MoodleService};
 * политику на сбой (пустой результат / исключение) решает caller
 */
public sealed interface MoodleWsResult<T> permits MoodleWsResult.Success, MoodleWsResult.Failure {

    record Success<T>(T value) implements MoodleWsResult<T> {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Failure<T>(
            @JsonProperty("exception") String exception,
            @JsonProperty("errorcode") String errorcode,
            @JsonProperty("message") String message,
            @JsonProperty("debuginfo") String debuginfo,
            @JsonIgnore Throwable cause
    ) implements MoodleWsResult<T> {

        @SuppressWarnings("unchecked")
        static <T> Failure<T> fromMoodle(ObjectMapper mapper, JsonNode node) {
            return (Failure<T>) mapper.convertValue(node, Failure.class);
        }

        static <T> Failure<T> local(String errorcode, String message, Throwable cause) {
            return new Failure<>(null, errorcode, message, null, cause);
        }
    }

    /**
     * Значение при {@link Success}; при {@link Failure} — бросает {@link MoodleWsException}
     * (для caller'ов, которым сбой = исключительная ситуация, а не пустой результат).
     */
    default T orElseThrow() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new MoodleWsException(
                    String.format("%s: %s", f.errorcode(), f.message()), f.cause());
        };
    }

    default T orElseThrow(Function<Failure<?>, ? extends RuntimeException> errorBuilder) {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw errorBuilder.apply(f);
        };
    }
}
