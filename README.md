[![CI/CD](https://github.com/apized/apized/actions/workflows/cicd.yml/badge.svg)](https://github.com/apized/apized/actions/workflows/cicd.yml)

# Apized

[Apized](https://apized.org) is modern JVM-based framework designed to make API development easier. It aims to give
developers more time to focus on model definition and business logic implementation by automatically providing the
underlying implementations to easily create a rich REST API.

By adopting apized your API will provide:

- [Enhanced REST endpoints](#enhanced-rest-endpoints)
- [Audit trail](#audit-trail)
- [Event publishing](#event-publishing)
- [Behaviours](#behaviours)
- [OpenAPI documentation](#openapi-documentation) (optional)
- [Security](#security) (optional)
- [Federation](#federation) (optional)

and will run on top of:

- [Micronaut](#micronaut)
- [Spring Boot](#spring-boot) [Planned]

<!-- For more information on using Apized see the documentation at [apized.org](https://apized.org). -->

## Features

### Enhanced REST endpoints

With apized your endpoints offer quite a bit more that your standard REST endpoint. You can ask for what you need and
get exactly what you ask for, get many resources with a single request and make use of advanced filtering without having
to write a single line of code beyond your model.

GET `/organizations/3fedcab7-97b7-4f81-b49f-2a70864f7cfa`

```json
{
    "billing": null,
    "departments": [
        "010958ae-75b5-4e90-b161-58420a3820db",
        "a3a9d141-5614-4686-821e-f3c3e3ace530",
        "7217d08f-09a4-4ef7-b978-48a4e1c5079a"
    ],
    "employees": [
        "e9fa40a7-3044-4328-82e9-f710a0911452",
        "f0203a8f-cb3b-430b-a866-cdf7ea1ed730",
        "f33ebe50-7fe6-42d0-b7c4-56848c93607d",
        "b1b41793-cb41-4036-a3f8-93a06b219fea"
    ],
    "name": "Org A",
    "id": "3fedcab7-97b7-4f81-b49f-2a70864f7cfa",
    "version": 0,
    "createdBy": "d568705a-a581-4923-828b-0f97c0891163",
    "createdAt": 1675363907394,
    "lastUpdatedBy": null,
    "lastUpdatedAt": 1675363907394,
    "metadata": {}
}
```

GET `/organizations/3fedcab7-97b7-4f81-b49f-2a70864f7cfa/employee/e9fa40a7-3044-4328-82e9-f710a0911452`

```json
{
    "address": "5c0d9686-9f26-4d61-880e-47f99a2dbf03",
    "department": "a3a9d141-5614-4686-821e-f3c3e3ace530",
    "name": "Sen Eng",
    "age": 35,
    "position": "A Senior Engineer",
    "salary": 100000,
    "favoriteDoctor": "e9fa40a7-3044-4328-82e9-f710a0911452",
    "id": "e9fa40a7-3044-4328-82e9-f710a0911452",
    "version": 0,
    "createdBy": null,
    "createdAt": 1675363907394,
    "lastUpdatedBy": null,
    "lastUpdatedAt": 1675363907394,
    "metadata": {}
}
```

#### Field filtering

No more over or under fetching of data.

GET `/organizations/3fedcab7-97b7-4f81-b49f-2a70864f7cfa?fields=name`

```json
{
    "name": "Org A",
    "id": "3fedcab7-97b7-4f81-b49f-2a70864f7cfa"
}
```

### Smaller payloads

Send only what needs to change instead of having to send the whole object.

### Model drilling

Get related data with one single request. In case you're wondering, yes, we do model drilling for both queries and
mutations.

GET `fields=name,employees.name`

```json
{
    "employees": [
        {
            "name": "Sen Eng",
            "id": "e9fa40a7-3044-4328-82e9-f710a0911452"
        },
        {
            "name": "Jun Eng",
            "id": "f0203a8f-cb3b-430b-a866-cdf7ea1ed730"
        },
        {
            "name": "Sen Har",
            "id": "f33ebe50-7fe6-42d0-b7c4-56848c93607d"
        },
        {
            "name": "Jun Har",
            "id": "b1b41793-cb41-4036-a3f8-93a06b219fea"
        }
    ],
    "name": "Org A",
    "id": "3fedcab7-97b7-4f81-b49f-2a70864f7cfa"
}
```

### Searching

Because apized understands your model you get querying out of the box for any field. This also extends to models you
might be fetching via model drilling.

### Audit trail

By using apized, your model will automatically be put into an audit trail. This is achieved by storing a snapshot of the
object at the time of the interaction on the database. By default, the object shape is the exact same shape you would
get from calling the api itself. In order to provide flexibility you can use the `@AuditField` and `@AuditIgnore` to
manipulate what this snapshot will look like on your database.

### Event publishing

By using apized, your model will automatically be put into an ESB. This is achieved by generating a snapshot of the
object at the time of the interaction. By default, the object shape is the exact same shape you would
get from calling the api itself. In order to provide flexibility you can use the `@EventField` and `@EventIgnore` to
manipulate what this snapshot will look like on your ESB message.

### Behaviours

Apized follows the Controller-Service-Repository pattern. These layers are auto-generated for you at
compile time. You can extend the generated service and repository layers by implementing extensions.
The behaviours form a request execution pipeline. Behaviours encapsulate a given business requirement in its own class
and leaves nice and isolated.

### OpenAPI documentation

Our generated Controllers are annotated with OpenAPI annotations and therefore you get openapi documentation out of the
box.

### Security

Our security is based on permissions to allow our users to have control as fine-grained or coarse grained as they wish.

Permissions where designed to follow the following pattern [project].[model].[action].[id].[field].[value] where the
create action excludes the [id] section since there’s no id to target. These permissions support wildcards
sample.organization.update.*.name - this would mean the user can update the name of any organization.

Some examples:

| Permission                                                             | description                                                                                                                                                         |
|------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `*`                                                                    | God permission, can to anything on any service                                                                                                                      |
| `sample`                                                               | Access to do everything on anything in the sample service                                                                                                           |
| `sample.organization`                                                  | Can do all operations on all organizations                                                                                                                          |
| `sample.organization.create`                                           | Can create organizations                                                                                                                                            |
| `sample.organization.update.4b949872-f9ec-489f-8f4c-613b7a9c1ecf`      | Can update organization with id `4b949872-f9ec-489f-8f4c-613b7a9c1ecf`                                                                                              |
| `sample.organization.update.4b949872-f9ec-489f-8f4c-613b7a9c1ecf.name` | Can update only the name of organization `4b949872-f9ec-489f-8f4c-613b7a9c1ecf`                                                                                     |
| `sample.address.update.*.country.PT`                                   | Can update the country of any address to `PT` but not set any other country (unless either a broader permission or permission for another country is also provided) |

Permissions are evaluated for each affected model (and field) when a request is made. If, for example, you send the
payload of an organization with an employee in it both `sample.organization.create` and `sample.employee.create` will be
verified.

### Federation

Every instance of every server becomes an API Gateway. The only thing to bear in mind is that you must request the root
object of your query to the servers that contain that model directly.

## Engines

### Micronaut

[Micronaut](https://micronaut.io/) is the default underlying engine for this framework. The advantages of micronaut
provides that make it our default choice is the reflection free approach which will drastically improve startup times.
Apized is also fully compatible with native compilation, which means you can deploy light-weight ulta-performant native
binaries.

### Spring Boot

[Spring Boot](https://spring.io/projects/spring-boot) is something we'd like to add in the future but we'd need the time
and resources to do so. This implementation will probably only happen if this projects gets wider adoption.

## Example applications

Example applications can be found [here](https://github.com/apized/samples).
