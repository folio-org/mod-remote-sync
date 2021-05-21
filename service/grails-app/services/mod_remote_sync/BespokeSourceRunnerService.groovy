package mod_remote_sync

import grails.gorm.transactions.Transactional
import mod_remote_sync.BespokeSource
import com.k_int.web.toolkit.settings.AppSetting
import mod_remote_sync.source.RemoteSyncActivity
import mod_remote_sync.source.RecordSourceController
import mod_remote_sync.source.DynamicClassLoader

@Transactional
class BespokeSourceRunnerService implements RecordSourceController {

  public String getAppSetting(String setting) {
    return AppSetting.findByKey(setting)?.value
  }

  public void start(mod_remote_sync.BespokeSource src) {
    log.debug("BespokeSourceRunnerService::start(${src})");
    log.debug("url setting: ${getAppSetting('url')}");

    RemoteSyncActivity rsa = getGroovyScript(src.script,RemoteSyncActivity.class)
    rsa.getNextBatch([:], this)

    log.debug("BespokeSourceRunnerService::start complete for ${src}");
  }

  private Class getGroovyScript(String code, Class required_interface) {

    Class result = false;

    try {
      // Parse the class
      result = new DynamicClassLoader().parseClass(code)
      log.debug("Got class ${clazz}");

      if ( required_interface ) {
        if ( required_interface.isAssignableFrom(clazz) ) {
          log.debug("${clazz.getName()} implements RemoteSyncActivity interface");
        }
        else {
          log.warn("Acquired class ${clazz} does not implement ${required_interface}.. Skip");
          throw new RuntimeException('Plugin class does not implement '+required_interface?.name)
        }
      }
    }
    catch ( Exception e ) {
      log.error("Error",e);
    }

    return result;
  }


  public void updateState(String source_id, Map state) {
  }

  public void upsertSourceRecord(String source_id,
                                 String resource_id,
                                 String resource_type,
                                 byte[] record) {
  }

}
