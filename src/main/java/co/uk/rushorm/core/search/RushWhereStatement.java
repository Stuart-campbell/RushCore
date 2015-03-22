package co.uk.rushorm.core.search;

/**
 * Created by Stuart on 22/03/15.
 */
public class RushWhereStatement extends RushWhere {

    private final String field;
    private final String modifier;
    private final String value;

    public RushWhereStatement(String field, String modifier, String value) {
        super(field + modifier + value);
        this.field = field;
        this.modifier = modifier;
        this.value = value;
    }

    @Override
    public String toString() {
        return "{\"field\":\"" + field + "\"," +
                "\"modifier\":\"" + modifier + "\"," +
                "\"value\":\"" + value + "\"," +
                "\"type\":\"whereStatement\"}";
    }
}
