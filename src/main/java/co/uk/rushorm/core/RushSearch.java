package co.uk.rushorm.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import co.uk.rushorm.core.exceptions.RushLimitRequiredForOffsetException;
import co.uk.rushorm.core.implementation.ReflectionUtils;
import co.uk.rushorm.core.implementation.RushSqlUtils;

/**
 * Created by Stuart on 14/12/14.
 */
public class RushSearch {

    private static final String WHERE_TEMPLATE = "SELECT * from %s %s %s %s %s %s;";

    private static final String JOIN = "JOIN %s on (%s." + RushSqlUtils.RUSH_ID + "=%s.parent)";
    private static final String JOIN_CHILD = "JOIN %s on (%s." + RushSqlUtils.RUSH_ID + "=%s.child)";

    private final List<Where> whereStatements = new ArrayList<>();
    private final List<OrderBy> orderStatements = new ArrayList<>();

    private StringBuilder joinString = new StringBuilder();
    private Integer limit;
    private Integer offset;

    private class OrderBy {
        private final String field;
        private final String order;
        private OrderBy(String field, String order) {
            this.field = field;
            this.order = order;
        }

        private String getStatement(){
            return field + " " + order;
        }

        @Override
        public String toString() {
            return "{\"field\":\"" + field + "\"," +
                    "\"order\":\"" + order + "\"}";
        }
    }

    private class WhereHasChild extends Where {
        private final String field;
        private final String id;
        private final Class<? extends Rush> clazz;
        private final String modifier;

        private WhereHasChild(String field, String id, Class<? extends Rush> clazz, String modifier) {
            this.field = field;
            this.id = id;
            this.clazz = clazz;
            this.modifier = modifier;
        }

        protected String getStatement(Class<? extends Rush> parentClazz){
            String joinTable = ReflectionUtils.joinTableNameForClass(parentClazz, clazz, field, RushCore.getInstance().getAnnotationCache());
            String parentTable = ReflectionUtils.tableNameForClass(parentClazz, RushCore.getInstance().getAnnotationCache());
            joinString.append("\n").append(String.format(JOIN, joinTable, parentTable, joinTable));
            return joinTable + ".child" + modifier + "'" + id + "'";
        }

        @Override
        public String toString() {
            return "{\"field\":\"" + field + "\"," +
                    "\"modifier\":\"" + modifier + "\"," +
                    "\"id\":\"" + id + "\"," +
                    "\"class\":\"" + RushCore.getInstance().getAnnotationCache().get(clazz).getSerializationName() + "\"," +
                    "\"type\":\"whereParent\"}";
        }
    }

    private class WhereChild extends Where {
        private final String field;
        private final String id;
        private final Class<? extends Rush> clazz;
        private final String modifier;

        private WhereChild(String field, String id, Class<? extends Rush> clazz, String modifier) {
            this.field = field;
            this.id = id;
            this.clazz = clazz;
            this.modifier = modifier;
        }

        protected String getStatement(Class<? extends Rush> childClazz){
            String joinTable = ReflectionUtils.joinTableNameForClass(clazz, childClazz, field, RushCore.getInstance().getAnnotationCache());
            String parentTable = ReflectionUtils.tableNameForClass(childClazz, RushCore.getInstance().getAnnotationCache());
            joinString.append("\n").append(String.format(JOIN_CHILD, joinTable, parentTable, joinTable));
            return joinTable + ".parent" + modifier + "'" + id + "'";
        }

        @Override
        public String toString() {
            return "{\"field\":\"" + field + "\"," +
                    "\"modifier\":\"" + modifier + "\"," +
                    "\"id\":\"" + id + "\"," +
                    "\"class\":\"" + RushCore.getInstance().getAnnotationCache().get(clazz).getSerializationName() + "\"," +
                    "\"type\":\"whereChild\"}";
        }
    }

    private class WhereStatement extends Where {
        private final String field;
        private final String modifier;
        private final String value;

