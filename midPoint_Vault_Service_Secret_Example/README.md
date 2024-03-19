# Example of using Service Objects to store Resource Secrets and Environment Configuration

This is an example of using midPoint service objects to store and automatically update resource object environment 
 and configuration secrets that may secrets handling and updating easier with tools such as Kubernetes and Hashicorp Vault. 

## Purpose

Hashicorp Vault and other PAM tools periodically generate new secrets. 
Currently, with midPoint you have to add that new secret into the database for the resource(s) that use that secret 
 to connect to external systems. This can be done via the GUI in a manual process, or REST API among other methods. 
The problem is resources may be reloaded from source control (SCM) systems and trying to navigate that and secrets 
 can lead to a mess of configuration. 

## Options

1. There are automatic midPoint retrieval methods such as Andrew Parmer's work at University of Wisconsin Madison 
 seen here: https://github.com/ParmerA/midpoint-envlib. That setup will automatically grab new 
 secrets from AWS Secrets Manager. This is great for an automatic pull style method.

2. Others prefer external methods, tools and integrations. This option/example is built for that purpose. 

## Brief Explanation




The midPoint Service Object as a Secret Holder was funded and is used by Purdue University