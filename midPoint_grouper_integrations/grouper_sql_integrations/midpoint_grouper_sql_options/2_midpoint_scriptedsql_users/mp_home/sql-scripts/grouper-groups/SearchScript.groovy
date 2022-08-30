import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.common.Pair;
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ConnectorObject
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.ResultsHandler
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.Filter
import java.sql.Connection
import java.util.stream.Collectors
import static common.ScriptedSqlUtils.*


def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def configuration = configuration as ScriptedSQLConfiguration
def filter = filter as Filter
def connection = connection as Connection
def query = query as Closure
def handler = handler as ResultsHandler

final Class<?> UID_TYPE_ACCOUNT = String
final Class<?> UID_TYPE_GROUP = String
final Map userMembershipResults = [:]
final Map groupresults = [:]


log.info(BaseScript.LOG_PREFIX + "Entering " + operation + " Script")
def sql = new Sql(connection)

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        handleAccount(sql, userMembershipResults, UID_TYPE_ACCOUNT, log)
        break
    case BaseScript.GROUPER_GROUP_CLASS:
        handleGroup(sql, groupresults, UID_TYPE_GROUP, log)
        break
    default:
        throw new ConnectorException(BaseScript.LOG_PREFIX + "Unknown object class " + objectClass)
}

return new SearchResult()


void handleAccount(Sql sql, Map userResults, Class<?> UID_TYPE_ACCOUNT, def log) {
    final Map userQueryParams = [:]
    userResults.clear()

    String sqlQuery = BaseScript.QUERY_USER
    String where = buildWhereClause(filter, userQueryParams, BaseScript.SUBJECT_ID_COLUMN, BaseScript.SUBJECT_ID_COLUMN, BaseScript.PREFIX_MAPPER_USER, UID_TYPE_ACCOUNT, BaseScript.LOG_PREFIX)
    if (!where.isEmpty()) {
        sqlQuery += " where " + where
    }

    log.ok(BaseScript.LOG_PREFIX + "Running select users query ''{0}'', params ''{1}''", sqlQuery, userQueryParams)
    sql.withTransaction {
        String paging = buildPaging(options, userQueryParams, BaseScript.LOG_PREFIX)

        if (!paging.isEmpty()) {
            sqlQuery = sqlQuery + " " + paging
        }

        Closure filter = { row ->
            if (row?.getProperty(BaseScript.SUBJECT_ID_COLUMN)?.trim() && row?.getProperty(BaseScript.GROUP_ID_COLUMN)?.trim()) {
                userResults.computeIfAbsent(row.getProperty(BaseScript.SUBJECT_ID_COLUMN).trim(), k -> new HashSet<String>()).add(row.getProperty(BaseScript.GROUP_ID_COLUMN).trim())
            }
        }

        if (!userQueryParams.isEmpty()) {
            sql.eachRow(userQueryParams, sqlQuery, filter)
        } else {
            sql.eachRow(sqlQuery, filter)
        }
    }

    userResults.each { k,v ->
        final ConnectorObject object = ICFObjectBuilder.co {
            objectClass ObjectClass.ACCOUNT
            uid k as String
            id k
            attribute 'grouperSubjectId', k
            attribute 'grouperGroupMembershipNames', v.toList()
        }

        final Set<Attribute> attributes = object.getAttributes()
        final Set<Attribute> filtered = attributes.stream()
                .filter({ a -> !isAttributeEmpty(a) }).collect(Collectors.toSet())

        return handler.call(new ConnectorObject(object.getObjectClass(), filtered))
    }
}

void handleGroup(Sql sql, Map groupResults, Class<?> UID_TYPE_GROUP, def log) {
    Map groupQueryParams = [:]
    groupResults.clear()

    def sqlQuery = BaseScript.QUERY_GROUPS
    String where = buildWhereClause(filter, groupQueryParams, BaseScript.GROUP_ID_COLUMN, BaseScript.GROUP_ID_COLUMN, BaseScript.PREFIX_MAPPER_GROUP, UID_TYPE_GROUP, BaseScript.LOG_PREFIX)
    if (!where.isEmpty()) {
        sqlQuery += " where " + where
    }

    log.ok(BaseScript.LOG_PREFIX + "Running select groups query ''{0}'', params ''{1}''", sqlQuery, groupQueryParams)
    sql.withTransaction {
        String paging = buildPaging(options, groupQueryParams, BaseScript.LOG_PREFIX)

        if (!paging.isEmpty()) {
            sqlQuery = sqlQuery + " " + paging
        }

        Closure filter = { row ->
            if (row?.getProperty(BaseScript.GROUP_ID_COLUMN)?.trim()) {
                def id = row.getProperty(BaseScript.GROUP_ID_COLUMN).trim()
                do {
                    def parent = ""
                    if (id.contains(":")) {
                        parent = id.substring(0, id.lastIndexOf(":"))
                    }

                    groupResults.put(id.trim(), parent)
                    id = parent
                } while (id.trim())
            }
        }

        if (!groupQueryParams.isEmpty()) {
            sql.eachRow(groupQueryParams, sqlQuery, filter)

            if (BaseScript.shouldCreateGroupHierarchy && groupResults.isEmpty()) {
                //Group may have been created by theis connector from a parent relationship. The following checks if it exists and if so returns the created group.
                def id = groupQueryParams.get(BaseScript.UID_VALUE).toString()
                def count = sql?.firstRow(BaseScript.QUERY_GROUPS_COUNT_BEGIN + id + BaseScript.QUERY_GROUPS_COUNT_END)?.getProperty(BaseScript.COUNT_VALUE)

                if (count > 0) {
                    groupResults.put(id?.trim(), (id.contains(":") ? id.substring(0, id.lastIndexOf(":")) : ""))
                }
            }
        } else {
            sql.eachRow(sqlQuery, filter)
        }
    }

    groupResults.each { k,v ->
        final ConnectorObject object = ICFObjectBuilder.co {
            objectClass BaseScript.GROUPER_GROUP_CLASS
            uid k as String
            id k
            attribute 'groupId', k
            attribute 'groupParentId', v
        }

        final Set<Attribute> attributes = object.getAttributes()
        final Set<Attribute> filtered = attributes.stream()
                .filter({ a -> !isAttributeEmpty(a) }).collect(Collectors.toSet())

        return handler.call(new ConnectorObject(object.getObjectClass(), filtered))
    }
}
