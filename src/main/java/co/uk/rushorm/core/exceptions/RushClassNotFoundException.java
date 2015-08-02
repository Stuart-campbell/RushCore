package co.uk.rushorm.core.exceptions;

/**
 * Created by Stuart on 12/04/15.
 */
public class RushClassNotFoundException extends RuntimeException {

    public RushClassNotFoundException(Class clazz) {
        super("Rush class " + clazz.getSimpleName() + " was not found. Please make sure that if you are using setClasses on RushInitializeConfig you include this class.");
    }

}
