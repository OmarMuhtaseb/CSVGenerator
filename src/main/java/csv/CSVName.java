package csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to give a preferable name to the column name for a specific field in the generated CSV
 * text.
 * In case of un-annotated field, the field name will be used in the CSV generated text
 *
 * @author Omar Muhtase
 * @since 2018-12-30
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CSVName {
    String value() default "";
}
