[![CI/CD](https://github.com/apized/apized/actions/workflows/cicd.yml/badge.svg)](https://github.com/apized/apized/actions/workflows/cicd.yml)

# Apized

[Apized](https://apized.org) is modern JVM-based framework designed to make API development easier. It aims to give
developers more time to focus on model definition and business logic implementation by automatically providing the
underlying implementations to easily create a rich REST API.

By adopting apized your API will provide:

- [Enhanced REST endpoints](https://github.com/apized/apized/wiki/Feature:-Enhanced-REST-endpoints)
- [Audit trail](https://github.com/apized/apized/wiki/Feature:-Audit-trail)
- [Event publishing](https://github.com/apized/apized/wiki/Feature:-Event-publishing)
- [Behaviours](https://github.com/apized/apized/wiki/Feature:-Behaviours)
- [OpenAPI documentation](https://github.com/apized/apized/wiki/Feature:-OpenAPI-documentation) (optional)
- [Security](https://github.com/apized/apized/wiki/Feature:-Security) (optional)
- [Federation](https://github.com/apized/apized/wiki/Feature:-Federation) (optional)

and will run on top of:

- [Micronaut](https://github.comhttps://github.com/apized/apized/wiki/Engine:-Micronaut) (default)
- [Spring Boot](https://github.comhttps://github.com/apized/apized/wiki/Engine:-Spring-Boot) (planned)

<!-- For more information on using Apized see the documentation at [apized.org](https://apized.org). -->

## Example applications

Example applications can be found [here](https://github.com/apized/samples).

## Developing

Start by publishing the packages to the local repo:

```
./gradlew -c first-settings.gradle publishToMavenLocal
```