        private WhereStatement(String field, String modifier, String value) {
            super(field + modifier + value);
            this.field = field;
            this.modifier = modifier;
            this.value = value;
        }

        @Override
        public String toString() {
            return "{\"field\":\"" + field + "\"," +
                    "\"modifier\":\"" + modifier + "\"," +
                    "\"value\":\"" + value + "\"," +
                    "\"type\":\"whereStatement\"}";
        }
    }

    private class Where {
        private String element;
        private Where(){}
        private Where(String string){
            element = string;
        }
        protected String getStatement(Class<? extends Rush> parentClazz){
            return element;
        }

        @Override
        public String toString() {
            return "{\"element\":\"" + element + "\"," +
                    "\"type\":\"where\"}";
        }
    }

    public <T extends Rush> T findSingle(Class<T> clazz) {
        List<T> results = find(clazz);
        return results.size() > 0 ? results.get(0) : null;
    }

    public <T extends Rush> void find(Class<T> clazz, RushSearchCallback<T> callback) {
        RushCore.getInstance().load(clazz, buildSql(clazz), callback);
    }

    public <T extends Rush> List<T> find(Class<T> clazz) {
        return RushCore.getInstance().load(clazz, buildSql(clazz));
    }

    private String buildSql(Class<? extends Rush> clazz) {
        joinString = new StringBuilder();
        StringBuilder whereString = new StringBuilder();
        for(int i = 0; i < whereStatements.size(); i ++) {
            if(i < 1){
                whereString.append("\nWHERE ");
            }
            Where where = whereStatements.get(i);
            whereString.append(where.getStatement(clazz));
        }

        StringBuilder order = new StringBuilder();
        for(int i = 0; i < orderStatements.size(); i ++) {
            if(i < 1){
                order.append("\nORDER BY ");
            }else if(i < orderStatements.size() - 1){
                order.append(", ");
            }
            order.append(orderStatements.get(i).getStatement());
        }

        String limit = "";
        if(this.limit != null) {
            limit = "LIMIT " + Integer.toString(this.limit);
        }

        String offset = "";
        if(this.offset != null) {
            offset = "OFFSET " + Integer.toString(this.offset);
        }

        return String.format(WHERE_TEMPLATE, ReflectionUtils.tableNameForClass(clazz, RushCore.getInstance().getAnnotationCache()), joinString.toString(), whereString.toString(), order.toString(), limit, offset);
    }


    public RushSearch whereId(String id) {
        return whereEqual(RushSqlUtils.RUSH_ID, id);
    }

    public RushSearch and(){
        whereStatements.add(new Where(" AND "));
        return this;
    }

    public RushSearch or(){
        whereStatements.add(new Where(" OR "));
        return this;
    }

    public RushSearch startGroup(){
        whereStatements.add(new Where("("));
        return this;
    }

    public RushSearch endGroup(){
        whereStatements.add(new Where(")"));
        return this;
    }

    public RushSearch whereLessThan(String field, int value) {
        return where(field, "<", Integer.toString(value));
    }

    public RushSearch whereGreaterThan(String field, int value) {
        return where(field, ">", Integer.toString(value));
    }

    public RushSearch whereLessThan(String field, double value) {
        return where(field, "<", Double.toString(value));
    }

    public RushSearch whereGreaterThan(String field, double value) {
        return where(field, ">", Double.toString(value));
    }

    public RushSearch whereLessThan(String field, long value) {
        return where(field, "<", Long.toString(value));
    }

    public RushSearch whereGreaterThan(String field, long value) {
        return where(field, ">", Long.toString(value));
    }

    public RushSearch whereLessThan(String field, short value) {
        return where(field, "<", Short.toString(value));
    }

    public RushSearch whereGreaterThan(String field, short value) {
        return where(field, ">", Short.toString(value));
    }

    public RushSearch whereBefore(String field, Date date) {
        return whereLessThan(field, date.getTime());
    }

