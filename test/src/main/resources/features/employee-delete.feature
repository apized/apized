Feature: Delete Employees

  Background:
    Given I login as admin
    Given I create an organization with
      | name    | Org 1                                                                                                                                  |
      | billing | [ companyName: 'Org 2 LTD', vatNumber: 'PT123456789', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] ] |
    And I create an employee as dude with
      | name           | Guy 1                                                                 |
      | address        | [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] |
      | favoriteDoctor | [ id: 'c10ec298-aa94-4f1d-8cd2-851b6cd7bcde', catalogopolisId: 1 ]    |

  Scenario: Can delete employee
    When I delete an employee with id ${dude.id}
    Then the request succeeds
    And I get an employee with id ${dude.id}
    And the request fails
    And the response path "errors" contains element with
      | message | Not Found |
