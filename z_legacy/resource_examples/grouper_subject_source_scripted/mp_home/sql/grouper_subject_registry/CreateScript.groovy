import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.AttributesAccessor
import org.identityconnectors.framework.common.exceptions.UnknownUidException
import java.sql.Connection

def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def createAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def connection = connection as Connection
def id = id as String
def configuration = configuration as ScriptedSQLConfiguration
def count = 0;

log.info("Grouper Subject Registry: Entering " + operation + " Script")

def sql = new Sql(connection)

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.info("Creating Account/Users...")
        return handleAccount(sql, createAttributes, count)
    default:
        log.info('Using Default!')
        throw new ConnectorException("Unknown object class " + objectClass)
}

Uid handleAccount(Sql sql, def createAttributes, def count) {
    def statementSql = "INSERT INTO subject.PERSON (id, first_name, last_name, email_address, description, login_id) " +
    "VALUES (?,?,?,?,?,?)"

    def values = [
            id,
            createAttributes.hasAttribute("firstname") ? createAttributes.findString("firstname") : "",
            createAttributes.hasAttribute("lastname") ? createAttributes.findString("lastname") : "",
            createAttributes.hasAttribute("email") ? createAttributes.findString("email") : "",
            createAttributes.hasAttribute("description") ? createAttributes.findString("description") : "",
            createAttributes.hasAttribute("loginid") ? createAttributes.findString("loginid") : ""
    ]
    log.info("Executing SQL: " + statementSql + " with values " + values.toString())

    log.info("Grouper Subject Registry: Executing insert/create user")
    def keys = sql.executeInsert(statementSql, values)


    //Extra Personal Attributes Section
    def statementForIds = "INSERT INTO subject.PERSON_IDENTIFIERS (person_id, source, value) " +
            "VALUES (?, ?, ?)"
    //TODO add additional personal identifiers below
    def extraPersonalIds = ["exampleId"]
    extraPersonalIds.each { extraId ->
        if (createAttributes.hasAttribute(extraId)) {
            def valuesForIds = [
                    id,
                    extraId,
                    createAttributes.findString(extraId)
            ]

            log.info("Executing SQL for Ids: " + statementForIds + " with values " + valuesForIds.toString())
            log.info("Grouper Subject Registry: Inserting extra attributes...")
            sql.execute(statementForIds, valuesForIds)
            count = sql.updateCount
        }
    }
    //End Extra Personal Attributes Handling

    if (count < 1) {
        throw new UnknownUidException("Couldn't create object with " + keys[0][0])
    } else {
        log.info("Grouper Subject Registry: Create Script complete!")
        return new Uid(keys[0][0])
    }
}
