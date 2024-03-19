# midPoint-Grouper Database Table Aggregate Option

## Advantages
The benefits of this option are using the simpler bundled Database Table connector 
 pull in the Group memberships using the database to aggregate the various memberships
  into a single row per user in a view. This is the simplest integration option.


## Disadvantages
The main drawback is it requires a view and there is a hard limit on the size/number of Grouper memberships based on your 
  database technology's column size/limit and also midPoint's codebase Java string limits whichever is smaller. 

In addition, midPoint cannot check for size limits so your database will need to do that and inform you or throw errors
  otherwise expect clipped or incorrect membership lists in the event you are exceeding size limits.   


## Configuration/Changes
Most changes for your deployment and Grouper integration should be made in the resource:
 *mp_home/post-initial-objects/resources/100-inbound_grouper_groups.xml*. 
