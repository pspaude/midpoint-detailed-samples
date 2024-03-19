# Grouper SQL Integration Samples

This folder contains options for integrating Grouper with midPoint using an intermediate Postgres SQL database.

This intermediate database could live anywhere including on the same database server as Grouper and midPoint
 or simply by adding the tables to Grouper's database, or in a separate schema as desired. 

This allows you to send identities (subjects) from midPoint to Grouper and also allows you to bring  
 Grouper group memberships, groups and group attributes into midPoint using various SQL connector options listed below.

**If you're not concerned about setting up a Grouper subject source skip to step IV!**


## midpoint-Grouper Identities into Grouper Subject Source Setup

### I. Add person Subject Source table to Database

Grouper needs a Subject Source with identities. To provision that subject source from midPoint, first you'll need to
 add the provided SQL to your database and create the table. See *grouper_config/postgres.sql*

If you're using a different database technology you'll need to adapt to that ddl as needed.


### II. Provision to the Grouper Subject Source table in midPoint

Next in midPoint add the objects found in *midpoint_outbound_grouper_subject_source* to send identities to the
 table created in the previous step. 

Note that if you want to use the extension attribute *loginid* then you'll need to add the schema extension to midPoint 
 home and restart midPoint. 

Also, please heed the warning to not blindly overwrite existing objects! If there is an
 existing object with a same or similar name, it's usually necessary to merge these changes. 
This holds true for all provided midPoint configuration.

Finally, these examples were created for midPoint 4.4.x and may need changes for different versions.

### III. Setup the person table Subject Source in Grouper

Inside of Grouper either using the UI or provided *grouper_config/subject.properties* file configure the 
 person table added above as the Grouper subject source. 

Note that if you're copying the provided files, you'll also need the base reference in grouper-loader.properties 
 which relates to "External Systems" in the UI. 


## midPoint-Grouper Group Memberships into midPoint Setup

### IV. Add Grouper Group Membership Table

Add the midpoint_groups table to the database. See *grouper_config/postgres.sql*. 
If the table already exists and was added previously ignore this step.  

Reminder if you're using different database technology you'll need to adapt to that ddl as needed.


### V. Setup Grouper SQL Provisioner to the group memberships table

Using the Grouper UI or the provided file *grouper_config/grouper-loader.properties*, create the 
 Grouper SQL provisioner that will send Group Membership information (and can be expanded to other details) 
 to the intermediate database. This should work with Grouper v2.6.x.


### VI. Configure midPoint to consume Grouper membership information

Next you'll need to pick a midPoint-Grouper Group Membership SQL integration option. The options are listed below.

1. Scripted SQL Affiliations and Groups - *Not Attempted Yet* TODO
2. Scripted SQL Users and Groups
3. Database Table MultiAccount
4. Database Table Aggregate


#### 1. Scripted SQL Affiliations

This will use affiliations and assign to organization in that way. This is a method to more closely
 match the data in the database to the imports in midPoint. This is not complete yet.

See the *midpoint_grouper_sql_options/1_midpoint_scriptedsql_affiliations* folder.


#### 2. Scripted SQL Users and Groups

This is the current recommended approach as it should accommodate most setups. 
 The resource brings in users/accounts and group objects from the single Grouper provisioned
 table. The benefits are the Scripted SQL connector controls the Group structure 
  and memberships are multivalued so there is less work to do in midPoint itself.

See the *midpoint_grouper_sql_options/2_midpoint_scriptedsql_users* folder and readme there.


#### 3. Database Table Multiaccount

This uses the midPoint multiaccount and tag functionality to natively bring in
 the multiple user's memberships using the Database Table Connector.
  This fixes the limitations of option 4 below, however at the time of this writing it may not work due to possible bugs in midPoint.
 Evolveum is working it and so this may become a recommended option for those that
 don't want to use Scripted SQL. As with option 4 the other tradeoff is midPoint 
  needs to do processing for the Organziation/Group structure in midPoint objects.

See the *midpoint_grouper_sql_options/3_midpoint_dbtable_multiaccount* folder.


#### 4. Database Table Aggregate

This use the midPoint Database Table connector which is bundled with midPoint. It uses
 a database view to aggregate the memberships into one column so there is one row per 
 user which is what midPoint prefers. The limitation here is the user can only have a
 certain number of max memberships based on database column limits and the Java String class.
Another tradeoff is midPoint is forced to process that list and the Group structure if you choose
 to bring in your Organization/Group structure. 

See the *midpoint_grouper_sql_options/4_midpoint_dbtable_aggregate* folder.