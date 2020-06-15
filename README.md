# cf-manager

A project to consilidate some general channelfinder testing, debugging, and reporting operations

### Building

`mvn clean install`

### Launching cf-manager

```
cd /target/
java -jar cf-manager-0.0.1-SNAPSHOT.jar -h
```

Supported operations

```
-generate-report          - Generate a report on the recsync properties
-es_host  localhost       - elastic server host
-es_port  9200            - elastic server port
-help                     - print this text

```
