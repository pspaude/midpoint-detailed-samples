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

log.info("Entering " + operation + " Script.")

log.info("Using driver: {0} version: {1}",
        connection.getMetaData().getDriverName(),
        connection.getMetaData().getDriverVersion())

Sql sql = new Sql(connection)

//TODO Change to appropriate values for Production!!!!
def pidmThatExists = "123456"
def emailCode = "SOMECODDE"
def emailAddress = "some@example.edu"

def statementSqlExists = "SELECT gb_somef_exists(?, ?, ?) FROM dual"
def values = [
        Sql.INTEGER(pidmThatExists),
        Sql.VARCHAR(emailCode),
        Sql.VARCHAR(emailAddress)
]

log.info("Executing SQL: " + statementSqlExists + " with values " + values.toString())
def testResult = sql.firstRow(statementSqlExists, values) 

if (testResult) {
   log.info("Test Result= " + testResult[0])
   if (testResult[0].toString().contains("N")) {
       log.info("Warning Test Result was not found!")
	   return testResult
   }
}

log.info("Test Script Complete.")

