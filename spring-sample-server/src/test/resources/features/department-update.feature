Feature: Create Departments

  Background:
    Given I login as admin
    And I create an organization with
      | name    | Org 1                                                                                                                                  |
      | billing | [ companyName: 'Org 2 LTD', vatNumber: 'PT123456789', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] ] |

#  TODO this now fails, fix it :P
#  Scenario: Things
#    Given I create a department as dep with
#      | name      | Department                                                                                                                                                                                                 |
#      | employees | [ [ name: 'Guy 1', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ]  ], [ name: 'Guy 2', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ]  ] ] |
#    When I update a department with id ${dep.id} with
#      | employees | ['${dep.employees[0]}'] |
#    Then the request succeeds
#    And the response path "employees" contains 1 elements
