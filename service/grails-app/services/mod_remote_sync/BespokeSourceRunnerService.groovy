package mod_remote_sync

import grails.gorm.transactions.Transactional
import mod_remote_sync.BespokeSource
import com.k_int.web.toolkit.settings.AppSetting
import mod_remote_sync.source.RemoteSyncActivity
import mod_remote_sync.source.RecordSourceController
import mod_remote_sync.source.DynamicClassLoader
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

@Transactional
class BespokeSourceRunnerService implements RecordSourceController {

  public String getAppSetting(String setting) {
    return AppSetting.findByKey(setting)?.value
  }

  public void start(mod_remote_sync.BespokeSource src) {
    log.debug("BespokeSourceRunnerService::start(${src})");
    log.debug("url setting: ${getAppSetting('laser.url')}");

    try {
      RemoteSyncActivity rsa = getGroovyScript(src.script,RemoteSyncActivity.class).getDeclaredConstructor().newInstance()

      Map state_info = null;

      // If the state info has been serialised into a JSON object, parse it here
      if ( src.stateInfo != null ) {
        def jsonSlurper = new JsonSlurper()
        state_info = jsonSlurper.parseText(src.stateInfo);
      }

      // the RemoteSyncActivity should update state_info in place if it wants values remembered for the next run
      rsa.getNextBatch(src.id, state_info, this)

      updateState(src.id, state_info);
    }
    catch ( Throwable t ) {
      log.error("Exception processing remote sync activity",t);
    }
    finally {
      log.info("BespokeSourceRunnerService::start complete");
    }

    log.debug("BespokeSourceRunnerService::start complete for ${src}");
  }

  private Class getGroovyScript(String code, Class required_interface) {

    Class result = null;

    try {
      // Parse the class
      result = new DynamicClassLoader().parseClass(code)
      log.debug("Got class ${result}");

      if ( required_interface ) {
        if ( required_interface.isAssignableFrom(result) ) {
          log.debug("${result.getName()} implements RemoteSyncActivity interface");
        }
        else {
          log.warn("Acquired class ${result} does not implement ${required_interface}.. Skip");
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
    log.debug("BespokeSourceRunnerService::updateState(${source_id},${state})");
    try {
      Source.executeUpdate('update Source s set s.stateInfo = :s where s.id=:id',[s:JsonOutput.toJson(state),id:source_id]);
    }
    catch ( Exception e ) {
      log.error("Error updating state info for source ${source_id}",e);
    }
  }

  public void upsertSourceRecord(String source_id,
                                 String authority,
                                 String resource_id,
                                 String resource_type,
                                 String hash,
                                 byte[] record) {
    upsertSourceRecord(source_id,authority,resource_id,resource_type,"${authority}:${resource_type}/${resource_id}",hash,record);
  }

  public void upsertSourceRecord(String source_id,
                                 String authority,
                                 String resource_id,
                                 String resource_type,
                                 String label,
                                 String hash,
                                 byte[] record) {
    log.debug("BespokeSourceRunnerService::updateState(${source_id},${resource_id},${resource_type},${hash},...)");
    Authority a = Authority.findByName(authority) ?: new Authority(name:authority).save(flush:true, failOnError:true)
    SourceRecord existing_record = SourceRecord.findByResourceUriAndAuth(resource_id,a)
    if ( existing_record == null ) {
      log.debug("No existing source record - creating ${resource_id}/hash:${hash}");
      existing_record = new SourceRecord(auth:a,
                                         resourceUri: resource_id,
                                         dateCreated: new Date(),
                                         lastUpdated: new Date(),
                                         recType: resource_type,
                                         record: record,
                                         checksum: hash,
                                         label: label,
                                         seqts: System.currentTimeMillis(),
                                         owner:Source.get(source_id))
      log.debug("Saving new source record: ${resource_id}");
      existing_record.save(flush:true, failOnError:true);
      log.debug("Made new source record: ${existing_record}");
    }
    else {
      log.debug("Found existing source record - creating ${resource_id}/hash:${hash} vs ${existing_record.checksum}");
      if ( existing_record.checksum != hash ) {
        log.debug("Checksum different - updating record");
        existing_record.record = record;
        existing_record.checksum = hash;
        existing_record.seqts = System.currentTimeMillis();
        existing_record.save(flush:true, failOnError:true);
      }
    }
  }

}
