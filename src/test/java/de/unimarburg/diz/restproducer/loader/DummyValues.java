/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.loader;

import de.unimarburg.diz.restproducer.manager.EtlManager.StringPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
            {"id":"111", "status":"OK", "name":"sample A"},
            {"id":"112", "status":"OK", "name":"sample B"}
            ]
        }
        """;

  public static final String jobExample2 =
      """
        {
        "id":12,
        "samples": [
            {"id":"121", "name":"sample C"},
            {"id":"122", "name":"sample D"}
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

  public static final ConcurrentMap<String, List<List<StringPair>>> getDummyGlobalParams() {
    var globalParam = new ConcurrentHashMap<String, List<List<StringPair>>>();
    final ArrayList<List<StringPair>> endpointAParallelParams = new ArrayList<>();
    endpointAParallelParams.add(
        List.of(
            new StringPair("e1_p11", "v11"),
            new StringPair("e1_p12", "v12"),
            new StringPair("e1_p13", "v13")));
    endpointAParallelParams.add(
        List.of(
            new StringPair("e1_p21", "v21"),
            new StringPair("e1_p22", "v22"),
            new StringPair("e1_p23", "v23")));
    endpointAParallelParams.add(
        List.of(
            new StringPair("e1_p31", "v31"),
            new StringPair("e1_p32", "v32"),
            new StringPair("e1_p33", "v33")));
    globalParam.put("endpointA", endpointAParallelParams);

    final ArrayList<List<StringPair>> endpointBParallelParams = new ArrayList<>();
    endpointBParallelParams.add(
        List.of(
            new StringPair("e2_p11", "v11"),
            new StringPair("e2_p12", "v12"),
            new StringPair("e2p13", "v13")));
    endpointBParallelParams.add(
        List.of(
            new StringPair("e2_p21", "v21"),
            new StringPair("e2_p22", "v22"),
            new StringPair("e2p23", "v23")));
    endpointBParallelParams.add(
        List.of(
            new StringPair("e2_p31", "v31"),
            new StringPair("e2_p32", "v32"),
            new StringPair("e2p33", "v33")));
    globalParam.put("endpointB", endpointAParallelParams);
    return globalParam;
  }
}
