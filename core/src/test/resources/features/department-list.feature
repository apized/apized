Feature: List Departments

  Background:
    Given I login as admin
    And I create an organization with
      | name    | Org 1                                                                                                                                  |
      | billing | [ companyName: 'Org 2 LTD', vatNumber: 'PT123456789', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] ] |
    And I create a department with
      | name | General 1 |
    And I create a department with
      | name | General 2 |

  Scenario: Can list departments
    When I list the departments
    Then the request succeeds
    And the response path "content" contains 2 elements
    And the response path "content" contains element with
      | name | General 1 |
    And the response path "content" contains element with
      | name | General 2 |
