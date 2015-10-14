package co.uk.rushorm.core;

/**
 * Created by Stuart on 12/04/15.
 */
public interface RushListField<T extends Rush> {
    public void setDetails(Rush parent, String parentId, String fieldName, Class<T> clazz);
}
