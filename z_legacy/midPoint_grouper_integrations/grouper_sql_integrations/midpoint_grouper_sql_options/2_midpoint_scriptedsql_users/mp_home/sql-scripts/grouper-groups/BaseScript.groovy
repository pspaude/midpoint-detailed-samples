import org.identityconnectors.framework.common.objects.ObjectClass
import common.ColumnPrefixMapper


class BaseScript extends Script {

    public static final int SYNC_MAX_ROWS = 5000

    public static final String LOG_PREFIX = "Grouper Groups: "
    public static final String GROUPER_GROUP_CLASS_NAME = "Group"
    public static final ObjectClass GROUPER_GROUP_CLASS = new ObjectClass(BaseScript.GROUPER_GROUP_CLASS_NAME)
    public static final ColumnPrefixMapper PREFIX_MAPPER_USER = new ColumnPrefixMapper("mgu")
    public static final ColumnPrefixMapper PREFIX_MAPPER_GROUP = new ColumnPrefixMapper("mgg")

    //Option for this connector to create parent Groups that aren't assigned members this is for hierarchical display in midPoint only
    public static final boolean shouldCreateGroupHierarchy = true

    //Table Names adjust to your database and setup
    public static final String USER_TABLE = "midpoint_groups"
    public static final String GROUP_TABLE = "midpoint_groups"

    //Column Names adjust to your table structure
    public static final String GROUP_ID_COLUMN = "group_name"
    public static final String SUBJECT_ID_COLUMN = "subject_id"
    public static final String USER_SYNC_TIMESTAMP_COLUMN = "updated"
    public static final String GROUP_SYNC_TIMESTAMP_COLUMN = "updated"

    //Queries adjust to your database and table structure
    public static final String QUERY_TEST = "select 1 from " + BaseScript.USER_TABLE + " limit 1"
    public static final String QUERY_USER = "select " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.SUBJECT_ID_COLUMN + ", " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.GROUP_ID_COLUMN + ", " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.USER_SYNC_TIMESTAMP_COLUMN + " as " + BaseScript.TIMESTAMP_VALUE + " from " + BaseScript.USER_TABLE + " " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix
    public static final String QUERY_GROUPS = "select distinct " + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix + "." + BaseScript.GROUP_ID_COLUMN + ", " + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix + "." + BaseScript.GROUP_SYNC_TIMESTAMP_COLUMN + " as " + BaseScript.TIMESTAMP_VALUE + " from " + BaseScript.GROUP_TABLE + " " + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix
    public static final String QUERY_GROUPS_COUNT_BEGIN = "select count(" + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix + "." + BaseScript.GROUP_ID_COLUMN + ") as " + BaseScript.COUNT_VALUE + " from " + BaseScript.GROUP_TABLE + " " + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix + " where " + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix + "." + BaseScript.GROUP_ID_COLUMN + " LIKE '%"
    public static final String QUERY_GROUPS_COUNT_END = "%';"


    //Sync Primary Keys used below
    public static final String USER_SYNC_PRIMARY_KEY = "CONCAT(" + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.GROUP_ID_COLUMN + ", " + BaseScript.PREFIX_MAPPER_USER.defaultPrefix + "." + BaseScript.SUBJECT_ID_COLUMN + ")"
    public static final String GROUP_SYNC_PRIMARY_KEY = "CONCAT(" + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix+ "." + BaseScript.GROUP_ID_COLUMN + ", " + BaseScript.PREFIX_MAPPER_GROUP.defaultPrefix + "." + BaseScript.SUBJECT_ID_COLUMN + ")"

    //Sync items used internally
    public static final String ID_VALUE = "id"
    public static final String TIMESTAMP_VALUE = "timestamp"
    public static final String COUNT_VALUE = "count"
    public static final String UID_VALUE = "__uid__"

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