package co.uk.rushorm.core;


/**
 * Created by Stuart on 12/04/15.
 */
public class RushJoin {

    private final Class<? extends Rush> parent;
    private final String parentId;
    private final String field;
    private final Rush child;

    public RushJoin(Class<? extends Rush> parent, String parentId, String field, Rush child) {
        this.parent = parent;
        this.parentId = parentId;
        this.field = field;
        this.child = child;
    }

    public Class<? extends Rush> getParent() {
        return parent;
    }

    public String getParentId() {
        return parentId;
    }

    public String getField() {
        return field;
    }
    public Rush getChild() {
        return child;
    }
}
