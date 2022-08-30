import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid


def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def updateAttributes = attributes
def uid = uid as Uid
def configuration = configuration as ScriptedSQLConfiguration
def count = 0

log.info("Grouper Subject Registry: Entering " + operation + " Script")

def sql = new Sql(connection)

switch (operation) {
    case OperationType.UPDATE:
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                log.info("Updating user [" + uid.getUidValue() + "] with [" + updateAttributes.size() + "] attributes...")
                return handleAccount(sql, uid, updateAttributes, count)
            default:
                log.info('In Default!')
                throw new ConnectorException("Warning this Resource only handles Accounts/Users! Unsupported objectClass " + objectClass)
        }
    case OperationType.ADD_ATTRIBUTE_VALUES:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
    case OperationType.REMOVE_ATTRIBUTE_VALUES:
        throw new UnsupportedOperationException(operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")
    default:
        throw new ConnectorException("UpdateScript can not handle operation:" + operation.name())
}

Uid handleAccount(def sql, def uid, def updateAttributes, def count) {
    if (uid != null) {
        def statement = "UPDATE subject.PERSON SET "
        def where = " WHERE id= ?"
        def extraIdStatement = "UPDATE subject.PERSON_IDENTIFIERS SET value='"
        def extraIdWhere = " WHERE person_id= ? AND source= ?"
        def columnMap = ["id":"id", "firstname":"first_name", "lastname":"last_name", "email":"email_address",
                         "description":"description", "loginid":"login_id"] //maps schema definition to actual column names
        //TODO add additional personal identifiers below
        def extraPersonalIds = ["exampleid":"exampleId"]

        //Begin Load SET Columns
        def attrStatement = ""
        for (final Attribute attr : updateAttributes) {

            if (columnMap.containsKey(attr.getName())) {
                if (attrStatement.size() > 0) {
                    attrStatement = attrStatement + ", "
                }
                attrStatement = attrStatement + columnMap.get(attr.getName()) + "='" + ((String) attr.getValue().get(0)) + "'"
                break
            }

            if (extraPersonalIds.containsKey(attr.getName())) {
                def extraIdSql = extraIdStatement + ((String) attr.getValue().get(0)) + "'" + extraIdWhere
                def extraIdParams = [uid.getUidValue(), extraPersonalIds.get(attr.getName())]
                log.info("Executing UPDATE PERSON_IDENTIFIERS: " + extraIdSql + " with param " + extraIdParams.toString())
                sql.execute(extraIdSql, extraIdParams)
                count = sql.updateCount
            }
        }
        //End SET Columns Attributes

        if (attrStatement.size() > 0) {
            log.info("Executing UPDATE PERSON: " + statement + attrStatement + where + " with param " + uid.getUidValue())
            sql.execute(statement + attrStatement + where, uid.getUidValue())
            count = sql.updateCount

        }
    }

    if (count < 1) {
        throw new ConnectorException("UpdateScript didn't complete for uid: " + uid.getUidValue())
    } else {
        log.info("Grouper Subject Registry: Update Script complete!")
        return uid
    }
}
