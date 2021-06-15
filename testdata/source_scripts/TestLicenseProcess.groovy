import mod_remote_sync.source.TransformProcess;

import org.springframework.context.ApplicationContext
import groovy.util.logging.Slf4j
import mod_remote_sync.ResourceMappingService
import mod_remote_sync.ResourceMapping

@Slf4j
public class TestLicenseProcess implements TransformProcess {

  public Map preflightCheck(String resource_id,
                            byte[] input_record,
                            ApplicationContext ctx,
                            Map local_context) {
    log.debug("ProcessTestLicense::preflightCheck()");

    ResourceMappingService rms = ctx.getBean('resourceMappingService');

    if ( rms ) {
      log.debug("Got mapping service");
      ResourceMapping rm = rms.lookupMapping('TEST',resource_id,'TEST');
      if ( rm ) {
        log.info("Located mapping");
      }
      else {
        log.info("No mapping");
      }
    }
    else {
      log.debug("Failed to obtain mapping service");
    }

    Map result = [
      preflightStatus:'FAIL'
    ]

    // We don't create licenses unless a user has told us to create a new one, or which license we should map
    // to. If we don't know - feedback that we need to know which license to map to, to create new, or not to map
    // If we know, the process can continue to process

    // manualCheckOnCreateResource()
    return [:]
  }

  public Map process(String resource_id,
                     byte[] input_record,
                     ApplicationContext ctx,
                     Map local_context) {
    return [:]
  }

}

