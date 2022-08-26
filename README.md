[![CI/CD](https://github.com/apized/apized/actions/workflows/cicd.yml/badge.svg)](https://github.com/apized/apized/actions/workflows/cicd.yml)

# Apized

[Apized](https://apized.org) is modern JVM-based framework designed to make API development easier. It aims to give
developers more time to focus on model definition and business logic implementation by automatically providing the 
underlying implementations to easily create a rich REST API. 

By adopting apized your API will provide:

- Audit trail
- Event publishing
- Enhanced REST endpoints
- Request execution pipeline
- OpenAPI documentation (optional)
- Security (optional)

and will run on top of:

- [Micronaut](https://micronaut.io/)
- [Spring Boot](https://spring.io/projects/spring-boot) [Planned]

For more information on using Apized see the documentation at [apized.org](https://apized.org).

## Example applications

Example applications can be found under `samples` on this repo.

## Known Issues

**_NOTE:_** This project is still experimental.

- Right now, we only support OneToOne, OneToMany and ManyToOne relationships. If a
  ManyToMany relationship is needed you will have to declare the joining table as an entity with
  OneToMany relations to both sides of the ManyToMany relation. We plan to better support these in the future.
