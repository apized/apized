Feature: Update Organizations

  Background:
    Given I login as admin
    And I create an organization as org with
      | name    | Org 1                                                                                                                                  |
      | billing | [ companyName: 'Org 1 LTD', vatNumber: 'PT123456789', address: [ line1 : 'Street', city: 'City', country: 'PT', postalCode: '3000' ] ] |

  Scenario: Can update organizations
    When I update an organization with id ${org.id} with
      | name | Org 2 |
    Then the request succeeds
    And the response contains
      | name | Org 2 |
