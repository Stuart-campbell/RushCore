package co.uk.rushorm.core.implementation;

import co.uk.rushorm.core.RushColumn;
import co.uk.rushorm.core.RushStringSanitizer;

/**
 * Created by Stuart on 03/03/15.
 */
public class RushColumnBooleanNumerical implements RushColumn<Boolean> {
    @Override
    public String sqlColumnType() {
        return "boolean";
    }

    @Override
    public String serialize(Boolean object, RushStringSanitizer stringSanitizer) {
        return object ? "1" : "0";
    }

    @Override
    public Boolean deserialize(String value) {
        if(value.equals("1") || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Class[] classesColumnSupports() {
        return new Class[]{Boolean.class, boolean.class};
    }
}
