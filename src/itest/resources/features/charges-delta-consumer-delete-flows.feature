Feature: Process Charges Delete Delta information with happy and error conditions

  Scenario Outline: Consume a valid delete message, process it and charges data api delete endpoint is invoked with the
  correct charge id extracted from the payload

    Given Charges delta consumer service for delete is running
    And Stubbed Charges Data API delete endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When delete message with payload "<deltaMessage>" is published to charges topic
    Then Charges Data API delete endpoint is only invoked once getting back "<responseCode>"

    Examples:
      | response |  deltaMessage                       | companyNumber | chargeId                    | responseCode |
      | 200      |  charges-delete-delta-source-1.json | 0             | NLVXY861zxOTr3NExemI3q4Nq4Y | 200          |


  Scenario Outline: Consume a valid Charges Delete message in avro format message with an invalid json payload,
  process it and move to invalid message topic charges data api never called

    Given Charges delta consumer service for delete is running
    And Stubbed Charges Data API delete endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When delete message with payload "<deltaMessage>" is published to charges topic
    Then delete message should be moved to topic "<targetTopic>"
    And Charges Data API delete endpoint is never invoked

    Examples:
      | response | targetTopic                                  | deltaMessage                             | companyNumber | chargeId                    |
      | 200      | charges-delta-charges-delta-consumer-invalid | charges-delete-delta-source-invalid.json | 0             | NLVXY861zxOTr3NExemI3q4Nq4Y |


  Scenario Outline: Consume a valid delete message, Charges Data API delete endpoint returns 400

    Given Charges delta consumer service for delete is running
    And Stubbed Charges Data API delete endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When delete message with payload "<deltaMessage>" is published to charges topic
    Then delete message should be moved to topic "<targetTopic>"
    And Charges Data API delete endpoint is only invoked once getting back "<responseCode>"

    Examples:
      | response | deltaMessage                       | companyNumber | chargeId                    | responseCode | targetTopic                                  |
      | 400      | charges-delete-delta-source-1.json | 0             | NLVXY861zxOTr3NExemI3q4Nq4Y | 400          | charges-delta-charges-delta-consumer-invalid |

  Scenario Outline: Consume a valid delete message, Charges Data API delete endpoint returns 503

    Given Charges delta consumer service for delete is running
    And Stubbed Charges Data API delete endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When delete message with payload "<deltaMessage>" is published to charges topic
    Then delete message should be retried "<retries>" on retry topic and moved to "<targetTopic>"
    And Charges Data API delete endpoint is retried "<retries>" getting back "<responseCode>"

    Examples:
      | response | deltaMessage                       | companyNumber | chargeId                    | responseCode | targetTopic                                | retries |
      | 503      | charges-delete-delta-source-1.json | 0             | NLVXY861zxOTr3NExemI3q4Nq4Y | 503          | charges-delta-charges-delta-consumer-error | 4       |

  Scenario Outline: Consume a valid delete message, but fails while processing before calling Charges Data API delete endpoint

    Given Charges delta consumer service for delete is running
    And Stubbed Charges Data API delete endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When delete message with payload "<deltaMessage>" is published to charges topic
    Then delete message should be retried "<retries>" on retry topic and moved to "<targetTopic>"
    And Charges Data API delete endpoint is retried "<retries>" getting back "<responseCode>"

    Examples:
      | response | deltaMessage                       | companyNumber | chargeId                    | responseCode | targetTopic                                | retries |  |
      | 500      | charges-delete-delta-source-1.json | 0             | NLVXY861zxOTr3NExemI3q4Nq4Y | 500          | charges-delta-charges-delta-consumer-error | 4       |  |

  Scenario Outline: Consume a Charges Delete message null/empty charge id,
  process it and move to invalid message topic charges data api never called

    Given Charges delta consumer service for delete is running
    And Stubbed Charges Data API delete endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When delete message with payload "<deltaMessage>" is published to charges topic
    Then delete message should be moved to topic "<targetTopic>"
    And Charges Data API delete endpoint is never invoked

    Examples:
      | response | targetTopic                                  | deltaMessage                                     | companyNumber | chargeId                    |
      | 200      | charges-delta-charges-delta-consumer-invalid | charges-delete-delta-source-null_charge-id.json  | 0             | NLVXY861zxOTr3NExemI3q4Nq4Y |
      | 200      | charges-delta-charges-delta-consumer-invalid | charges-delete-delta-source-empty_charge-id.json | 0             | NLVXY861zxOTr3NExemI3q4Nq4Y |