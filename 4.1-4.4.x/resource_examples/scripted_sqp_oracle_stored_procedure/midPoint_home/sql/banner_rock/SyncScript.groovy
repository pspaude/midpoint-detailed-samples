import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.exceptions.ConnectorException


def configuration = configuration as ScriptedSQLConfiguration
def operation = operation as OperationType
def objectClass = objectClass as ObjectClass
def log = log as Log
def handler = handler as SyncResultsHandler
def token = token as Object

log.info("Entering " + operation + " Script");

def sql = new Sql(connection)

if (objectClass != ObjectClass.ACCOUNT) {
    //Future note if you're supporting Groups and Organizations you'd need to add a switch or something to handle them
    // separately below since each operation would be different SQL
    throw new ConnectorException("SyncScript only works for Users/Accounts! " + objectClass)
}

switch (operation) {
    case OperationType.SYNC:
        log.info("Performing SYNC Operation...")
        handleSync(sql, token, handler)
        break

    case OperationType.GET_LATEST_SYNC_TOKEN:
        log.info("Performing GET LATEST SYNC TOKEN Operation...")
        handleGetLatestSyncToken(sql)
        break

    default:
        log.info("In Default")
        throw new ConnectorException("Unknown operation type for SyncScript" + operation)

    log.info("Sync Script Complete.")
    return new SyncToken(1)
}


//Use examples from Evolveum if needed
void handleSync(Sql sql, Object tokenObject, SyncResultsHandler handler) {

}

void handleGetLatestSyncToken(Sql sql) {

}

