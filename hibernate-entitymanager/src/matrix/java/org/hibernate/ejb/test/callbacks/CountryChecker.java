//$Id$
package org.hibernate.ejb.test.callbacks;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.persistence.EntityListeners;

/**
 * @author Emmanuel Bernard
 */
@EntityListeners(CountryNameCheckerListener.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CountryChecker {
}
