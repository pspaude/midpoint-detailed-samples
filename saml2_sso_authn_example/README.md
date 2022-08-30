#Example of midPoint Flexible Authentication using a SAML2 SSO

This is an example of using midPoint's Flexible Authentication functionality to use a SAML2 SSO server to authenticate a user into midPoint.

Note: If using a container, it's usually best to ssh/exec/log into the container and grab the generated metadata via this 
  url: http://your_midPoint_server_url:8080/midpoint/auth/default/mysamlsso/metadata/spmidpoint using wget/curl or similar. Depending on how your container is configuered you likely need localhost or 127.0.0.1 in place of the midPoint_server_url. 

The SAML2 midPoint Service Provider Assertion Consumer Service URL will be:

The emergency login URL to bypass the SAML2 IdP (for administrators only) is: 


This example was created by Colorado School of Mines and Unicon. 
