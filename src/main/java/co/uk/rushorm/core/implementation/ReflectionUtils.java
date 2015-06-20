package co.uk.rushorm.core.implementation;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushStatementGeneratorCallback;


/**
 * Created by Stuart on 11/12/14.
 */
public class ReflectionUtils {

    public static final int GROUP_SIZE = 499;

    public static String tableNameForClass(String name) {
        return RushSqlUtils.RUSH_TABLE_PREFIX + name.replace(".", "_").replace("$", "_");
    }

    public static String joinTableNameForClass(String parentName, String childName, String fieldName) {
        return parentName + "_" + childName + "_" + fieldName;
    }

    public static void getAllFields(List<Field> fields, Class<?> type) {
        if (type.getSuperclass() != null) {
            fields.addAll(Arrays.asList(type.getDeclaredFields()));
            getAllFields(fields, type.getSuperclass());
        }
    }

    public interface LoopCallBack {
        public void start();
        public void actionAtIndex(int index);
        public void join();
        public void doAction();
    }

    public static void doLoop(int max, int interval, LoopCallBack callBack) {
        callBack.start();
        for (int i = 0; i < max; i ++) {
            callBack.actionAtIndex(i);
            if(i > 0 && i % interval == 0) {
                callBack.doAction();
                callBack.start();
            } else if(i < max - 1) {
                callBack.join();
            }
        }
        if(max == 1 || (max - 1) % interval != 0) {
            callBack.doAction();
        }
    }

    public static void deleteManyJoins(Map<String, List<String>> joinDeletes, final RushStatementGeneratorCallback callback) {

        for (final Map.Entry<String, List<String>> entry : joinDeletes.entrySet()) {
            final StringBuilder columnsString = new StringBuilder();

            final List<String> ids = entry.getValue();

            doLoop(ids.size(), GROUP_SIZE, new LoopCallBack() {
                @Override
                public void start() {
                    columnsString.delete(0, columnsString.length());
                }

                @Override
                public void actionAtIndex(int index) {
                    columnsString.append("parent='")
                            .append(ids.get(index))
                            .append("'");
                }

                @Override
                public void join() {
                    columnsString.append(" OR ");
                }

                @Override
                public void doAction() {
                    String sql = String.format(RushSqlUtils.MULTIPLE_DELETE_TEMPLATE, entry.getKey(),
                            columnsString.toString());

                    callback.deleteStatement(sql);
                }
            });
        }
    }
}
