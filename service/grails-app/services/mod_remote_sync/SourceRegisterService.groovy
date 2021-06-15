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
import com.k_int.web.toolkit.settings.AppSetting

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
          log.debug("Process entry: ${entry.recordType}")

          if ( ( entry?.parameters != null ) && 
               ( entry?.parameters instanceof Map ) ) {
            log.debug("Definition states parameters - checking....");
            ensureSettings(entry.parameters)
          }

          switch ( entry.recordType ) {
            case 'source':
              processSourceEntry(entry)
              break;
            case 'process':
              processProcessEntry(entry)
              break;
            case 'extract':
              processExtractEntry(entry)
              break;
            default:
              log.warn("Unhandled record type: ${entry}");
          }
        }
      }
    }
  }

  private void ensureSettings(Map definitions) {
    definitions.each { key, defn ->
      log.debug("Ensure definition: ${defn}");
      def st = AppSetting.findBySectionAndKey(defn.section, defn.key)
      if ( st == null ) {
        switch ( defn?.type ) {
          case 'String':
            st = new AppSetting(section: defn.section,
                                key: defn.key,
                                settingType: 'String').save(flush:true, failOnError:true);
            break;
          default:
            log.warn("Unhandled setting type: ${defn.type}");
            break;
        }
      }
    }
  }

  private void processExtractEntry(Map descriptor) {
    log.debug("SourceRegisterService::processExtract(${descriptor})");
    if ( descriptor.extractName &&
         descriptor.source && 
         descriptor.process ) {
      Source s = Source.findByName(descriptor.source)
      if ( s ) {
        ResourceStream rs = ResourceStream.findByName(descriptor.extractName) ?: new ResourceStream(streamStatus:'IDLE', cursor:'{}');
        TransformationProcess tp = TransformationProcess.findByName(descriptor.process)
        if ( ( rs != null ) &&
             ( tp != null ) ) {
          rs.name = descriptor.extractName;
          rs.source = s
          rs.streamId = tp
          rs.save(flush:true, failOnError:true);
        }
      }
      else {
        log.warn("Unable to locate source with name ${descriptor.source}");
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

  private void processProcessEntry(Map descriptor) {
    println("Got process descriptor: ${descriptor}");
    switch ( descriptor.packaging ) {
      case 'script':
        ingestProcessDescriptor(descriptor);
        break;
      default:
        log.warn("unhandled packaging: ${descriptor.packaging}");
        break;
    }
  }

  private void ingestProcessDescriptor(Map descriptor) {
    log.debug("ingestProcessDescriptor ${descriptor.processName}");
    if (  ( descriptor.processName ) &&
          ( descriptor.sourceUrl ) ) {
      Map code_info = fetchAndValidateCode(descriptor.sourceUrl, descriptor.language, TransformProcess.class);
      if ( code_info?.is_valid ) {
        TransformationProcess tp = TransformationProcess.findByName(descriptor.processName) ?: new TransformationProcess()
        tp.name = descriptor.processName
        tp.script = code_info.plugin_content
        tp.sourceLocation = descriptor.sourceUrl;
        tp.checksum = code_info.hash;
        tp.lastPull = new Date()
        tp.language = RefdataValue.lookupOrCreate('BespokeSource.Language',descriptor.language);
        tp.packaging = RefdataValue.lookupOrCreate('BespokeSource.Packaging',descriptor.packaging);
        tp.accepts = descriptor.accepts
        tp.save(flush:true, failOnError:true);
        log.debug("Saved new transformation process ${tp}");
      }
    }
    else {
      log.error("Process descriptor missing required props (processName, sourceUrl) - ${descriptor}")
    }
  }

  private void processScript(Map agent_descriptor) {
    log.debug("processScript ${agent_descriptor.sourceName} ${agent_descriptor.sourceUrl} ${agent_descriptor.parameters}");
    if ( ( agent_descriptor.sourceUrl ) &&
         ( agent_descriptor.authority ) && 
         ( agent_descriptor.sourceName ) ) {

      // Fetch the code and validate it
      Map code_info = fetchAndValidateCode(agent_descriptor.sourceUrl, agent_descriptor.language, RemoteSyncActivity.class);

      // Step 3 - Script is valid, and signature checks out, create (Or update) record
      if ( code_info?.is_valid ) {
        // create record
        BespokeSource bs = BespokeSource.findByName(agent_descriptor.sourceName) ?: new BespokeSource()
        bs.name = agent_descriptor.sourceName
        bs.auth = Authority.findByName(agent_descriptor.authority) ?: new Authority(name: agent_descriptor.authority).save(flush:true, failOnError:true);
        bs.script = code_info.plugin_content;
        bs.sourceLocation = agent_descriptor.sourceUrl;
        bs.checksum = code_info.hash;
        bs.lastPull = new Date()
        bs.interval = 1000*60*60*12
        bs.nextDue = 0;
        bs.language = RefdataValue.lookupOrCreate('BespokeSource.Language',agent_descriptor.language);
        bs.packaging = RefdataValue.lookupOrCreate('BespokeSource.Packaging',agent_descriptor.packaging);
        bs.enabled = true
        bs.status = 'IDLE'
        bs.emits = agent_descriptor.emits;
        bs.save(flush:true, failOnError:true);
        log.debug("Saved bespoke source ${bs}");
      }
    }
    else {
      log.error("malformed agent_descriptor (${agent_descriptor.sourceUrl}/${agent_descriptor.authority}/${agent_descriptor.sourceName})");
    }
  }

  private Map fetchAndValidateCode(String source_url, String language, Class required_interface) {

    log.debug("SourceRegisterService::fetchAndValidateCode(${source_url},${language})")

    Map result = [
      is_valid : false,
      plugin_content: null,
      hash: null
    ]
    if ( source_url ) {

      HttpBuilder plugin_fetch_agent = HttpBuilder.configure {
        request.uri = source_url
      }

      result.plugin_content = plugin_fetch_agent.get()

      if ( result.plugin_content != null ) {
        // Step 1 - calculate a checksum for plugin_content and 
        // if the installation is set to secure verify the signature
        MessageDigest md5_digest = MessageDigest.getInstance("MD5");
        md5_digest.update(result.plugin_content.toString().getBytes())
        byte[] md5sum = md5_digest.digest();
        result.hash = new BigInteger(1, md5sum).toString(16);

        // Step 2 - Validate the script
        switch ( language ) {
          case 'groovy':
            result.is_valid = validateGroovyScript(result.plugin_content, required_interface)
            break;
          default:
            log.warn("unhandled language: ${descriptor.language}");
            break;
        }
      }
    }

    return result;
  }

  private boolean validateGroovyScript(String code, Class required_interface) {

    boolean result = false;

    try {
      // Parse the class
      Class clazz = new DynamicClassLoader().parseClass(code)
      log.debug("Got class ${clazz}");

      // if ( RemoteSyncActivity.class.isAssignableFrom(clazz) ) {
      if ( required_interface.isAssignableFrom(clazz) ) {
        log.debug("${clazz.getName()} implements RemoteSyncActivity interface");
        result = true;
      }
      else {
        log.warn("Acquired class ${clazz} does not implement ${required_interface}.. Skip");
      }
      // clazz
    }
    catch ( Exception e ) {
      log.error("Error",e);
    }

    return result;
  }
}
