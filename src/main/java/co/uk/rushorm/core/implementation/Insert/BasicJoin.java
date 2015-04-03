package co.uk.rushorm.core.implementation.Insert;

import co.uk.rushorm.core.Rush;

/**
 * Created by Stuart on 03/04/15.
 */
public class BasicJoin {

    private final String table;
    private final Rush parent;
    private final Rush child;

    public BasicJoin(String table, Rush parent, Rush child) {
        this.table = table;
        this.parent = parent;
        this.child = child;
    }

    public String getTable() {
        return table;
    }

    public Rush getParent() {
        return parent;
    }

    public Rush getChild() {
        return child;
    }
}