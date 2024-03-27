import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.StringUtil
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.*
import java.sql.Timestamp
import java.util.function.BiFunction
import java.util.function.Function
import java.util.stream.Collectors


def configuration = configuration as ScriptedSQLConfiguration
def operation = operation as OperationType
def objectClass = objectClass as ObjectClass
def log = log as Log

log.info(BaseScript.LOG_PREFIX + "Entering " + operation + " Script");

def sql = new Sql(connection)

switch (operation) {
    case OperationType.SYNC:
        def token = token as Object
        def handler = handler as SyncResultsHandler

        handleSync(sql, token, handler, log)
        break
    case OperationType.GET_LATEST_SYNC_TOKEN:
        return handleGetLatestSyncToken(sql, log)
}

void handleSync(Sql sql, Object tokenObject, SyncResultsHandler handler, def log) {
    String token = (String) tokenObject

    switch (objectClass) {
        case ObjectClass.ACCOUNT:
            handleSyncAccount(sql, token, handler, log)
            break
//        case BaseScript.GROUP_CLASS:
//            handleSyncGroup(sql, token, handler, log)
//            break
        default:
            throw new ConnectorException(BaseScript.LOG_PREFIX + "Unknown object class in SyncScript" + objectClass)
    }
}

//Begin Account/User Specific Methods
Object handleGetLatestSyncToken(Sql sql, def log) {
    switch (objectClass) {
        case ObjectClass.ACCOUNT:
            return handleSyncTokenForAccount(sql, log)
//        case BaseScript.GROUP_CLASS:
//            return handleSyncTokenForGroup(sql, log)
        default:
            throw new ConnectorException(BaseScript.LOG_PREFIX + "Unknown object class in SyncScript" + objectClass)
    }
}

private void handleSyncAccount(Sql sql, String token, SyncResultsHandler handler, def log) {
    String query = BaseScript.SYNC_ACCOUNT

    handleSyncGeneric(sql, token, handler, query,
            { t -> buildParamsFromTokenForAccount(t) },
            syncTokenRowTransformForAccount(),
            { sql1, result -> buildAccountObject(sql1, result, log) },
            ObjectClass.ACCOUNT,
            log
    )
}

private Object handleSyncTokenForAccount(Sql sql, def log) {
    return handleSyncTokenGeneric(sql, BaseScript.SYNC_TOKEN_ACCOUNT, syncTokenRowTransformForAccount(),log)
}

private Map buildParamsFromTokenForAccount(String token) {
    String[] array = token.split(";")

    return [
            (BaseScript.TIMESTAMP_VALUE) : Timestamp.valueOf(array[0]),
            (BaseScript.ID_VALUE) : array[1] as String
    ]
}
private Closure syncTokenRowTransformForAccount() {
    return { row ->
        [
                row.getProperty(BaseScript.TIMESTAMP_VALUE).toString(),
                row.getProperty(BaseScript.ID_VALUE)?.toString()
        ]
    }
}

private ConnectorObject buildAccountObject(Sql sql, GroovyRowResult rowResult, def log) {
    String query = BaseScript.QUERY_USER + " where " + BaseScript.USER_SYNC_PRIMARY_KEY + " = :" + BaseScript.ID_VALUE
    Map params = [(BaseScript.ID_VALUE) : rowResult.getProperty(BaseScript.ID_VALUE)]

    log.info(BaseScript.LOG_PREFIX + "Executing sync query to build account object {0} with params {1}", query, params)

    List<GroovyRowResult> rows = sql.rows(params, query, 0, 1);
    if (rows == null || rows.isEmpty()) {
        log.info(BaseScript.LOG_PREFIX + "Couldn't find account for specified account identifier {}", params)
        return null
    }

    def row = rows.get(0)
    def subjectId = row.getProperty(BaseScript.SUBJECT_ID_COLUMN)?.trim()
    def groupName = row.getProperty(BaseScript.GROUP_ID_COLUMN)?.trim()

    if (subjectId && groupName) {
        final ConnectorObject object = ICFObjectBuilder.co {
            objectClass ObjectClass.ACCOUNT
            uid subjectId as String
            id subjectId
            attribute 'grouperSubjectId', subjectId
            attribute 'grouperGroupMembershipNames', groupName
}

        return object
    }

        return null
    }
