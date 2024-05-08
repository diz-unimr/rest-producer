# Concept
## Dynamic configuration for source data
* build a m-array-tree of REST endpoint targets with a *FIRST CHILD/NEXT SIBLING REPRESENTATION*
  * each tree node will be embedded into one property of parent structure
  * configuration will be automatically traversed
  * depth and length is dynamic
* JSON-Path property selection
  * value for next sibling endpoints are described via JSON-path expressions 

### m-arry tree structures example
A regular m-array tree looks like this. 
```angular2html
         A
       / | \
      B  C  D
     /|\  
    E F G 
   /|  
  H I
```
Our configuration uses a *first child/next sibling representation* of that tree which looks like this.
```angular2html
        A
        |
        B --> C --> D 
        |           
        E --> F --> G
        |      
        H --> I
```


## loading data
Endpoints are read from root to bottom of defined n-array-tree.

| config key                                       | value                            | description                                                                                                                                                                                                                                                                     |
|--------------------------------------------------|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| app.loader.username                              | text                             | (optional) basic auth username                                                                                                                                                                                                                                                  |                                                                                                                                                                                                                                                            
| app.loader.password                              | text                             | (optional) basic auth password                                                                                                                                                                                                                                                  |
| app.loader.endpoints[x]                          | x is a integer number from 0...n | Each entry defines one REST endpoint                                                                                                                                                                                                                                            |
| app.loader.endpoints[x].endpoint-name            | text                             | **output kafka topic name** <br> Also internal reference name for this endpoint - can be used at nextSiblingEndpointor or nextChildEndpointName                                                                                                                                 | 
| app.loader.endpoints[x].endpoint-address         |                                  | Target endpoint request url. parameter must fit naming convention: URL variables may contain only: Lowercase alphabetical,Numerical,Hyphen,Underscore. Therefore we replace other characters with '_'    - e.g. 'jobs[*].job_id' -> 'jobs____job_id'. Each name must be unique. |
| app.loader.endpoints[x].id-property              | json path expression             | Identifier property of endpoint result json - also the **kafka message key**                                                                                                                                                                                                    |
| app.loader.endpoints[x].pagination               | boolean                          | (optional)   **not implemented yet**                                                                                                                                                                                                                                            | 
| app.loader.endpoints[x].page-size-param-name     |                                  | (optional)   **not implemented yet**                                                                                                                                                                                                                                            |
| app.loader.endpoints[x].page-size-value          |                                  | (optional)   **not implemented yet**                                                                                                                                                                                                                                            |
| app.loader.endpoints[x].page-param-name          |                                  | (optional)   **not implemented yet**                                                                                                                                                                                                                                            |
| app.loader.endpoints[x].page-start-value         |                                  | (optional)   **not implemented yet**                                                                                                                                                                                                                                            |
| app.loader.endpoints[x].next-child-endpoint-name | text                             | (optional) endpoint name of a child node                                                                                                                                                                                                                                        |
| app.loader.endpoints[x].next-sibling-endpoint    | text                             | (optional) endpoint name of a sibling node                                                                                                                                                                                                                                      |
| app.loader.endpoints[x].next-node-reference      | json path expression             | (optional) path to the property which contains values to use as parameter for next child endpoint call.                                                                                                                                                                         |
| app.loader.endpoints[x].extraction-target        | json path expression             | (optional) **if defined** machted json will be extracted as payload to kaka topic                                                                                                                                                                                               |


## security
* support basic auth
* static access token (not implemented yet)