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
import groovy.sql.Sql
import groovy.sql.DataSet;


def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def configuration = configuration as ScriptedSQLConfiguration
def filter = filter as Filter
def connection = connection as Connection
def query = query as Closure
def handler = handler as ResultsHandler

log.info("Grouper Subject Registry: Entering " + operation + " Script!")

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

log.info("Grouper Subject Registry: SearchScript Complete.")
return new SearchResult();





//handle accounts/users
void handleAccount(Sql sql) {
    Map params = [:]
    //TODO add additional personal identifiers below!
    def extraPersonalIds = ["exampleId"]

    def select = "SELECT gsr.id, gsr.first_name, gsr.last_name, gsr.email_address, gsr.description, gsr.login_id"
    def from = " FROM subject.PERSON gsr LEFT JOIN  subject.PERSON_IDENTIFIERS gsrpids ON gsr.id = gsrpids.person_id"

    extraPersonalIds.each {
        select = select +  ", gsrpids.value AS " + it;
    }

    select = select + from
    String where = buildWhereClause(filter, params, 'gsr.id', 'gsr.id', extraPersonalIds)

    log.info("Running Search Query: [" + select + "] where [" + where + "] params [" + params + "]...")
    sql.eachRow(params, select + where, { row ->
        handler {
            uid row.id as String
            id row.id as String
            attribute 'id', row.id
            attribute 'firstname', row.first_name
            attribute 'lastname', row.last_name
            attribute 'email', row.email_address
            attribute 'description', row.description
            attribute 'loginid', row.login_id
            //TODO add additional personal identifiers below!
            attribute 'exampleid', row.exampleId
        }

        lastId = row.id
    });
}

def String buildWhereClause(Filter filter, Map sqlParams, String uidColumn, String nameColumn, List extraIds) {
    if (filter == null) {
        log.info("Returning empty where clause")
        return handleWhereExtraIds("", extraIds, sqlParams)
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

    where = handleWhereExtraIds(where, extraIds, sqlParams)
    log.info("Where clause: {0}, with parameters {1}", where, sqlParams)
    return where
}

String handleWhereExtraIds(String where, List extraIds, def params) {
    if (extraIds.size() > 0) {
        if (where.length() > 0) {
            where = where + " AND gsrpids.source IN (:srcIds)"
        } else {
            where = " gsrpids.source IN (:srcIds)"
        }
        params.put("srcIds", extraIds.join(","))
    }

    return " WHERE " + where
}