    public RushSearch whereAfter(String field, Date date) {
        return whereGreaterThan(field, date.getTime());
    }

    public RushSearch whereEqual(String field, String value) {
        return where(field, "=", RushCore.getInstance().sanitize(value));
    }

    public RushSearch whereEqual(String field, int value) {
        return where(field, "=", Integer.toString(value));
    }

    public RushSearch whereEqual(String field, long value) {
        return where(field, "=", Long.toString(value));
    }

    public RushSearch whereEqual(String field, double value) {
        return where(field, "=", Double.toString(value));
    }

    public RushSearch whereEqual(String field, short value) {
        return where(field, "=", Short.toString(value));
    }

    public RushSearch whereEqual(String field, boolean value) {
        return where(field, "=", "'" + Boolean.toString(value) + "'");
    }

    public RushSearch whereEqual(String field, Date date) {
        return whereEqual(field, date.getTime());
    }

    public RushSearch whereNotEqual(String field, String value) {
        return where(field, "<>", RushCore.getInstance().sanitize(value));
    }

    public RushSearch whereNotEqual(String field, int value) {
        return where(field, "<>", Integer.toString(value));
    }

    public RushSearch whereNotEqual(String field, long value) {
        return where(field, "<>", Long.toString(value));
    }

    public RushSearch whereNotEqual(String field, double value) {
        return where(field, "<>", Double.toString(value));
    }

    public RushSearch whereNotEqual(String field, short value) {
        return where(field, "<>", Short.toString(value));
    }

    public RushSearch whereNotEqual(String field, boolean value) {
        return where(field, "<>", "'" + Boolean.toString(value) + "'");
    }

    public RushSearch whereNotEqual(String field, Date date) {
        return whereNotEqual(field, date.getTime());
    }


    public RushSearch whereEqual(String field, Rush value) {
        whereStatements.add(new WhereHasChild(field, value.getId(), value.getClass(), "="));
        return this;
    }

    public RushSearch whereNotEqual(String field, Rush value) {
        whereStatements.add(new WhereHasChild(field, value.getId(), value.getClass(), "<>"));
        return this;
    }

    public RushSearch whereChildOf(Rush value, String field) {
        whereStatements.add(new WhereChild(field, value.getId(), value.getClass(), "="));
        return this;
    }

    public RushSearch whereNotChildOf(Rush value, String field) {
        whereStatements.add(new WhereChild(field, value.getId(), value.getClass(), "<>"));
        return this;
    }

    private RushSearch where(String field, String modifier, String value) {
        whereStatements.add(new WhereStatement(field, modifier, value));
        return this;
    }

    public RushSearch orderDesc(String field){
        orderStatements.add(new OrderBy(field, "DESC"));
        return this;
    }

    public RushSearch orderAsc(String field){
        orderStatements.add(new OrderBy(field, "ASC"));
        return this;
    }

    public RushSearch limit(int limit) {
        this.limit = limit;
        return this;
    }

    public RushSearch offset(int offset) {
        if(limit == null) {
            throw new RushLimitRequiredForOffsetException();
        }
        this.offset = offset;
        return this;
    }

    private static final String JSON_TEMPLATE = "{\"limit\":%s," +
            "\"offset\":%s," +
            "\"order\":[%s]," +
            "\"where\":[%s]}";

    @Override
    public String toString() {

        StringBuilder where = new StringBuilder();
        for(int i = 0; i < whereStatements.size(); i ++) {
            where.append(whereStatements.get(i).toString());
            if(i < whereStatements.size() - 1) {
                where.append(",");
            }
        }

        StringBuilder order = new StringBuilder();
        for(int i = 0; i < orderStatements.size(); i ++) {
            order.append(orderStatements.get(i).toString());
            if(i < orderStatements.size() - 1) {
                order.append(",");
            }
        }

        return String.format(JSON_TEMPLATE, limit, offset, order.toString(), where.toString());
    }
}
