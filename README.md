# Concept
## Dynamic configuration for source data
* build a n-tree of REST endpoint targets
  * each tree node will be embedded into one property of parent structure
  * configuration will be automatically traversed
  * depth and length is dynamic
* JSON-Path property selection
  * value for next sibling endpoints are described via JSON-path expressions 

## loading data
* nodes are executed per parent in parallel 


## security
* support basic auth
* static access token