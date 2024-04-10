/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
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
            {"id":"111", "status":"OK"},
            {"id":"112", "status":"ERROR"}
            ]
        }
        """;

  public static final String jobExample2 =
      """
        {
        "id":12,
        "samples": [
            {"id":"121", "name":"sample A"},
            {"id":"122", "name":"sample B"}
            ]
        }
      """;

  public static final String resultFilteredList1 =
      """
      {
      "id":"111", "note": "result note 1"
      }
    """;

  public static final String resultFilteredList2 =
      """
      {
      "id":112, "note": "other result note"
      }
    """;

  public static final String resultVariantFilteredList =
      """
      "id":111, "note":"any other note"
    """;
}
