package avecsafoi.ktx.mts.io.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TA {
    int[] z() default {0, 0};

    String f() default "yyyy-MM-dd";
}
