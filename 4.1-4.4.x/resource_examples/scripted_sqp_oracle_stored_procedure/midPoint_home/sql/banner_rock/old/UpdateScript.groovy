import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.Uid
import org.identityconnectors.framework.common.objects.AttributesAccessor
import oracle.jdbc.driver.OracleTypes;
import groovy.sql.OutParameter;
import java.sql.ResultSet;

def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def updateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def uid = uid as Uid
def configuration = configuration as ScriptedSQLConfiguration
def count = 0

log.info("Entering " + operation + " Script")

def sql = new Sql(connection)

switch (operation) {
    case OperationType.UPDATE:
        switch (objectClass) {
            case ObjectClass.ACCOUNT:
                log.info("Updating user [" + uid.getUidValue() + "]...")
                return handleAccount(sql, uid.getUidValue(), updateAttributes, count)
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
    
	log.ok("Rock Update for pidm=[" + uid + "] with values: [" + updateAttributes.toString() + "]!")
	
	def rowNumberToCodeMap = [:]
	def statementSqlQueryAll = "SELECT something_email_address, something_emal_code, rowid FROM something WHERE something_pidm = '" + uid + "'"
	
	log.ok("Executing SQL: " + statementSqlQueryAll)
	sql.eachRow(statementSqlQueryAll, { row ->
        if ("BUV".equalsIgnoreCase(row.SOME_EMAL_CODE?.toString()?.trim()) && updateAttributes.hasAttribute("emailUniv1")) {
			rowNumberToCodeMap.put(row.ROWID?.toString()?.trim(), new Tuple2("BUV", updateAttributes.findString("emailUniv1").trim()))
			
		} else if ("PUV1".equalsIgnoreCase(row.SOME_EMAL_CODE?.toString()?.trim()) && updateAttributes.hasAttribute("emailPer1")) {
		    rowNumberToCodeMap.put(row.ROWID?.toString()?.trim(), new Tuple2("PUV1", updateAttributes.findString("emailPer1").trim()))
			
		} else if ("PUV2".equalsIgnoreCase(row.SOME_EMAL_CODE?.toString()?.trim()) && updateAttributes.hasAttribute("emailPer2")) {
		    rowNumberToCodeMap.put(row.ROWID?.toString()?.trim(), new Tuple2("PUV2", updateAttributes.findString("emailPer2").trim()))
			
		} else {
		    log.ok("No email code match for: " + row.SOME_EMAL_CODE + ". Doing nothing!")
		}
 	}); 
	log.ok("Query All Result=" + rowNumberToCodeMap.toString())
	
	if (!rowNumberToCodeMap.isEmpty()) {
		def statementSqlUpdate = "call gb_something.p_update(?,?,?,?,?,?,?,?,?,?)"
		rowNumberToCodeMap.each{ k,v ->	
				def valuesUpdate = [
					Sql.INTEGER(uid),
					Sql.VARCHAR(v.first),
					Sql.VARCHAR(v.second),
					Sql.VARCHAR("A"),
					Sql.VARCHAR("Y"),
					Sql.VARCHAR("SOME_VALUE"),
					Sql.VARCHAR(""),
					Sql.VARCHAR("Y"),
					null,
					Sql.VARCHAR(k)					
				]
				log.info("Executing Update SQL: " + statementSqlUpdate + " with values " + valuesUpdate.toString())
				def updateStatus = sql.call(statementSqlUpdate, valuesUpdate)
				log.ok("Update SQL Result: [" + updateStatus + "]")
		}
	} else {
        log.error("No row number returned, Update query can't be completed!")
		return null
	}
	
	//NOTE: Not checking result status because if there's no error it's always the same value! Error should throw exception and block update.
    log.info("Update Script complete!")
    return new Uid(uid)
}
