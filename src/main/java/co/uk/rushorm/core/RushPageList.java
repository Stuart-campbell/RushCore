package co.uk.rushorm.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Stuart on 12/04/15.
 */
public class RushPageList<T extends Rush> implements RushListField<T>, Iterable<T> {

    private static final int PAGE_SIZE = 100;

    private Class<T> clazz;
    private Class<? extends Rush> parentClazz;
    private String parentId;
    private String field;

    private RushSearch rushSearch;

    private List<T> loadedObjects;

    public RushPageList() {

    }

    public RushPageList(Class<T> clazz) {
        rushSearch = new RushSearch();
        this.clazz = clazz;
    }

    public RushPageList(RushSearch rushSearch, Class<T> clazz) {
        this.rushSearch = rushSearch;
        this.clazz = clazz;
    }

    public RushPageList(Rush parent, String fieldName, Class<T> clazz) {
        setDetails(parent, fieldName, clazz);
    }

    public RushSearch getRushSearch() {
        return rushSearch;
    }

    @Override
    public void setDetails(Rush parent, String fieldName, Class<T> clazz) {
        this.clazz = clazz;
        this.parentClazz = parent.getClass();
        this.parentId = parent.getId();
        if(this.parentId == null) {
            parent.save();
        }
        this.parentId = parent.getId();
        this.field = fieldName;

        rushSearch = new RushSearch().whereChildOf(parent, fieldName);
        rushSearch.setLimit(PAGE_SIZE);
        rushSearch.setOffset(0);
    }

    public int size() {
        return (int)rushSearch.count(clazz);
    }

    public boolean contains(Object rush) {
        return rush != null && Rush.class.isAssignableFrom(rush.getClass()) && new RushSearch().whereChildOf(parentClazz, field, parentId).whereId(((Rush) rush).getId()).findSingle(clazz) != null;
    }

    @Override
    public Iterator<T> iterator() {
        return new RushPageListIterator();
    }

    public boolean add(T rush) {
        List<T> objects = new ArrayList<>();
        objects.add(rush);
        return addAll(objects);
    }

    public boolean remove(T rush) {
        List<T> objects = new ArrayList<>();
        objects.add(rush);
        return removeAll(objects);
    }

    public boolean addAll(Collection collection) {
        List<T> objects = new ArrayList<>();
        for (Object object : collection) {
            T rush = (T) object;
            objects.add(rush);
        }
        RushCore.getInstance().save(objects);
        RushCore.getInstance().join(joins(collection));
        loadedObjects = null;
        return true;
    }

    public boolean removeAll(Collection collection) {
        RushCore.getInstance().deleteJoin(joins(collection));
        loadedObjects = null;
        return true;
    }

    private List<RushJoin> joins(Collection collection) {
        List<RushJoin> joins = new ArrayList<>();
        for (Object object : collection) {
            Rush rush = (Rush) object;
            joins.add(new RushJoin(parentClazz, parentId, field, rush));
        }
        return joins;
    }

    public void clear() {
        RushCore.getInstance().clearChildren(parentClazz, field, clazz, parentId);
    }

    public boolean containsAll(Collection collection) {
        for (Object object : collection) {
            Rush rush = (Rush) object;
            if (!contains(rush)) {
                return false;
            }
        }
        return true;
    }

    public T get(int index) {
        boolean requiresLoad = false;

        if(loadedObjects == null) {
            requiresLoad = true;
        }

        int min = rushSearch.getOffset();
        int localIndex = index - min;

        if(localIndex < 0 || localIndex >= rushSearch.getLimit()) {
            requiresLoad = true;
        }

        if(requiresLoad) {
            int remainder = index / rushSearch.getLimit();
            int offset = remainder * rushSearch.getLimit();
            rushSearch.setOffset(offset);
            loadedObjects = rushSearch.find(clazz);
        }
        int newIndex = index - rushSearch.getOffset();
        return loadedObjects.get(newIndex);
    }

    public List<T> subList(int fromIndex, int toIndex) {
        return new RushSearch().whereChildOf(parentClazz, field, parentId).offset(fromIndex).limit(toIndex - fromIndex).find(clazz);
    }

    public class RushPageListIterator implements Iterator<T> {

        private Integer count;
        private int place = 0;

        public boolean hasNext() {
            if(count == null) {
                count = size();
            }
            return place < count;
        }

        public T next() {
            T object = get(place);
            place++;
            return object;
        }

        @Override
        public void remove() {
            place --;
            RushPageList.this.remove(get(place));
            count = null;
        }
    }
}
