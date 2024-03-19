# midPoint-Grouper Database Table MultiAccount Option

## Advantages
The benefits of this option are using the simpler bundled Database Table connector to natively
 pull in the Group memberships using midPoint MultiAccount functionality to process the multiple rows
  per identity into a multivalued membership assignment. 


## Disadvantages
The drawbacks are an added view due to Database Table connector limitations and increased 
 configuration and processing in midPoint of Groups (Organizations)
  and their relationships. 

In addition, at the time of this writing (4.4.x) it's been reported
  that their may be bugs in the tag/multiaccount functionality in certain deployments.


## Configuration/Changes
Most changes for your deployment and Grouper integration should be made in the resource:
 *mp_home/post-initial-objects/resources/100-inbound_grouper_groups.xml*. 
