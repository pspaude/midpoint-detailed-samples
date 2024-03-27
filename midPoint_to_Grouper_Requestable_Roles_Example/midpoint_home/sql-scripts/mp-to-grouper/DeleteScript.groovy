import common.ScriptedSqlUtils
import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.*
import java.sql.Connection


def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def uid = uid as Uid
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def schema = schema as Schema

log.info(BaseScript.LOG_PREFIX + "Entering " + operation + " Script")

def sql = new Sql(connection)

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        return handleAccountDelete(sql)
    case BaseScript.GROUP_CLASS:
        return handleEntitlementDelete(sql)
    default:
        throw new ConnectorException("Unknown object class in DeleteScript with value " + objectClass)
}

void handleAccountDelete(Sql sql) {
    sql.withTransaction {
        ScriptedSqlUtils.buildAndExecuteDeleteQuery(sql, BaseScript.USER_TABLE, [subject_id: uid.getUidValue() as String], BaseScript.LOG_PREFIX)
    }
}

void handleEntitlementDelete(Sql sql) {
    sql.withTransaction {
        ScriptedSqlUtils.buildAndExecuteDeleteQuery(sql, BaseScript.USER_TABLE, [group_name: uid.getUidValue()], BaseScript.LOG_PREFIX)
    }
}
