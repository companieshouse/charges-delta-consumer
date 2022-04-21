Feature: Process Charges Delta information

  Scenario Outline: Consume the message and process it and call charges data api

    Given Charges delta consumer service is running
    When a message with payload "<deltaMessage>" is published to topic
    Then the Consumer should process and send a request with payload "<apiRequest>" to the Charges Data API getting back 200

  Examples:
    | deltaMessage                | apiRequest                           |
    | charges-delta-source-2.json | internal-charges-api-expected-2.json |