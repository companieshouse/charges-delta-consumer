Feature: Process Charges Delta information with error conditions

  Scenario Outline: Consume a non-avro message, process it and move to invalid message topic charges data api never called

    Given Charges delta consumer service is running
    And Stubbed Charges Data API endpoint will return "<response>" http response code
    When A non-avro format message is sent to the Kafka topic
    Then the message should be moved to topic "<targetTopic>"
    And Charges Data API endpoint is never invoked

    Examples:
      | response    | targetTopic                                    |
      | 200         | charges-delta-charges-delta-consumer-invalid   |


  Scenario Outline: Consume a valid message, process it and move to invalid message topic charges data api never called
    Given Charges delta consumer service is running
    And Stubbed Charges Data API endpoint will return "<response>" http response code
    When A valid avro message in with an invalid json payload is sent to the Kafka topic
    Then the message should be moved to topic "<targetTopic>"
    And Charges Data API endpoint is never invoked

    Examples:
      | response    | targetTopic                                    |
      | 200         | charges-delta-charges-delta-consumer-invalid   |

  Scenario Outline: Consume a valid message, Charges Data API endpoint returns 400
    Given Charges delta consumer service is running
    And Stubbed Charges Data API endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When a message with payload "<deltaMessage>" is published to charges topic
    Then the message should be moved to topic "<targetTopic>"
    And Charges Data API endpoint is only invoked once

    Examples:
      | response    | targetTopic                                    | deltaMessage                     | companyNumber | chargeId                    |
      | 400         | charges-delta-charges-delta-consumer-invalid   | satisfied_on_Happy_Path.json     | OC342023      | TnYWNS5p1GdMPVGvNXIx63D5Uc8 |

  Scenario Outline: Consume a valid message, Charges Data API endpoint returns 503
    Given Charges delta consumer service is running
    And Stubbed Charges Data API endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When a message with payload "<deltaMessage>" is published to charges topic
    Then the message should be moved to topic "<targetTopic>"
    And Charges Data API endpoint is retried "<retries>"

    Examples:
      | response    | targetTopic                                  | deltaMessage                     | companyNumber | chargeId                    | retries |
      | 503         | charges-delta-charges-delta-consumer-error   | satisfied_on_Happy_Path.json     | OC342023      | TnYWNS5p1GdMPVGvNXIx63D5Uc8 | 3       |

  Scenario Outline: Consume a valid message with null charges in payload
    Given Charges delta consumer service is running
    And Stubbed Charges Data API endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When a message with payload without charges is published to charges topic
    Then the message should be retried "<retries>" on retry topic "<targetTopic>"
    And Charges Data API endpoint is never invoked

    Examples:
      | response    | targetTopic                                  | retries | companyNumber | chargeId                    |
      | 200         | charges-delta-charges-delta-consumer-error   | 3       | OC342023      | TnYWNS5p1GdMPVGvNXIx63D5Uc8 |

  Scenario Outline: Consume a message, with null/empty charge id
    Given Charges delta consumer service is running
    And Stubbed Charges Data API endpoint will return "<response>" http response code for "<companyNumber>" and "<chargeId>"
    When a message with payload "<deltaMessage>" is published to charges topic
    Then the message should be moved to topic "<targetTopic>"
    And Charges Data API endpoint is never invoked

    Examples:
      | response | targetTopic                                  | deltaMessage                              | companyNumber | chargeId                    |
      | 200      | charges-delta-charges-delta-consumer-invalid | charges-delta-source-null-charge-id.json  | NI622400      | TnYWNS5p1GdMPVGvNXIx63D5Uc8 |
      | 200      | charges-delta-charges-delta-consumer-invalid | charges-delta-source-empty-charge-id.json | NI622400      | TnYWNS5p1GdMPVGvNXIx63D5Uc8 |
