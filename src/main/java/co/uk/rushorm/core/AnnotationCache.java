package co.uk.rushorm.core;

import java.util.List;
import java.util.Map;

/**
 * Created by Stuart on 03/03/15.
 */
public interface AnnotationCache {
    public List<String> getFieldToIgnore();
    public List<String> getDisableAutoDelete();
    public Map<String, Class<? extends Rush>> getListsClasses();
    public Map<String, Class<? extends List>> getListsTypes();
    public String getSerializationName();
    public String getTableName();
    public boolean prefixTable();
}
