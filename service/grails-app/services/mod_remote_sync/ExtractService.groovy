package mod_remote_sync

import grails.gorm.transactions.Transactional
import mod_remote_sync.source.RecordSourceController

@Transactional
class ExtractService implements RecordSourceController {

  public void processSources() {
    log.debug("SourceProcessingService::processSources()");
    Source.executeQuery('select s.id from Source as s').each { src ->
      processSource(src);
    }
  }

  public void processSource(String id) {
    log.debug("SourceProcessingService::processSource(${id})");
  }

  public void update(String source, byte[] record, Map state) {
    log.debug("SourceProcessingService::update(${source},...,${state})");
  }
}
