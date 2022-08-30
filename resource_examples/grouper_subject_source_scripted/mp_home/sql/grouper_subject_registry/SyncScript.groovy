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

log.info("Grouper Subject Registry: Entering " + operation + " Script");

def sql = new Sql(connection)

if (objectClass != ObjectClass.ACCOUNT) {
    //Future note if you're supporting Groups and Organizations you'd need to add a switch or something to handle them
    // separately below since each operation would be different SQL
    throw new ConnectorException("Grouper Subject Registry SyncScript only works for Users/Accounts! " + objectClass)
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

    log.info("Grouper Subject Registry: Sync Script Complete.")
    return new SyncToken(1)
}

void handleSync(Sql sql, Object tokenObject, SyncResultsHandler handler) {
    Map params = [:]
    def tstamp = null

    if (tokenObject != null){
        tstamp = new java.sql.Timestamp(tokenObject)
    } else {
        tstamp = new java.sql.Timestamp(new Date().time)
    }

    //TODO add additional personal identifiers below!
    def extraPersonalIds = ["exampleId"]

    def select = "SELECT gsr.id, gsr.first_name, gsr.last_name, gsr.email_address, gsr.description, gsr.login_id"
    def from = " FROM subject.PERSON gsr LEFT JOIN  subject.PERSON_IDENTIFIERS gsrpids ON gsr.id = gsrpids.person_id"
    def where = " WHERE date_trunc('milliseconds',gsr.updated) > ${tstamp} AND gsrpids.source IN (:srcIds)"

    extraPersonalIds.each {
        select = select +  ", gsrpids.value AS " + it;
    }

    select = select + ", date_trunc('milliseconds', gsr.updated) AS gsr.updated" + from

    log.info("Running Sync Query: [" + select + "] where [" + where + "] params [" + params + "]...")
    sql.eachRow(params, select + where, { row ->
        handler {
            syncToken row.updated.getTime()
            CREATE_OR_UPDATE()
            object {
                uid row.id as String
                id row.id as String
                attribute 'id', row.id
                attribute 'firstname', row.first_name
                attribute 'lastname', row.last_name
                attribute 'email', row.email_address
                attribute 'description', row.description
                attribute 'loginid', row.login_id
                //TODO add additional personal identifiers below!
                attribute 'exampleid', row.exampleId
            }
        }
    })
}

void handleGetLatestSyncToken(Sql sql) {
    sql.firstRow("SELECT date_trunc('milliseconds', gsr.updated) as gsr.updated" +
            " FROM subject.PERSON gsr ORDER BY gsr.updated DESC;",
    { row ->
        handler {
            syncToken row.updated.getTime()
        }
    })
}
