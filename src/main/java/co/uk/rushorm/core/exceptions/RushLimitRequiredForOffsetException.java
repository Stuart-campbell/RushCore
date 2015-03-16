package co.uk.rushorm.core.exceptions;

/**
 * Created by Stuart on 16/03/15.
 */
public class RushLimitRequiredForOffsetException extends RuntimeException {

    public RushLimitRequiredForOffsetException() {
        super("You must set a limit before you can set an offset");
    }
}
