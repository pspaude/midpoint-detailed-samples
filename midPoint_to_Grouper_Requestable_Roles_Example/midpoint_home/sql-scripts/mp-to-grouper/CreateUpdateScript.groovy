import common.ScriptedSqlUtils

import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.*
import org.identityconnectors.framework.common.objects.AttributeUtil
import org.identityconnectors.framework.common.objects.Attribute
import java.sql.Connection
import java.sql.SQLIntegrityConstraintViolationException
import java.util.stream.Collectors
import static common.ScriptedSqlUtils.*


def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def attributes = attributes as Set<Attribute>
def id = id as String
def uidValue = (id) ? new Uid(String.valueOf(id)) : uid as Uid
def configuration = configuration as ScriptedSQLConfiguration
def connection = connection as Connection
def schema = schema as Schema


log.info(BaseScript.LOG_PREFIX + "Entering CreateUpdate Script current trying to perform " + operation + " operation.")
def sql = new Sql(connection)

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        return handleAccountCreateUpdate(sql, uidValue)
    case BaseScript.GROUP_CLASS:
        return handleGroupCreateUpdate(sql, uidValue)
    default:
        throw new ConnectorException("Unknown object class in CreateUpdateScript with value " + objectClass)
}

//Begin Account/User Methods
Uid handleAccountCreateUpdate(final Sql sql, final Uid uidValue) {
    if (OperationType.CREATE.equals(operation)) {
        try {
            log.ok(BaseScript.LOG_PREFIX + "Attempting create/insert membership operation for " + id)
            return handleAccountMembershipCreate(sql, uidValue)

        } catch (final Exception e) {
            log.ok(BaseScript.LOG_PREFIX + "Encountered already exists exception for membership, trying update! " + e)
            return handleAccountMembershipUpdate(sql, uidValue)
        }

    } else {
        log.ok(BaseScript.LOG_PREFIX + "Performing update operation membership for " + uidValue.getUidValue())
        return handleAccountMembershipUpdate(sql, uidValue)
    }
}

Uid handleAccountMembershipCreate(final Sql sql, final Uid uidValue) {
    def uidNew = null
    final String[] memberships = getStringArray(attributes, "grouperGroupMembershipNames")
    final String subjectId = (uidValue?.getUidValue()) ? uidValue.getUidValue() : getString(attributes, "grouperSubjectId")

    if (memberships && subjectId) {
        for (final String group : memberships) {
            Map<String, Object> params = [
                    subject_id : entitlementid,
                    group_name: group
            ]

            sql.withTransaction {
                try {
                    def ret = ScriptedSqlUtils.buildAndExecuteInsertQuery(sql, BaseScript.USER_TABLE, params, BaseScript.LOG_PREFIX)
                    //uidNew = new Uid(String.valueOf(ret[0][0]))

                } catch (final SQLIntegrityConstraintViolationException ex) {
                    throw new AlreadyExistsException("Object with id " + id + " already exists in create for membership!", ex)
                }
            }
        }

        uidNew = new Uid(subjectId)

    } else {
        log.info(BaseScript.LOG_PREFIX + "No subject_id was supplied to create membership! Returning null, create operation cannot proceed!")
    }

    return uidNew  //return __UID__ of the created group
}

Uid handleAccountMembershipUpdate(final Sql sql, final Uid uidValue) {
    final String[] updatesToMemberships = getStringArray(attributes, "grouperGroupMembershipNames")
    final String subjectId = uidValue.getUidValue()

    if (subjectId?.trim() && updatesToMemberships?.size() > 0) {
        Set toInsert = new HashSet<>(Arrays.asList(updatesToMemberships))
        Set toDelete

        def currentMemberships = getCurrentMemberships(sql, subjectId, log)?.get(subjectId)

        if (currentMemberships && !currentMemberships?.isEmpty()) {
            toInsert = updatesToMemberships.stream().filter({ e -> !currentMemberships.contains(e) }).collect(Collectors.toSet())
            toDelete = currentMemberships.stream().filter({ e -> !updatesToMemberships.contains(e) }).collect(Collectors.toSet())
        }

        sql.withTransaction {
            for(final String delGroup : toDelete) {
                ScriptedSqlUtils.buildAndExecuteDeleteQuery(sql, BaseScript.USER_TABLE, [subject_id: subjectId as String, group_name: delGroup], BaseScript.LOG_PREFIX)
            }

            for(final String insGroup : toInsert) {
                ScriptedSqlUtils.buildAndExecuteInsertQuery(sql, BaseScript.USER_TABLE, [subject_id: subjectId as String, group_name: insGroup], BaseScript.LOG_PREFIX)
            }
        }

    } else {
        //nothing to do no membership updates
    }

    return new Uid(uidValue.getUidValue())
}

