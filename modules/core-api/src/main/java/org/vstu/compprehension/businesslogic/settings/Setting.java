package org.vstu.compprehension.businesslogic.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Подпись настройки или варианта её значения в форме упражнения. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD})
public @interface Setting {
    String ru();

    String en();

    int min() default Integer.MIN_VALUE;

    int max() default Integer.MAX_VALUE;
}
