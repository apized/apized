Feature: Create Departments

  Background:
    Given I login as admin
    And I create an organization with
      | name    | Org 1                                                                                                                                  |
      | billing | [ companyName: 'Org 2 LTD', vatNumber: 'PT123456789', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] ] |

  Scenario: Can create a full structure of departments in one go top-down
    When I create a department as dep with
      | name     | 1                                              |
      | children | [ [ name: '2', children: [ [ name: '3' ] ] ] ] |
    Then the request succeeds
    And the responses are expanded to contain children
    And the responses are expanded to contain children.children
    And I get a department with id ${dep.id}
    And the response contains
      | name | 1 |
    And the response path "children" contains element with
      | name | 2 |
    And the response path "children.0.children" contains element with
      | name | 3 |

  Scenario: Can create a full structure of departments in one go bottom-up
    When I create a department as dep with
      | name   | 3                                    |
      | parent | [ name: '2', parent: [ name: '1' ] ] |
    Then the request succeeds
    And the responses are expanded to contain parent
    And the responses are expanded to contain parent.parent
    And I get a department with id ${dep.id}
    And the request succeeds
    And the response contains
      | name | 3 |
    And the response path "parent" contains
      | name | 2 |
    And the response path "parent.parent" contains
      | name | 1 |

  Scenario: Can create a department with inlined employees
    When I create a department as dep with
      | name      | Department                                                                                                                                                                                                 |
      | employees | [ [ name: 'Guy 1', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ]  ], [ name: 'Guy 2', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ]  ] ] |
    Then the request succeeds
    And the responses are expanded to contain employees
    And I get a department with id ${dep.id}
    And the request succeeds
    And the response contains
      | name | Department |
    And the response path "employees" contains 2 elements
    And the response path "employees" contains element with
      | name | Guy 1 |
    And the response path "employees" contains element with
      | name | Guy 2 |