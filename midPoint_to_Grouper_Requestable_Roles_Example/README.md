# Example of midPoint to Grouper Request Process

This is an example of using midPoint's built-in Role Catalog and Shopping Cart functionality.
 It allows authorized users to order/request groups/roles/authorizations, 
  and if approval is needed, gain approval, and send that information to Grouper via an intermediate SQL table. 

For more information on Role Request, Shopping Cart, and GUI authorizations, the following 
  are some links to relevant Evolveum midPoint authorization documentation:
* https://docs.evolveum.com/midpoint/reference/support-4.8/admin-gui/request-access/
* https://docs.evolveum.com/midpoint/reference/before-4.8/admin-gui/role-request/configuration/
* https://docs.evolveum.com/midpoint/reference/support-4.8/security/authorization/configuration/
* https://docs.evolveum.com/midpoint/reference/support-4.8/cases/approval/examples/1-multi-stage-metarole-driven-approvals/

Grouper also can be integrated to send application and policy result information back to midPoint for downstream
 provisioning. (Grouper to midPoint). 
See the Grouper_to_midPoint_Inbound_Groups_Example in this repo or the following links:
* https://spaces.at.internet2.edu/display/Grouper/Grouper+MidPoint+provisioner
* https://docs.evolveum.com/connectors/connectors/com.evolveum.polygon.connector.grouper.GrouperConnector/
* https://github.internet2.edu/docker/midPoint_container/tree/master/demo/grouper
* https://github.internet2.edu/internet2/InCommonTAP-Examples/tree/main/Workbench

The midPoint to Grouper Request Roles Process was funded and used by Virginia Tech and Unicon Inc. 
