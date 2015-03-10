# Findwise integration module for Alfresco

This module integrates Alfresco with the Findwise search framework which allows Alfresco to publish documents to the Findwise indexing service.

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
