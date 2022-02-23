import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.MapFilterVisitor
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.ResultsHandler
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.Filter
import java.sql.Connection
import java.sql.Types
import java.sql.ResultSet
import groovy.sql.Sql
import groovy.sql.DataSet;
import groovy.sql.InOutParameter;
import oracle.jdbc.OracleTypes;


def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def configuration = configuration as ScriptedSQLConfiguration
def filter = filter as Filter
def connection = connection as Connection
def query = query as Closure
def handler = handler as ResultsHandler

log.info("Entering " + operation + " Script!")

def sql = new Sql(connection)

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        log.info("Searching for Account/Users...")
        handleAccount(sql)
        break
    default:
        log.info('In Default!')
        throw new ConnectorException("Warning this Resource only handles Accounts/Users! Unsupported objectClass " + objectClass)
}

log.info("SearchScript Complete.")
return new SearchResult();

//handle accounts/users
void handleAccount(Sql sql) {
    Map params = [:]

	def innerSelect = "SELECT SOME_pidm, SOME_emal_code, SOME_email_address, SOME_external_user FROM SOME LEFT JOIN SOME ON SOME_pidm = SOME_pidm"
	
    String where = buildWhereClause(filter, params, 'SOME_pidm', 'SOME_pidm')

    def fullSelect = "SELECT SOME_pidm, email_univ, email_per1, email_per2, SOME_external_user FROM (" + innerSelect + where + ") PIVOT (MAX(SOME_email_address) FOR SOME_emal_code IN ('UNIV' AS email_univ,'PER1' AS email_per1,'PER2' AS email_per2))"

    log.info("Running Search Query: [" + fullSelect + "] params [" + params + "]...")
    sql.eachRow(params, fullSelect, { row ->
        handler {
            uid row.SOME_pidm as String
            id row.SOME_pidm as String
            attribute 'pidm', row.SOME_pidm
            attribute 'emailUniv', row.email_univ
			attribute 'emailPer1', row.email_per1
			attribute 'emailPer2', row.email_per2
            attribute 'externalUser', row.SOME_external_user
        }

        lastId = row.SOME_pidm
    });
}

def String buildWhereClause(Filter filter, Map sqlParams, String uidColumn, String nameColumn) {
    if (filter == null) {
        log.info("Returning empty where clause")
        return ""
    }

    Map query = filter.accept(MapFilterVisitor.INSTANCE, null)

    log.info("Building where clause, query {0}, uidcolumn {1}, nameColumn {2}", query, uidColumn, nameColumn)

    String columnName = uidColumn.replaceFirst("[\\w]+\\.", "")

    String left = query.get("left")
    if (Uid.NAME.equals(left)) {
        left = uidColumn
    } else if (Name.NAME.equals(left)) {
        left = nameColumn
    }

    String right = query.get("right")

    String operation = query.get("operation")
    switch (operation) {
        case "CONTAINS":
            right = '%' + right + '%'
            break;
        case "ENDSWITH":
            right = '%' + right
            break;
        case "STARTSWITH":
            right = right + '%'
            break;
    }

    sqlParams.put(columnName, right)
    right = ":" + columnName

    def engine = new groovy.text.SimpleTemplateEngine()

    def whereTemplates = [
            CONTAINS          : ' $left ${not ? "not " : ""}like $right',
            ENDSWITH          : ' $left ${not ? "not " : ""}like $right',
            STARTSWITH        : ' $left ${not ? "not " : ""}like $right',
            EQUALS            : ' $left ${not ? "<>" : "="} $right',
            GREATERTHAN       : ' $left ${not ? "<=" : ">"} $right',
            GREATERTHANOREQUAL: ' $left ${not ? "<" : ">="} $right',
            LESSTHAN          : ' $left ${not ? ">=" : "<"} $right',
            LESSTHANOREQUAL   : ' $left ${not ? ">" : "<="} $right'
    ]

    def wt = whereTemplates.get(operation)
    def binding = [left: left, right: right, not: query.get("not")]
    def template = engine.createTemplate(wt).make(binding)
    def where = template.toString()

    where = " WHERE " + where
    log.info("Where clause: {0}, with parameters {1}", where, sqlParams)
    return where
}
