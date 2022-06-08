# apized

The apized project's goal is to provide server developers with the constructs for providing all the 
features usually present in servers in order to allow the developers to focus on the modelling and business logic.

In order to keep our business logic clean and concise we have introduced an execution pipeline implementation which 
we call behaviours. These behaviours should be kept small and each should cater to a specific business need.
They can be defined to execute before/after any of the typical server-side MVC flow (Controller, Service & Repository) 
and can have a specific order (thus establishing a pipeline of execution).

To do so we use a regular MVC style pattern with a Controller Service and Repository layers. You are welcome to write these
yourself on top of the abstractions we have already provided for you but 

**_NOTE:_** This project is experimental and in its early days. 

## Features

### Included

### model
### behaviour
### error
### execution
### mvc
### serde

### Optional

### audit
### event
### federation
### search
### security

## Known Issues

- Right now, we only support OneToOne, OneToMany and ManyToOne relationships. If a
  ManyToMany relationship is needed you will have to declare the joining table as an entity with
  OneToMany relations to both sides of the ManyToMany relation.
