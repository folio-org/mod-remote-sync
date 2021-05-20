package mod_remote_sync

import grails.gorm.transactions.Transactional
import mod_remote_sync.BespokeSource

@Transactional
class BespokeSourceRunnerService {

  public void start(mod_remote_sync.BespokeSource src) {
    log.debug("BespokeSourceRunnerService::start(${src})");
  }
}
