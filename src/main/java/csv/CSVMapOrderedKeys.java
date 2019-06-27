package csv;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation should used with java.util.Map fields only. It is used to give the order of the possible keys
 * that the map might have by setting `keys` attribute.
 * `includeNull` is used to
 *
 * @author Omar Muhtaseb
 * @since 2018-12-30
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CSVMapOrderedKeys {
    String[] keys();
    boolean includeNull() default true;
}
