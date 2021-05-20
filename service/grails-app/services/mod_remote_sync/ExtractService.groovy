package mod_remote_sync

import grails.gorm.transactions.Transactional
import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.HttpBuilder
import grails.converters.JSON
import mod_remote_sync.source.DynamicClassLoader
import grails.databinding.SimpleMapDataBindingSource 
import java.security.MessageDigest
import com.k_int.web.toolkit.refdata.RefdataValue
import mod_remote_sync.source.RemoteSyncActivity
import mod_remote_sync.source.TransformProcess

@Transactional
class ExtractService {

  def grailsApplication

  def start() {
    log.debug("ExtractService::start()");
    Source.list(readOnly:true).each { src ->
      log.debug("Process source ${src} - service to use is ${src.getHandlerServiceName()}");
      def runner_service = grailsApplication.mainContext.getBean(src.getHandlerServiceName())
      log.debug("Got runner service: ${runner_service}");
      runner_service.start(src);
    }
  }
}
