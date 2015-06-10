package co.uk.rushorm.core.implementation.Insert;

import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushConfig;
import co.uk.rushorm.core.RushMetaData;
import co.uk.rushorm.core.RushSaveStatementGeneratorCallback;
import co.uk.rushorm.core.implementation.ReflectionUtils;
import co.uk.rushorm.core.implementation.RushSqlUtils;

/**
 * Created by Stuart on 03/04/15.
 */
public class SqlBulkInsertGenerator implements RushSqlInsertGenerator {

    private final RushConfig rushConfig;

    public SqlBulkInsertGenerator(RushConfig rushConfig) {
        this.rushConfig = rushConfig;
    }

    @Override
    public void createManyJoins(Map<String, List<BasicJoin>> joinValues, final RushSaveStatementGeneratorCallback saveCallback) {
        for (final Map.Entry<String, List<BasicJoin>> entry : joinValues.entrySet()) {
            final StringBuilder columnsString = new StringBuilder();
            final List<BasicJoin> values = entry.getValue();

            ReflectionUtils.doLoop(values.size(), ReflectionUtils.GROUP_SIZE, new ReflectionUtils.LoopCallBack() {
                @Override
                public void start() {
                    columnsString.delete(0, columnsString.length());
                }

                @Override
                public void actionAtIndex(int index) {
                    columnsString.append("('")
                            .append(values.get(index).getParent().getId())
                            .append("','")
                            .append(values.get(index).getChild().getId())
                            .append("')");
                }

                @Override
                public void join() {
                    columnsString.append(", ");
                }

                @Override
                public void doAction() {
                    String sql = String.format(RushSqlUtils.MULTIPLE_INSERT_JOIN_TEMPLATE, entry.getKey(),
                            columnsString.toString());
                    saveCallback.createdOrUpdateStatement(sql);
                }
            });
        }
    }

    @Override
    public void createOrUpdateObjects(Map<Class<? extends Rush>, List<BasicUpdate>> valuesMap, Map<Class<? extends Rush>, List<String>> columnsMap, final Map<Class<? extends Rush>, AnnotationCache> annotationCache, final RushSaveStatementGeneratorCallback saveCallback) {
        for (final Map.Entry<Class<? extends Rush>, List<BasicUpdate>> entry : valuesMap.entrySet()) {

            StringBuilder columnsBuilder = new StringBuilder();
            columnsBuilder.append(RushSqlUtils.RUSH_ID)
                    .append(",")
                    .append(RushSqlUtils.RUSH_CREATED)
                    .append(",")
                    .append(RushSqlUtils.RUSH_UPDATED)
                    .append(",")
                    .append(RushSqlUtils.RUSH_VERSION)
                    .append(commaSeparated(columnsMap.get(entry.getKey())));

            final String columns = columnsBuilder.toString();

            final StringBuilder valuesString = new StringBuilder();
            final List<BasicUpdate> creates = entry.getValue();

            ReflectionUtils.doLoop(creates.size(), ReflectionUtils.GROUP_SIZE, new ReflectionUtils.LoopCallBack() {
                @Override
                public void start() {
                    valuesString.delete(0, valuesString.length());
                }

                @Override
                public void actionAtIndex(int index) {

                    RushMetaData rushMetaData = creates.get(index).rushMetaData;
                    rushMetaData.save();

                    valuesString.append("('")
                            .append(rushMetaData.getId())
                            .append("',")
                            .append(rushMetaData.getCreated())
                            .append(",")
                            .append(rushMetaData.getUpdated())
                            .append(",")
                            .append(rushMetaData.getVersion())
                            .append(commaSeparated(creates.get(index).values))
                            .append(")");
                }

                @Override
                public void join() {
                    valuesString.append(", ");
                }

                @Override
                public void doAction() {
                    String sql = String.format(rushConfig.usingMySql() ? RushSqlUtils.MULTIPLE_INSERT_UPDATE_TEMPLATE_MYSQL : RushSqlUtils.MULTIPLE_INSERT_UPDATE_TEMPLATE_SQLITE,
                            annotationCache.get(entry.getKey()).getTableName(),
                            columns,
                            valuesString.toString());

                    saveCallback.createdOrUpdateStatement(sql);
                }
            });
        }
    }

    private String commaSeparated(List<String> values) {
        StringBuilder string = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            string.append(",")
                    .append(values.get(i));
        }
        return string.toString();
    }
}
