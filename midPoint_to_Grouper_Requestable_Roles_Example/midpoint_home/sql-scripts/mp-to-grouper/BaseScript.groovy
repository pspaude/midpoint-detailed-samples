import org.identityconnectors.framework.common.objects.ObjectClass
import common.ColumnPrefixMapper


class BaseScript extends Script {

    public static final int SYNC_MAX_ROWS = 5000

    public static final String LOG_PREFIX = "Scripted SQL to Grouper Requests: "

    //Connector Object Classes
    public static final String GROUP_CLASS_NAME = "Group"
    public static final ObjectClass GROUP_CLASS = new ObjectClass(GROUP_CLASS_NAME)

    //Table Names adjust to your database and setup
    public static final String USER_TABLE = "mp_gr_group_requests"
    public static final String GROUP_TABLE = "mp_gr_group_requests"

    //Optional Column Prefix
    public static final ColumnPrefixMapper PREFIX_MAPPER_USER = new ColumnPrefixMapper("mpgru")
    public static final ColumnPrefixMapper PREFIX_MAPPER_GROUP = new ColumnPrefixMapper("mpgrg")

    //Column Names adjust to your table structure
    public static final String SUBJECT_ID_COLUMN = "subject_id"
    public static final String GROUP_ID_COLUMN = "group_name"
    public static final String USER_SYNC_TIMESTAMP_COLUMN = "updated"
    public static final String GROUP_SYNC_TIMESTAMP_COLUMN = "updated"

    //Sync Primary Keys used below
    public static final String USER_SYNC_PRIMARY_KEY = "CONCAT(" + PREFIX_MAPPER_USER.defaultPrefix + "." + GROUP_ID_COLUMN + ", " + PREFIX_MAPPER_USER.defaultPrefix + "." + SUBJECT_ID_COLUMN + ")"
    public static final String GROUP_SYNC_PRIMARY_KEY = "CONCAT(" + PREFIX_MAPPER_GROUP.defaultPrefix + "." + GROUP_ID_COLUMN + ", " + PREFIX_MAPPER_GROUP.defaultPrefix + "." + SUBJECT_ID_COLUMN + ")"

    //Sync items used internally
    public static final String ID_VALUE = "id"
    public static final String TIMESTAMP_VALUE = "timestamp"
    public static final String COUNT_VALUE = "count"

    //Queries adjust to your database and table structure
    public static final String QUERY_TEST = "select 1 from " + USER_TABLE + " limit 1"
    public static final String QUERY_USER = "select " + PREFIX_MAPPER_USER.defaultPrefix + "." + SUBJECT_ID_COLUMN + ", " + PREFIX_MAPPER_USER.defaultPrefix + "." + GROUP_ID_COLUMN + ", " + PREFIX_MAPPER_USER.defaultPrefix + "." + USER_SYNC_TIMESTAMP_COLUMN + " as " + TIMESTAMP_VALUE + " from " + USER_TABLE + " " + PREFIX_MAPPER_USER.defaultPrefix
    public static final String QUERY_GROUPS = "select distinct " + PREFIX_MAPPER_GROUP.defaultPrefix + "." + GROUP_ID_COLUMN + ", " + PREFIX_MAPPER_GROUP.defaultPrefix + "." + GROUP_SYNC_TIMESTAMP_COLUMN + " as " + TIMESTAMP_VALUE + " from " + GROUP_TABLE + " " + PREFIX_MAPPER_GROUP.defaultPrefix
    public static final String QUERY_GROUPS_COUNT_BEGIN = "select count(" + PREFIX_MAPPER_GROUP.defaultPrefix + "." + GROUP_ID_COLUMN + ") as " + COUNT_VALUE + " from " + GROUP_TABLE + " " + PREFIX_MAPPER_GROUP.defaultPrefix + " where " + PREFIX_MAPPER_GROUP.defaultPrefix + "." + GROUP_ID_COLUMN + " LIKE '%"
    public static final String QUERY_GROUPS_COUNT_END = "%';"

    //Sync Queries
    public static final String SYNC_ACCOUNT = "" +
            "SELECT " +
            BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.USER_SYNC_TIMESTAMP_COLUMN + " as " + BaseScript.TIMESTAMP_VALUE + ", " +
            BaseScript.USER_SYNC_PRIMARY_KEY + " as " + BaseScript.ID_VALUE +
            " FROM " + BaseScript.USER_TABLE + " " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + " " +
            "WHERE " +
            "    (" + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.USER_SYNC_TIMESTAMP_COLUMN + " = :" + BaseScript.TIMESTAMP_VALUE + " AND " + BaseScript.USER_SYNC_PRIMARY_KEY + " > :" + BaseScript.ID_VALUE+ ")  OR " +
            "    (" + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.USER_SYNC_TIMESTAMP_COLUMN + " > :" + BaseScript.TIMESTAMP_VALUE + ") " +
            "ORDER by " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.USER_SYNC_TIMESTAMP_COLUMN + ", " + BaseScript.ID_VALUE

    public static final String SYNC_TOKEN_ACCOUNT = "" +
            "SELECT " +
            BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.USER_SYNC_TIMESTAMP_COLUMN + " as " + BaseScript.TIMESTAMP_VALUE + ", " +
            BaseScript.USER_SYNC_PRIMARY_KEY + " as " + BaseScript.ID_VALUE +
            " FROM " + BaseScript.GROUP_TABLE + " " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + " " +
            "ORDER by " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.USER_SYNC_TIMESTAMP_COLUMN +" DESC, " + BaseScript.ID_VALUE + " DESC " +
            "LIMIT 1"

    @Override
    Object run() {
        return null
    }
}