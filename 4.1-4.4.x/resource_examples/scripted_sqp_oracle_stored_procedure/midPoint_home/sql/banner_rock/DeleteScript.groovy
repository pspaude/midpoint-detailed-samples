import groovy.sql.Sql
import org.identityconnectors.common.logging.Log
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.forgerock.openicf.misc.scriptedcommon.ScriptedConfiguration
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.exceptions.UnknownUidException


def operation = operation as OperationType
def configuration = configuration as ScriptedConfiguration
def log = log as Log
def objectClass = objectClass as ObjectClass
def options = options as OperationOptions
def uid = uid as Uid
def count = 0


log.info("Entering " + operation + " Script");
def sql = new Sql(connection);

if (uid == null) {
    throw new ConnectorException("Delete only works for Users that exist! Passed Uid is null.")
}

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.info("Deleting SOMETHING HERE [" + uid.getUidValue() + "]")
        //count = sql.call("PROCEDURE HERE", uid.getUidValue())
        break

    default:
        log.info("In Default")
        throw new UnsupportedOperationException("Warning " + operation.name() + " operation of type:" +
                objectClass.objectClassValue + " is not supported.")

    if (count != 1) {
        throw new UnknownUidException("Couldn't find and delete object $objectClass with uid $uid")
    } else {
        ObjectCacheLibrary.instance.delete(objectClass, uid)
    }

    log.info("Delete Script Complete.")
}

