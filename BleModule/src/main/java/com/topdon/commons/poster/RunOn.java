package com.topdon.commons.poster;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * <p>
 * date: 2019/8/2 23:53
 * author: chuanfeng.bi
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RunOn {
    /**
     *
     */
    ThreadMode value() default ThreadMode.UNSPECIFIED;
}
