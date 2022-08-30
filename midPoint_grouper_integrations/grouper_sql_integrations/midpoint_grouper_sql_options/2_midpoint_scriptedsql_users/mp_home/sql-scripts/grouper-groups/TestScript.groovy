import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import java.sql.Connection
import groovy.sql.Sql
import groovy.sql.DataSet;


def log = log as Log
def operation = operation as OperationType
def connection = connection as Connection
def configuration = configuration as ScriptedSQLConfiguration

log.info(BaseScript.LOG_PREFIX + "Entering " + operation + " Script.")

log.info("Using driver: {0} version: {1}",
        connection.getMetaData().getDriverName(),
        connection.getMetaData().getDriverVersion())

Sql sql = new Sql(connection)

sql.eachRow(BaseScript.QUERY_TEST, {})

log.info(BaseScript.LOG_PREFIX + "Test Script Complete.")
