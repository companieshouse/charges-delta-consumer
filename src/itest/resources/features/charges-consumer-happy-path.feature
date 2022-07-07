Feature: Process Charges Delta information

  Scenario Outline: Consume the message and process it and call charges data api

    Given Charges delta consumer service is running
    When a message with payload "<deltaMessage>" is published to topic
    Then the Consumer should process and send a request with payload "<apiRequest>" to the Charges Data API getting back 200

  Examples:
    | deltaMessage                                         | apiRequest                                                  |
    | charges-delta-source-2.json                          | internal-charges-api-expected-2.json                        |
    | Additional_notices_Happy_Path.json                   | Additional_notices_Happy_Path_output_correct_body.json      |
    | alterations_to_order_alteration_to_prohibitions.json | alterations_to_order_alteration_to_prohibitions_output.json |
    | assets_ceased_released_Happy_Path.json               | assets_ceased_released_output.json                          |
    | created_on_Happy_Path.json                           | created_on_output.json                                      |
    | floating_charge_Happy_Path.json                      | floating_charge_output.json                                 |
    | Insolvency_cases_Happy_Path.json                     | Insolvency_cases_Happy_Path_output.json                     |
    | More_than_4_persons_Y_Happy_Path.json                | More_than_4_persons_Y_Output.json                           |
    | obligations_secured_nature_of_charge_Happy_Path.json | obligation_secured_nature_of_charge_Happy_Path_output.json  |
    | satisfied_on_Happy_Path.json                         | satisfied_on_Happy_Path_output.json                         |
    | Scottish_Alterations.json                            | Scottish_Alterations_output.json                            |
    | Notice_type_value_with_empty_space_Happy_Path.json   | Additional_notices_Happy_Path_output_correct_body.json      |
    | charges-delta-source-empty-dates.json                | internal-charges-api-empty-dates-expected.json              |
    | charges-delta-source-3.json                          | internal-charges-api-expected-3.json                        |
    | charges-delta-source-19.json                         | internal-charges-api-expected-19.json                       |
    | charges-delta-source-20.json                         | internal-charges-api-expected-20.json                       |
    | Additional_notices_No_Trans_Id.json                  | Additional_notices_No_Trans_Id_output.json                  |




    










