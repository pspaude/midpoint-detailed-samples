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
def createUpdateAttributes = new AttributesAccessor(attributes as Set<Attribute>)
def uid = uid as Uid
def configuration = configuration as ScriptedSQLConfiguration
def count = 0

log.info("Entering " + operation + " Script")

def sql = new Sql(connection)

switch (objectClass) {
	case ObjectClass.ACCOUNT:
		log.info("Creating or Updating user [" + uid.getUidValue() + "]...")
		return handleAccountEmailPossiblyExists(sql, uid.getUidValue(), createUpdateAttributes, count)
	default:
		log.info('In Default!')
		throw new ConnectorException("Warning this Resource only handles Accounts/Users! Unsupported objectClass " + objectClass)
}    

//Update Script if Emails Exist will use Create below if they don't
Uid handleAccountEmailPossiblyExists(def sql, def uid, def createUpdateAttributes, def count) {    
    
	log.ok("Update for pidm=[" + uid + "] with values: [" + createUpdateAttributes.toString() + "]!")
	
	def codeRowEmailMap = [:]
	def statementSqlQueryAll = "SELECT some_some_address, some_emal_code, rowid FROM some WHERE some_pidm = '" + uid + "'"
	
	log.ok("Executing SQL: " + statementSqlQueryAll)
	sql.eachRow(statementSqlQueryAll, { row ->
	    if ("BUV".equalsIgnoreCase(row.SOME_EMAL_CODE?.toString()?.trim())) {
		    if (createUpdateAttributes.hasAttribute("emailUniv1")) {
			    codeRowEmailMap.put("BUV", new Tuple2(row.ROWID?.toString()?.trim(), createUpdateAttributes.findString("emailUniv1").trim()))
			}
		
		} else if ("PER1".equalsIgnoreCase(row.SOME_EMAL_CODE?.toString()?.trim())) {					
			if (createUpdateAttributes.hasAttribute("emailPer1")) {
				codeRowEmailMap.put("PER1", new Tuple2(row.ROWID?.toString()?.trim(), createUpdateAttributes.findString("emailPer1").trim()))
			}
		
		} else if ("PER2".equalsIgnoreCase(row.SOME_EMAL_CODE?.toString()?.trim())) {
		    if (createUpdateAttributes.hasAttribute("emailPer2")) {
			    codeRowEmailMap.put("PER2", new Tuple2(row.ROWID?.toString()?.trim(), createUpdateAttributes.findString("emailPer2").trim()))
			}
			
		} else {
		    //Do Nothing
		}
 	}); 
	log.ok("Query All Result=" + codeRowEmailMap.toString())
	
	if (!codeRowEmailMap.isEmpty()) {
		def statementSqlUpdate = "call gb_some.p_update(?,?,?,?,?,?,?,?,?,?)"
		codeRowEmailMap.each{ k,v ->	
				def valuesUpdate = [
					Sql.INTEGER(uid),
					Sql.VARCHAR(k),
					Sql.VARCHAR(v.second),
					Sql.VARCHAR("A"),
					(k.equalsIgnoreCase("BUV")) ? Sql.VARCHAR("Y") : Sql.VARCHAR("N"),
					Sql.VARCHAR("SOME_VALUE"),
					Sql.VARCHAR(""),
					Sql.VARCHAR("Y"),
					null,
					Sql.VARCHAR(v.first)					
				]
				log.info("Executing Update SQL: " + statementSqlUpdate + " with values " + valuesUpdate.toString())
				def updateStatus = sql.call(statementSqlUpdate, valuesUpdate)
				log.ok("Update SQL Result: [" + updateStatus + "]")
		}
	}
	
	def createEmailMap = [:]
	if (createUpdateAttributes.hasAttribute("emailUniv1") && !codeRowEmailMap.containsKey("BUV")) {
		createEmailMap.put("BUV",createUpdateAttributes.findString("emailUniv1"))
	} else if (createUpdateAttributes.hasAttribute("emailPer1") && !codeRowEmailMap.containsKey("PER1")) {
	    createEmailMap.put("PER1",createUpdateAttributes.findString("emailPer1"))
	} else if (createUpdateAttributes.hasAttribute("emailPer2") && !codeRowEmailMap.containsKey("PER2")) {
	    createEmailMap.put("PER2",createUpdateAttributes.findString("emailPer2"))
	} else {
	   //do nothing
	}
	
	if (!createEmailMap.isEmpty()) {
		handleCreateEmail(uid, sql, createUpdateAttributes, count, createEmailMap)
	} 
	
	if (createUpdateAttributes.hasAttribute("externalUser")) {
	    handleCreateUpdateUsername(uid, sql, createUpdateAttributes)
	}
	
	//NOTE: Not checking result status because if there's no error it's always the same value! Error should throw exception and block update.
    log.info("Create/Update Script complete!")
    return new Uid(uid)
}
//End Update Emails Task


