# Findwise integration module for Alfresco

This module integrates Alfresco with the Findwise search framework which allows Alfresco to publish documents to the Findwise indexing service.

## Architecture

Publishing of documents to the Findwise indexing service is done by a Policy which reacts on property updates once a specific aspect has been applied to a node (fwi:findwiseIndexable).

It's the responsability of the implementing application to apply this aspect to nodes which should be candidates for publishing.

Furthermore a validation occurs before a node is published. The default validation is implemented in the processor called DefaultVerifierProcessor. Typically one have additional requirements that should be taken into account when verifying a node for publishing. In those cases the DefaultVerifierProcessor is overridden or extended and its replacement is injected into the SearchIntegrationService.

## Configuration options
In alfresco-global.properties you have the following configuration parameters to use:
```
#Findwise
findwise.pushEnabled=true 
#Prod
#findwise.pushService=http://localhost:8080/rest/test/documents.json
#Test
findwise.pushService=http://localhost:8080/rest/alfresco/documents.json
#Default max file size 20mb
findwise.maxFileSize=20971520
#Default allowed file extensions
findwise.allowedFileExtensions=doc,docx,docm,xls,xlsx,xlsm,ppt,pptx,pptm,pdf,odt,ods,odp,txt,html
```

## Installation
TODO

