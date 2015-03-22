package co.uk.rushorm.core.search;

import co.uk.rushorm.core.Rush;

/**
 * Created by Stuart on 22/03/15.
 */
public class RushWhere  {
    private String element;

    public RushWhere(){}

    public RushWhere(String string){
        element = string;
    }
    public String getStatement(Class<? extends Rush> parentClazz, StringBuilder joinString){
        return element;
    }

    @Override
    public String toString() {
        return "{\"element\":\"" + element + "\"," +
                "\"type\":\"where\"}";
    }
}