//Handles Creating emails if they don't exist
Uid handleCreateEmail(def id, Sql sql, def createAttributes, def count, def createEmailMap) {

    def statementSqlCreate = "call gb_some.p_create(?,?,?,?,?,?,?,?,?,?)"
	
	log.ok("Create Script for pidm=[" + id + "] with values: [" + createAttributes.toString() + "] for " + createEmailMap.toString() + "!")
	createEmailMap.each{k,v ->
	    def valuesCreate = [
            Sql.INTEGER(id),
			Sql.VARCHAR(k),
			Sql.VARCHAR(v),
			Sql.VARCHAR("A"),
			(k.equalsIgnoreCase("buv")) ? Sql.VARCHAR("Y") : Sql.VARCHAR("N"),
			Sql.VARCHAR("somevalue"),
            Sql.VARCHAR(""),
			Sql.VARCHAR("Y"),
			Sql.VARCHAR(""),
			Sql.VARCHAR
        ]
        log.info("Executing Create SQL: " + statementSqlCreate + " with values " + valuesCreate.toString())
        def createStatus = sql.call(statementSqlCreate, valuesCreate)
		log.ok("Create SQL Result: [" + createStatus + "]")
	
	}
}
//End Email Create


//Handles Username Update and Creation in Rock if external username does/does not exist
Uid handleCreateUpdateUsername(def id, Sql sql, def createAttributes) {

    def statementUsernameExists = "SELECT gb_some_access.f_exists(?,?) FROM dual"
    def values = [
        Sql.INTEGER(id),
        null
    ]

    log.info("Executing SQL: " + statementUsernameExists + " with values " + values.toString())
    def usernameResult = sql.firstRow(statementUsernameExists, values) 

    log.ok("external username result: " + usernameResult[0].toString())

    if (usernameResult[0].toString().trim().equalsIgnoreCase("Y")) {

	    def statementSqlUpdateUsername = "call gb_some_access.p_update(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
		def valuesUpdateUsername = [
				Sql.INTEGER(id),
				Sql.VARCHAR("N"),
				Sql.VARCHAR("N"),
				Sql.VARCHAR("some_login"),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				createAttributes.hasAttribute("externalUser") ? Sql.VARCHAR(createAttributes.findString("externalUser")) : Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR(""),
				Sql.VARCHAR
		]
		
		log.info("Executing Update Username SQL: " + statementSqlUpdateUsername + " with values " + valuesUpdateUsername.toString())
		def updateUsernameStatus = sql.call(statementSqlUpdateUsername, valuesUpdateUsername)
		log.ok("Update Username SQL Result: [" + updateUsernameStatus + "]")
		
	} else if (usernameResult[0].toString().trim().equalsIgnoreCase("N")) {
	
    	def statementSql3rdPartyCreate = "call gb_some_access.p_create(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
		def values3rdPartyCreate = [
			Sql.INTEGER(id),
			Sql.VARCHAR("N"),
			Sql.VARCHAR("N"),
			Sql.VARCHAR("some_value"),
			null,
			null,
			null,
			null,
			Sql.VARCHAR("value"),
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			createAttributes.hasAttribute("externalUser") ? Sql.VARCHAR(createAttributes.findString("externalUser")) : Sql.VARCHAR(""),
			Sql.VARCHAR
		]
		log.info("Executing Create SQL: " + statementSql3rdPartyCreate + " with values " + values3rdPartyCreate.toString())
		def thirdPartyCreateStatus = sql.call(statementSql3rdPartyCreate, values3rdPartyCreate)
		log.ok("Create SQL Result: [" + thirdPartyCreateStatus + "]")  

		def statementSqlProcPin = "SELECT gb_some_access.f_proc_pin(?) FROM dual"
		def valuesProcPin = [Sql.INTEGER(id)]
		log.info("Executing Create SQL: " + statementSqlProcPin + " with values " + valuesProcPin.toString())
		def procPinStatus = sql.firstRow(statementSqlProcPin, valuesProcPin)
		log.ok("Create SQL Result: [" + procPinStatus[0]?.toString() + "]")
	
	} else {
	    log.info("Couldn't set/update username. Username query result doesn't exist or doesn't match expected value.")
	}
}
