package co.uk.rushorm.core.implementation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushConfig;
import co.uk.rushorm.core.annotations.RushClassSerializationName;
import co.uk.rushorm.core.annotations.RushDisableAutodelete;
import co.uk.rushorm.core.annotations.RushIgnore;
import co.uk.rushorm.core.annotations.RushList;

/**
 * Created by Stuart on 01/02/15.
 */
public class RushAnnotationCache implements AnnotationCache {
    
    private final List<String> fieldToIgnore;
    private final List<String> disableAutoDelete;
    private final Map<String, Class<? extends Rush>> listsClasses;
    private final Map<String, Class<? extends List>> listsTypes;
    private final String serializationName;
    private final String tableName;
    
    public RushAnnotationCache(Class clazz, List<Field> fields, RushConfig rushConfig) {
        
        if(clazz.isAnnotationPresent(RushClassSerializationName.class)) {
            RushClassSerializationName rushClassSerializationName = (RushClassSerializationName) clazz.getAnnotation(RushClassSerializationName.class);
            serializationName = rushClassSerializationName.name();
        } else {
            serializationName = clazz.getSimpleName();
        }
        
        if(rushConfig.usingMySql()) {
            tableName = serializationName;
        }else {
            tableName = clazz.getName();
        }

        listsClasses = new HashMap<>();
        listsTypes = new HashMap<>();
        fieldToIgnore = new ArrayList<>();
        disableAutoDelete = new ArrayList<>();

        for(Field field : fields) {
            if(field.isAnnotationPresent(RushIgnore.class)
                    || Modifier.isFinal(field.getModifiers())) {
                fieldToIgnore.add(field.getName());
            } else {
                if (field.isAnnotationPresent(RushList.class)) {
                    RushList rushList = field.getAnnotation(RushList.class);
                    listsClasses.put(field.getName(), rushList.classType());
                    listsTypes.put(field.getName(), rushList.listType());
                }

                if (field.isAnnotationPresent(RushDisableAutodelete.class)) {
                    disableAutoDelete.add(field.getName());
                }
            }
        }
    }

    @Override
    public List<String> getFieldToIgnore() {
        return fieldToIgnore;
    }

    @Override
    public List<String> getDisableAutoDelete() {
        return disableAutoDelete;
    }

    @Override
    public Map<String, Class<? extends Rush>> getListsClasses() {
        return listsClasses;
    }

    @Override
    public Map<String, Class<? extends List>> getListsTypes() {
        return listsTypes;
    }

    @Override
    public String getSerializationName() {
        return serializationName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }
}