//End Account/User Specific Methods

//TODO if needed sync token for Groups add here

//Begin Generic Methods
private void handleSyncGeneric(Sql sql, String token, SyncResultsHandler handler, String query, Function<String, Map> buildParamsFromToken,
                               Function<GroovyRowResult, List<String>> buildTokenFromRow,
                               BiFunction<Sql, GroovyRowResult, ConnectorObject> buildConnectorObject, ObjectClass oc, def log) {
    if (token == null) {
        return
    }

    Map params = buildParamsFromToken.apply(token)

    int countProcessed = 0

    List<GroovyRowResult> results
    outer:
    //while (true) {
        log.info(BaseScript.LOG_PREFIX + "Executing handle sync generic query {0} with params {1}", query, params)

        sql.withTransaction {
            results = sql.rows(params, query, 1, BaseScript.SYNC_MAX_ROWS)
        }

        if (results == null || results.isEmpty()) {
            log.info(BaseScript.LOG_PREFIX + "Nothing found in queue")
            //break
        }

        log.info(BaseScript.LOG_PREFIX + "Starting to process {0} records", results.size())

        for (int i = 0; i < results.size(); i++) {
            GroovyRowResult result = results.get(i)

            ConnectorObject object = null
            String newToken = null
            sql.withTransaction {
                object = buildConnectorObject.apply(sql, result)
                newToken = buildSyncToken(result, buildTokenFromRow, log)
            }

            if (object == null) {
                continue
            }

            SyncDelta delta = buildSyncDelta(SyncDeltaType.CREATE_OR_UPDATE, oc, newToken, object, log)

            log.info(BaseScript.LOG_PREFIX + "Created sync delta for object {0} with token {1}, delta on TRACE level", object.getUid().getUidValue(), newToken)
            if (log.isOk()) {
                log.ok(BaseScript.LOG_PREFIX + "Delta {0}", delta)
            }

            if (!handler.handle(delta)) {
                log.info(BaseScript.LOG_PREFIX + "Handler paused processing")
                break outer
            }

            params = buildParamsFromToken.apply(newToken)

            countProcessed++
        }
    //}

    log.info(BaseScript.LOG_PREFIX + "Synchronization done, processed {0} events", countProcessed)
}

private Object handleSyncTokenGeneric(Sql sql, String query, Function<GroovyRowResult, List<String>> rowTransform) {
    String result = null

    sql.withTransaction {
        log.ok(BaseScript.LOG_PREFIX + "Executing get sync token generic query {0}", query)

        GroovyRowResult row = sql.firstRow(query)
        if (row == null) {
            row = sql.firstRow("select now() as " + BaseScript.TIMESTAMP_VALUE + ", 0 as " + BaseScript.ID_VALUE)
        }

        result = buildSyncToken(row, rowTransform, log)

        log.info(BaseScript.LOG_PREFIX + "Created token: {0}", result)
    }

    return result
}

private String buildSyncToken(Map row, Function<GroovyRowResult, List<String>> rowTransform, def log) {
    if (row == null) {
        return null
    }

    List<String> values = rowTransform.apply(row)
    if (values == null || values.isEmpty()) {
        return null
    }

    if (values.size() == 1) {
        result = values.get(0)
    }

    result = StringUtil.join(values, (char) ';')
}

private SyncDelta buildSyncDelta(SyncDeltaType type, ObjectClass oc, Object newToken, ConnectorObject obj, def log) {
    SyncDeltaBuilder builder = new SyncDeltaBuilder()
    builder.setDeltaType(type)
    builder.setObjectClass(oc)

    if (newToken != null) {
        builder.setToken(new SyncToken(newToken))
    }

    builder.setObject(obj)
    builder.setUid(obj.getUid())

    return builder.build()
}
//End Generic Methods