private Map getCurrentMemberships(final Sql sql, final String subjectId, def log) {
    final Class<?> UID_TYPE_ACCOUNT = String
    final Map userQueryParams = ["__uid__":subjectId]
    final Map userResults = [:]

    String sqlQuery = BaseScript.QUERY_USER + " where " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." +
            BaseScript.SUBJECT_ID_COLUMN + " = :__uid__"

    log.ok(BaseScript.LOG_PREFIX + "Running get single users memberships query ''{0}'', params ''{1}''", sqlQuery, userQueryParams)
    sql.withTransaction {
        String paging = buildPaging(options, userQueryParams, BaseScript.LOG_PREFIX)

        if (!paging.isEmpty()) {
            sqlQuery = sqlQuery + " " + paging
        }

        Closure filter = { row ->
            if (row?.getProperty(BaseScript.SUBJECT_ID_COLUMN)?.trim() && row?.getProperty(BaseScript.GROUP_ID_COLUMN)?.trim()) {
                userResults.computeIfAbsent(row.getProperty(BaseScript.SUBJECT_ID_COLUMN).trim(), k -> new HashSet<String>()).add(row.getProperty(BaseScript.GROUP_ID_COLUMN).trim())
            }
        }

        if (!userQueryParams.isEmpty()) {
            sql.eachRow(userQueryParams, sqlQuery, filter)
        } else {
            sql.eachRow(sqlQuery, filter)
        }
    }

    return userResults
}
//End Account/User methods

//Entitlement Methods
Uid handleGroupCreateUpdate(final Sql sql, final Uid uidValue) {
    if (OperationType.CREATE.equals(operation)) {
        try {
            log.ok(BaseScript.LOG_PREFIX + "Attempting create/insert operation for group request for " + id)
            return handleGroupCreate(sql, uidValue)
        } catch (final Exception e) {
            log.ok(BaseScript.LOG_PREFIX + "Encountered already exists exception for group request create trying update! " + e)
            return handleGroupUpdate(sql, uidValue)
        }
    } else {
        log.ok(BaseScript.LOG_PREFIX + "Performing update operation for homedirectory for " + uidValue.getUidValue())
        return handleGroupUpdate(sql, uidValue)
    }
}

Uid handleGroupCreate(final Sql sql, final Uid uidValue) {
    def uidNew = null
    final String groupName = (uidValue?.getUidValue()) ? uidValue.getUidValue() : getString(attributes, "groupId")

    if (groupName?.trim()) {
        //uidNew = new Uid(groupName)
        return uidValue //don't do anything return synthetic result, will be added by membership later
    } else {
        log.info(BaseScript.LOG_PREFIX + "No group name was supplied to create group! Returning null, create operation cannot proceed!")
    }

    return uidNew  //return __UID__ of the created group
}

Uid handleGroupUpdate(final Sql sql, final Uid uidValue) {
    final String groupName = uidValue.getUidValue()

    if (groupName?.trim()) {
        final Map<String, Object> params = [:]

        for (final Attribute attribute : attributes) {
            Object value

             if (attribute.getValue() != null && attribute.getValue()?.size() > 1) {
                value = attribute.getValue()
            } else {
                value = AttributeUtil.getSingleValue(attribute)
            }

            params.put(attribute.getName(), value)
        }

        sql.withTransaction {
            ScriptedSqlUtils.buildAndExecuteUpdateQuery(sql, BaseScript.GROUP_TABLE, params, [group_name: groupName], BaseScript.LOG_PREFIX)
        }
    } else {
        log.info(BaseScript.LOG_PREFIX + "No group name was supplied to update group! Returning null, create operation cannot proceed!")
        return null
    }

    return uidValue
}
