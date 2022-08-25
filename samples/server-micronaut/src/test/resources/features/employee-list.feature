Feature: List Employees

  Background:
    Given I login as admin
    Given I create an organization with
      | name    | Org 1                                                                                                                                  |
      | billing | [ companyName: 'Org 2 LTD', vatNumber: 'PT123456789', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] ] |
    And I create an employee with
      | name           | Guy 1                                                                 |
      | address        | [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] |
      | favoriteDoctor | [catalogopolisId: 1]                                                  |
    And I create an employee with
      | name    | Guy 2                                                                 |
      | address | [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] |

  Scenario: Can list employees
    When I list the employees
    Then the request succeeds
    And the response path "content" contains 2 elements
    And the response path "content" contains element with
      | name | Guy 1 |
    And the response path "content" contains element with
      | name | Guy 2 |

  Scenario: Can list employees with federated favoriteDoctor
    Given the responses are expanded to contain favoriteDoctor
    When I list the employees
    Then the request succeeds
    And the response path "content" contains 2 elements
    And the response path "content" contains element with
      | favoriteDoctor | [ incarnation: 'First Doctor' ] |
