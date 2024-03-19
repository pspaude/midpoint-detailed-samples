#Grouper Rest Examples

This uses an enhanced/update version of the Internet2 Grouper Rest connector. 

The enhancements and this example were created and funded by University of Wisconsin Madison. See this [link](https://github.internet2.edu/pspaude/midPoint-Grouper_connector)

The connector has added timeouts, additional filtering options and performance (paging) enhancements over the existing Grouper Rest connector. 

Filtering of Groups is now done by Grouper via the REST calls where possible typically bringing 
 significant performance improvements. Still there are limits to what REST and the Grouper 
 Web Services can provide. As a result, it's recommended to look at other integration options
 such as SQL or LDAP. 
