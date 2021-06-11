import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j

@Slf4j
public class ProcessLaserLicense implements TransformProcess {

  public Map preflightCheck(Map input_record,
                            ApplicationContext ctx,
                            Map local_context) {
    log.debug("ProcessLaserLicense::preflightCheck()");
    Map result = [
      preflightStatus:'FAIL'
    ]

    // We don't create licenses unless a user has told us to create a new one, or which license we should map
    // to. If we don't know - feedback that we need to know which license to map to, to create new, or not to map
    // If we know, the process can continue to process

    // manualCheckOnCreateResource()
    return [:]
  }

  public Map process(Map input_record,
                     ApplicationContext ctx,
                     Map local_context) {
    return [:]
  }

}
