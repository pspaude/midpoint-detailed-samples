# Grouper SQL Provisioner

The folder contains Grouper configuration properties files and a SQL file showing
 an example of setting up an intermediate database to be used for midPoint and 
  Grouper SQL integrations. 

### Subject Source

midPoint can create/provision Grouper's subject source. 
You'll need a few properties for the database in "External Systems" which is 
 grouper-loader.properties and the rest is in subject.properties. For example
SQL see the postgres.sql file the first section for subject source.

### midPoint-Grouper SQL membership table

Grouper can provision group memberships to a table using the SQL provisioner that 
 midPoint then can consume and make provisioning decisions on. See primarily
 grouper-loader.properties and the last section of postgres.sql.