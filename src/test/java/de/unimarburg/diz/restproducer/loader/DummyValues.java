/* GNU AFFERO GENERAL PUBLIC LICENSE Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

public class DummyValues {

  public static final String jobsExample1 =
      """
        {
        "jobs": [
            {
                "id":1,
                "job_id": "11",
                "status": "COMPLETED"
            },
            {
                "id":2,
                "job_id": "12",
                "status": "COMPLETED"
            }
            ]
        }
        """;

  public static final String jobExample1 =
      """
        {
        "id":11,
        "samples": [
            {"id":"111", "status":"OK", "sampleId":"HA2345-24_R1_S001", "name":"sample A"},
            {"id":"112", "status":"OK", "sampleId":"HA2346-24_R1_S001" ,"name":"sample B"}
            ]
        }
        """;

  public static final String jobExample2 = """
        {
        "id":12,
        }
      """;

  public static final String resultFilteredList1 =
      """
      {
      "id":"111", "sampleId": "111" ,"note": "filtered"
      }
    """;

  public static final String resultFilteredList2 =
      """
      {
      "id":112, "sampleId": "111" ,"note": "filtered2"
      }
    """;

  public static final String resultCNVFilteredList =
      """
      {"id":111, "sampleId": "111", "note":"CNV"}
    """;
  public static final String resultCNVFilteredList2 =
      """
      {"id":112, ,"sampleId": "111",  "note":"CNV2"}
    """;

  public static final String sample_1 =
      """
      {"id":"111", "status":"OK", "sampleId":"HA2345-24_R1_S001", "name":"sample A"}
      """;
}
