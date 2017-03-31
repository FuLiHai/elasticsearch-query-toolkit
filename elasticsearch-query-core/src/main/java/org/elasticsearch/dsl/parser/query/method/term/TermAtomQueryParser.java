package org.elasticsearch.dsl.parser.query.method.term;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.dsl.exception.ElasticSql2DslException;
import org.elasticsearch.dsl.helper.ElasticSqlArgTransferHelper;
import org.elasticsearch.dsl.helper.ElasticSqlMethodInvokeHelper;
import org.elasticsearch.dsl.listener.ParseActionListener;
import org.elasticsearch.dsl.parser.query.method.AbstractFieldSpecificMethodQueryParser;
import org.elasticsearch.dsl.parser.query.method.MethodInvocation;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.util.List;
import java.util.Map;

public class TermAtomQueryParser extends AbstractFieldSpecificMethodQueryParser {

    private static List<String> TERM_QUERY_METHOD = ImmutableList.of("term", "term_query", "termQuery");

    public TermAtomQueryParser(ParseActionListener parseActionListener) {
        super(parseActionListener);
    }

    @Override
    public boolean isMatchMethodInvocation(MethodInvocation invocation) {
        return ElasticSqlMethodInvokeHelper.isMethodOf(TERM_QUERY_METHOD, invocation.getMatchQueryExpr().getMethodName());
    }

    @Override
    protected String getExtraParamString(MethodInvocation invocation) {
        SQLMethodInvokeExpr methodInvokeExpr = invocation.getMatchQueryExpr();
        if (methodInvokeExpr.getParameters().size() == 3) {
            SQLExpr extraParamExpr = methodInvokeExpr.getParameters().get(2);
            Object[] sqlArgs = invocation.getSqlArgs();
            return ElasticSqlArgTransferHelper.transferSqlArg(extraParamExpr, sqlArgs, false).toString();
        }
        return StringUtils.EMPTY;
    }

    @Override
    protected QueryBuilder buildQuery(MethodInvocation invocation, String fieldName, Map<String, String> extraParams) {
        SQLMethodInvokeExpr methodInvokeExpr = invocation.getMatchQueryExpr();

        SQLExpr textExpr = methodInvokeExpr.getParameters().get(1);
        Object text = ElasticSqlArgTransferHelper.transferSqlArg(textExpr, invocation.getSqlArgs(), false);

        TermQueryBuilder termQuery = QueryBuilders.termQuery(fieldName, text);
        setExtraMatchQueryParam(termQuery, extraParams);

        return termQuery;
    }

    @Override
    protected SQLExpr getFieldExpr(MethodInvocation invocation) {
        return invocation.getMatchQueryExpr().getParameters().get(0);
    }

    @Override
    protected void checkQueryMethod(MethodInvocation invocation) throws ElasticSql2DslException {
        if (!isMatchMethodInvocation(invocation)) {
            throw new ElasticSql2DslException(
                    String.format("[syntax error] Expected term query method name is [term],but get [%s]",
                            invocation.getMatchQueryExpr().getMethodName()));
        }

        SQLMethodInvokeExpr methodCallExpr = invocation.getMatchQueryExpr();

        int paramCount = methodCallExpr.getParameters().size();
        if (paramCount != 2 && paramCount != 3) {
            throw new ElasticSql2DslException(String.format("[syntax error] There's no %s args method: term", paramCount));
        }

        SQLExpr textExpr = methodCallExpr.getParameters().get(1);

        String text = ElasticSqlArgTransferHelper.transferSqlArg(textExpr, invocation.getSqlArgs(), false).toString();
        if (StringUtils.isEmpty(text)) {
            throw new ElasticSql2DslException("[syntax error] Term text can not be blank!");
        }
    }


    private void setExtraMatchQueryParam(TermQueryBuilder termQuery, Map<String, String> extraParamMap) {
        if (MapUtils.isEmpty(extraParamMap)) {
            return;
        }
        if (extraParamMap.containsKey("boost")) {
            String val = extraParamMap.get("boost");
            termQuery.boost(Float.valueOf(val));
        }
    }
}
