/* GNU AFFERO GENERAL PUBLIC LICENSE  Version 3 (C)2024 Datenintegrationszentrum Fachbereich Medizin Philipps Universität Marburg */
package de.unimarburg.diz.restproducer.manager;

import de.unimarburg.diz.restproducer.loader.RestLoader;
import de.unimarburg.diz.restproducer.sender.KafkaSender;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EtlManager {

  private final RestLoader loader;
  private final KafkaSender sender;

  @Autowired
  public EtlManager(RestLoader loader, KafkaSender sender) {
    this.loader = loader;
    this.sender = sender;
  }

  public void execute() {

    /*
    1. prep config level order
    2. begin at root and descend
    3. call all current level endpoints and produce data into target topic
    */

    // 1
    // var tree = LoaderUtil.getNArrayTree(config);
    // var levelOrderedElements = LoaderUtil.getAllNodesAsList(tree).reversed();

    // 2
    var paramMap = new HashMap<String, String>();

    // 3
  }

  /**
   * @implSpec execute like cron job: load data and pump it into target topics.
   *     <ul>
   *       <li>First version do full load every time! </item>
   *       <li>Final version will likely need an internal offset management, since we do not want to
   *           make full load operations every time.</item>
   *     </ul>
   *
   * @return <c>true</c> if execution was successful
   */
  public boolean transferRestDataToKafka() {
    throw new UnsupportedOperationException("not implemented, yet!");
  }
}
