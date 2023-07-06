# caliban-tracing-poc

- To run `sbt run`
- Example request
  ```
  curl --location --request POST 'http://localhost:8080/api/graphql' \
  --header 'Content-Type: application/json' \
  --data-raw '{"query":"query {\n  someQuery {\n      value\n  }\n}\n","variables":{}}'
  ```
