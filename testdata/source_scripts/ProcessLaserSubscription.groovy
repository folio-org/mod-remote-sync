import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j

@Slf4j
public class ProcessLaserSubscription implements TransformProcess {

  public Map preflightCheck(String resource_id,
                            byte[] input_record,
                            ApplicationContext ctx,
                            Map local_context) {
    return [
      preflightStatus:'FAIL'  // FAIL|PASS
    ]
  }

  public Map process(String resource_id,
                     byte[] input_record,
                     ApplicationContext ctx,
                     Map local_context) {
    return [
      processStatus:'FAIL'   // FAIL|COMPLETE
    ]
  }

}
