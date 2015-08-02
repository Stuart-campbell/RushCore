package co.uk.rushorm.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.exceptions.RushLimitRequiredForOffsetException;
import co.uk.rushorm.core.implementation.RushSqlUtils;
import co.uk.rushorm.core.search.RushOrderBy;
import co.uk.rushorm.core.search.RushWhere;
import co.uk.rushorm.core.search.RushWhereChild;
import co.uk.rushorm.core.search.RushWhereHasChild;
import co.uk.rushorm.core.search.RushWhereStatement;

/**
 * Created by Stuart on 14/12/14.
 */
public class RushSearch {

    private static final String WHERE_TEMPLATE = "SELECT * from %s %s %s %s %s %s %s;";
    private static final String COUNT_TEMPLATE = "SELECT COUNT(*) from %s %s %s;";

    private final List<RushWhere> whereStatements = new ArrayList<>();
    private final List<RushOrderBy> orderStatements = new ArrayList<>();
    private final List<String> groupBy = new ArrayList<>();

    private Integer limit;
    private Integer offset;

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

    public <T extends Rush> long count(Class<T> clazz) {
        return RushCore.getInstance().count(buildCountSql(clazz));
    }

    private String buildSql(Class<? extends Rush> clazz) {
        StringBuilder joinString = new StringBuilder();
        StringBuilder whereString = new StringBuilder();
        for(int i = 0; i < whereStatements.size(); i ++) {
            if(i < 1){
                whereString.append("\nWHERE ");
            }
            RushWhere where = whereStatements.get(i);
            whereString.append(where.getStatement(clazz, joinString));
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

        StringBuilder groupBy = new StringBuilder();
        for(int i = 0; i < this.groupBy.size(); i ++) {
            if(i < 1){
                order.append("\nGROUP BY ");
            }else if(i < this.groupBy.size() - 1){
                order.append(", ");
            }
            order.append(this.groupBy.get(i));
        }

        Map<Class<? extends Rush>, AnnotationCache> annotationCache = RushCore.getInstance().getAnnotationCache();
        return String.format(WHERE_TEMPLATE, annotationCache.get(clazz).getTableName(), joinString.toString(), whereString.toString(), order.toString(), groupBy.toString(), limit, offset);
    }

    private String buildCountSql(Class<? extends Rush> clazz) {
        StringBuilder joinString = new StringBuilder();
        StringBuilder whereString = new StringBuilder();
        for(int i = 0; i < whereStatements.size(); i ++) {
            if(i < 1){
                whereString.append("\nWHERE ");
            }
            RushWhere where = whereStatements.get(i);
            whereString.append(where.getStatement(clazz, joinString));
        }
        Map<Class<? extends Rush>, AnnotationCache> annotationCache = RushCore.getInstance().getAnnotationCache();
        return String.format(COUNT_TEMPLATE, annotationCache.get(clazz).getTableName(), joinString.toString(), whereString.toString());
    }

    public RushSearch whereId(String id) {
        return whereEqual(RushSqlUtils.RUSH_ID, id);
    }

    public RushSearch and(){
        whereStatements.add(new RushWhere(" AND "));
        return this;
    }

    public RushSearch or(){
        whereStatements.add(new RushWhere(" OR "));
        return this;
    }

    public RushSearch startGroup(){
        whereStatements.add(new RushWhere("("));
        return this;
    }

    public RushSearch endGroup(){
        whereStatements.add(new RushWhere(")"));
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
        whereStatements.add(new RushWhereHasChild(field, value.getId(), value.getClass(), "="));
        return this;
    }

    public RushSearch whereNotEqual(String field, Rush value) {
        whereStatements.add(new RushWhereHasChild(field, value.getId(), value.getClass(), "<>"));
        return this;
    }

    public RushSearch whereStartsWith(String field, String value) {
        return whereLike(field, value + "%");
    }

    public RushSearch whereEndsWith(String field, String value) {
        return whereLike(field, "%" + value);
    }

    public RushSearch whereContains(String field, String value) {
        return whereLike(field, "%" + value + "%");
    }

    public RushSearch whereLike(String field, String value) {
        return where(field, " LIKE ", RushCore.getInstance().sanitize(value));
    }

    public RushSearch whereChildOf(Rush value, String field) {
        return whereChildOf(value.getClass(), field, value.getId());
    }

    public RushSearch whereChildOf(Class<? extends Rush> clazz, String field, String id) {
        whereStatements.add(new RushWhereChild(field, id, clazz, "="));
        return this;
    }

    public RushSearch whereNotChildOf(Rush value, String field) {
        return whereNotChildOf(value.getClass(), field, value.getId());
    }

    public RushSearch whereNotChildOf(Class<? extends Rush> clazz, String field, String id) {
        whereStatements.add(new RushWhereChild(field, id, clazz, "<>"));
        return this;
    }

    private RushSearch where(String field, String modifier, String value) {
        whereStatements.add(new RushWhereStatement(field, modifier, value));
        return this;
    }

    public RushSearch orderDesc(String field){
        orderStatements.add(new RushOrderBy(field, "DESC"));
        return this;
    }

    public RushSearch orderAsc(String field){
        orderStatements.add(new RushOrderBy(field, "ASC"));
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

    public RushSearch whereIN(String column, List<String> values) {
        String inStatement = "(";
        for (String value : values) {
            inStatement += value + ",";
        }
        inStatement = inStatement.substring(0, inStatement.length() - 1) + ")";
        whereStatements.add(new RushWhereStatement(column, " IN ", inStatement));
        return this;
    }

    public List<RushWhere> getWhereStatements() {
        return whereStatements;
    }

    public List<RushOrderBy> getOrderStatements() {
        return orderStatements;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public RushSearch setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public RushSearch setOffset(Integer offset) {
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

    public RushSearch groupBy(String field) {
        groupBy.add(field);
        return this;
    }
}
