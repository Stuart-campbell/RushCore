package co.uk.rushorm.core.implementation.Insert;

import java.util.List;

import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushMetaData;

/**
 * Created by Stuart on 03/04/15.
 */
public class BasicUpdate {

    protected final List<String> values;
    protected final Rush object;
    protected final RushMetaData rushMetaData;

    public BasicUpdate(List<String> values, Rush object, RushMetaData rushMetaData) {
        this.values = values;
        this.object = object;
        this.rushMetaData = rushMetaData;
    }

    public List<String> getValues() {
        return values;
    }

    public Rush getObject() {
        return object;
    }

    public RushMetaData getRushMetaData() {
        return rushMetaData;
    }
}
