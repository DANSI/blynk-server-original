package cc.blynk.core.http.annotation;


import java.lang.annotation.*;

/**
 * This annotation is used to inject information into a class
 * field, bean property or classMethod parameter.
 *
 * @author Paul Sandoz
 * @author Marc Hadley
 * @see javax.ws.rs.ext.Providers
 * @since 1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Context {
}
