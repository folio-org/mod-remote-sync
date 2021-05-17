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

@Transactional
class SourceRegisterService {

  def grailsWebDataBinder

  def load(String url) {
    log.debug("Load: ${url}");

    HttpBuilder http_client = HttpBuilder.configure {
      request.uri = url
      // request.uri.query = [name: 'Bob']
      // request.cookie('user-session', 'SDF@$TWEFSDT', new Date()+30)
      request.contentType = 'application/json'
      request.accept = ['application/json']
    }

    Object response_content = http_client.get()

    if ( response_content != null ) {
      def parsed_register = JSON.parse(response_content)
      if ( parsed_register ) {
        parsed_register.each { entry ->
          switch ( entry.recordType ) {
            case 'source':
              processSourceEntry(entry)
              break;
            default:
              log.warn("Unhandled record type: ${entry}");
          }
        }
      }
    }
  }

  private void processSourceEntry(Map agent_descriptor) {
    println("Got agent descriptor: ${agent_descriptor}");
    switch ( agent_descriptor.packaging ) {
      case 'script':
        processScript(agent_descriptor);
        break;
      default:
        log.warn("unhandled packaging: ${agent_descriptor.packaging}");
        break;
    }
  }

  private void processScript(Map agent_descriptor) {
    log.debug("processScript ${agent_descriptor.sourceName} ${agent_descriptor.sourceUrl} ${agent_descriptor.parameters}");
    if ( ( agent_descriptor.sourceUrl ) &&
         ( agent_descriptor.authority ) && 
         ( agent_descriptor.sourceName ) ) {
      HttpBuilder plugin_fetch_agent = HttpBuilder.configure {
        request.uri = agent_descriptor.sourceUrl
      }
      Object plugin_content = plugin_fetch_agent.get()

      boolean is_valid = false;

     
      // Step 1 - calculate a checksum for plugin_content and 
      // if the installation is set to secure verify the signature
      MessageDigest md5_digest = MessageDigest.getInstance("MD5");
      md5_digest.update(plugin_content.toString().getBytes())
      byte[] md5sum = md5_digest.digest();
      String hash = new BigInteger(1, md5sum).toString(16);

      // Step 2 - Validate the script
      switch ( agent_descriptor.language ) {
        case 'groovy':
          is_valid = validateGroovyScript(agent_descriptor, plugin_content)
          break;
        default:
          log.warn("unhandled language: ${agent_descriptor.language}");
          break;
      }

      // Step 3 - Script is valid, and signature checks out, create (Or update) record
      if ( is_valid ) {
        // create record
        BespokeSource bs = BespokeSource.findByName(agent_descriptor.sourceName) ?: new BespokeSource()
        bs.name = agent_descriptor.sourceName
        bs.auth = Authority.findByName(agent_descriptor.authority) ?: new Authority(name: agent_descriptor.authority).save(flush:true, failOnError:true);
        bs.script = plugin_content;
        bs.sourceLocation = agent_descriptor.sourceUrl;
        bs.checksum = hash;
        bs.lastPull = new Date()
        bs.language = RefdataValue.lookupOrCreate('BespokeSource.Language',agent_descriptor.language);
        bs.packaging = RefdataValue.lookupOrCreate('BespokeSource.Packaging',agent_descriptor.packaging);
        bs.save(flush:true, failOnError:true);
        log.debug("Saved new bespoke source ${bs}");
      }
    }
    else {
      log.error("malformed agent_descriptor (${agent_descriptor.sourceUrl}/${agent_descriptor.authority}/${agent_descriptor.sourceName})");
    }
  }

  private boolean validateGroovyScript(Map agent_descriptor, String code) {

    boolean result = false;

    try {
      // Parse the class
      Class clazz = new DynamicClassLoader().parseClass(code)
      println("Got class ${clazz}");
      result = true;
      clazz
    }
    catch ( Exception e ) {
      log.error("Error",e);
    }

    return result;
  }
}
