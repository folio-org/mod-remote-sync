import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j

@Slf4j
public class ProcessLaserLicense implements TransformProcess {

  public Map preflightCheck(Map input_record,
                            ApplicationContext ctx,
                            Map local_context) {
    log.debug("ProcessLaserLicense::preflightCheck()");
    return [:]
  }

  public Map process(Map input_record,
                     ApplicationContext ctx,
                     Map local_context) {
    return [:]
  }

}
