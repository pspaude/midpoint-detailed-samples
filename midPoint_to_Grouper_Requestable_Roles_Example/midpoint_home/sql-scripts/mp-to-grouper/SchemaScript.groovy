import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder
import org.identityconnectors.framework.spi.operations.SearchOp
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.MULTIVALUED


def log = log as Log
def operation = operation as OperationType
def builder = builder as ICFObjectBuilder
def configuration = configuration as ScriptedSQLConfiguration

log.info(BaseScript.LOG_PREFIX + "Entering " + operation + " Script.")

def schema = builder.schema({
    objectClass {
        type ObjectClass.ACCOUNT_NAME
        attributes {
            grouperSubjectId()
            grouperGroupMembershipNames String.class, MULTIVALUED
        }
    }

    objectClass {
        type BaseScript.GROUP_CLASS_NAME
        attributes {
            groupId()
        }
    }

    //defineOperationOption OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp
    //defineOperationOption OperationOptionInfoBuilder.buildPageSize(), SearchOp
})

log.info(BaseScript.LOG_PREFIX + "Schema Script Complete.")
return schema
