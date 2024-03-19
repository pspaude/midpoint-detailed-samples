package old

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

log.info("Entering " + operation + " Script")

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

    def statementSqlCreate = "call gb_someemail.p_create(?,?,?,?,?,?,?,?,?,?)"
	
	log.ok("Create Script for pidm=[" + id + "] with values: [" + createAttributes.toString() + "]!")
	def emailToCodeMap = [:]
	if (createAttributes.hasAttribute("emailUniv1")) {
		emailToCodeMap.put("BUV",createAttributes.findString("emailUniv1"))
	} else if (createAttributes.hasAttribute("emailPer1")) {
	    emailToCodeMap.put("PUV",createAttributes.findString("emailPer1"))
	} else if (createAttributes.hasAttribute("emailPer2")) {
	    emailToCodeMap.put("PUV2",createAttributes.findString("emailPer2"))
	} else {
	   //do nothing
	}
	
	emailToCodeMap.each{k,v ->
	    def valuesCreate = [
            Sql.INTEGER(id),
			Sql.VARCHAR(k),
			Sql.VARCHAR(v),
			Sql.VARCHAR("A"),
			Sql.VARCHAR("Y"),
			Sql.VARCHAR("SOME_VALUE"),
            Sql.VARCHAR(""),
			Sql.VARCHAR("Y"),
			Sql.VARCHAR(""),
			Sql.VARCHAR
        ]
        log.info("Executing Create SQL: " + statementSqlCreate + " with values " + valuesCreate.toString())
        def createStatus = sql.call(statementSqlCreate, valuesCreate)
		log.ok("Create SQL Result: [" + createStatus + "]")
	
	}

    def statementSql3rdPartyCreate = "call gb_some_access.p_create(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
    def values3rdPartyCreate = [
            Sql.INTEGER(id),
			Sql.VARCHAR("N"),
            Sql.VARCHAR("N"),
            Sql.VARCHAR("DB_LOGIN"),
            null,
            null,
            null,
            null,
            Sql.VARCHAR("SYSTEM"),
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


    def statementSqlProcPin = "SELECT gb_some_access.f_proc3_pin(?) FROM dual"
    def valuesProcPin = [Sql.INTEGER(id)]
    log.info("Executing Create SQL: " + statementSqlProcPin + " with values " + valuesProcPin.toString())
    def procPinStatus = sql.firstRow(statementSqlProcPin, valuesProcPin)
    log.ok("Create SQL Result: [" + procPinStatus[0]?.toString() + "]")


    //NOTE: Not checking result status because if there's no error it's always the same value! Error should throw exception and block create.
    log.info("Create Script complete!")
    return new Uid(id)
}

