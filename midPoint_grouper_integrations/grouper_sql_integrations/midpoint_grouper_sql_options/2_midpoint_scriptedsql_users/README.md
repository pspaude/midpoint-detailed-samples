# midPoint-Grouper Scripted SQL Option

## Advantages
The benefits of this option are less processing in midPoint of Organization relationships and a more
 midPoint native mapping of objects. No work-arounds or views needed for Database Table limitations.


## Disadvantages

The processing is done in the Groovy Scripted SQL scripts. It's possible this may not be performant
 at some scale, but that has yet to be determined. There is also extra overhead in maintaining those scripts
  and the added connector jar. However, that is true of any Scripted SQL implementation and
   other connectors outside the bundled connectors. It is hoped that if this connector is chosen
   the community will assist with any (likely very rare) maintenance that's required.


## Configuration/Changes
Most changes for your deployment and Grouper integration should be made in the resource:
 *mp_home/post-initial-objects/resources/100-inbound_grouper_groups.xml*. 

In rare cases where you are using a different database other than PostgreSQL, different table structure or other 
 large changes from this guide/reeadmes then changes may have to be made to *mp_home/sql-scripts/grouper-groups/BaseScript.groovy*.


## Flat Group Organzation in midPoint

If you desire a flat structure under the generic "Grouper Groups" organization in midPoint then in
*mp_home/sql-scripts/grouper-groups/BaseScript.groovy* set the boolean **shouldCreateGroupHierarchy** to *false*. 
This should also have the benefit of less processing in the connector scripts.

## Miscellaneous

The Scripted SQL Groovy Scripts are direct copies where possible of the [Evolveum examples](https://github.com/Evolveum/midpoint-samples/tree/master/samples/resources/scriptedsql) 
and only changed to  accommodate this integration and make some areas simpler. 

It is believed in 99% of cases you shouldn't have to modify the Groovy script files other than 
 if you're using a different database technology, different table structure or view, or have other unique concerns.


This was tested in midPoint version 4.4.x+, issues were found in older versions!