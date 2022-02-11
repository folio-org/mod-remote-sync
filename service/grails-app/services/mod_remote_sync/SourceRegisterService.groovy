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
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import org.apache.commons.codec.binary.Base64;


@Transactional
class SourceRegisterService {

  def grailsWebDataBinder
  def transformationRunnerService
  def resourceMappingService

  private List<Map> crosswalk_cache = null;

  public Map load(String url) {

    log.debug("Load: ${url}");
    Map result = [status:'OK',messages:[]]

    try {
  
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
            log.debug("Process entry: ${entry.recordType} / pubDate ${entry.pubDate}")
  
            if ( ( entry?.parameters != null ) && 
                 ( entry?.parameters instanceof Map ) ) {
              log.debug("Definition states parameters - checking....");
              ensureSettings(entry.parameters, result)
            }
  
            switch ( entry.recordType ) {
              case 'source':
                processSourceEntry(entry, result)
                break;
              case 'process':
                processProcessEntry(entry, result)
                break;
              case 'extract':
                processExtractEntry(entry, result)
                break;
              case 'mappings':
                processMappings(entry, result)
                break;
              case 'authorityControl':
                processAuthorityControlSources(entry, result)
                break;
              default:
                log.warn("Unhandled record type: ${entry}");
            }
          }
  
          // Clear the compiled process cache to adopt any changed handlers
          transformationRunnerService.clearProcessCache()
  
          // Clear the crosswalk cache to force it to be rebuilt
          this.crosswalk_cache = null;
        }
      }
    }
    catch ( Exception e ) {
      log.error("Unexpected error processing definitions file",e);
    }
    finally {
      log.info("Final result of SourceRegisterService::load(${url}) is ${result}");
    } 

    return result;
  }

  private void ensureSettings(Map definitions, Map state) {
    definitions.each { key, defn ->
      log.debug("Ensure definition: ${defn}");
      def st = AppSetting.findBySectionAndKey(defn.section, defn.key)
      if ( st == null ) {
        switch ( defn?.type ) {
          case 'String':
            st = new AppSetting(section: defn.section,
                                key: defn.key,
                                settingType: 'String',
                                defValue: defn.default).save(flush:true, failOnError:true);
            break;
          case 'Password':
            st = new AppSetting(section: defn.section,
                                key: defn.key,
                                settingType: 'Password',
                                defValue: defn.default).save(flush:true, failOnError:true);
            break;
          case 'Refdata':
            st = new AppSetting(section: defn.section,
                                key: defn.key,
                                settingType: 'Refdata',
                                vocab: defn.vocab,
                                defValue: defn.default).save(flush:true, failOnError:true);
          default:
            log.warn("Unhandled setting type: ${defn.type}");
            break;
        }
      }
    }
  }

  private void processExtractEntry(Map descriptor, Map state) {
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
          rs.interval = descriptor.interval ?: 1000*60*30
          // We're updating the config - so set next due to now
          rs.nextDue = null;
          rs.save(flush:true, failOnError:true);
        }
      }
      else {
        log.warn("Unable to locate source with name ${descriptor.source}");
      }
    }
  }

  private void processSourceEntry(Map agent_descriptor, Map state) {
    println("Got agent descriptor: ${agent_descriptor}");
    switch ( agent_descriptor.packaging ) {
      case 'script':
        processScript(agent_descriptor, state);
        break;
      default:
        log.warn("unhandled packaging: ${agent_descriptor.packaging}");
        break;
    }
  }

  private void processProcessEntry(Map descriptor, Map state) {
    println("Got process descriptor: ${descriptor}");
    switch ( descriptor.packaging ) {
      case 'script':
        ingestProcessDescriptor(descriptor, state);
        break;
      default:
        log.warn("unhandled packaging: ${descriptor.packaging}");
        break;
    }
  }

  private void ingestProcessDescriptor(Map descriptor, Map state) {
    log.debug("ingestProcessDescriptor ${descriptor.processName}");
    if (  ( descriptor.processName ) &&
          ( descriptor.sourceUrl ) ) {
      Map code_info = fetchAndValidateCode(descriptor.sourceUrl, 
                                           descriptor.language, 
                                           TransformProcess.class, 
                                           state,
                                           descriptor.sourceMD5,
                                           descriptor.sourceSignedBy,
                                           descriptor.sourceSignature);
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

  private void processScript(Map agent_descriptor, Map state) {
    log.debug("processScript ${agent_descriptor.sourceName} ${agent_descriptor.sourceUrl} ${agent_descriptor.parameters}");
    if ( ( agent_descriptor.sourceUrl ) &&
         ( agent_descriptor.authority ) && 
         ( agent_descriptor.sourceName ) ) {

      // Fetch the code and validate it
      Map code_info = fetchAndValidateCode(agent_descriptor.sourceUrl, 
                                           agent_descriptor.language, 
                                           RemoteSyncActivity.class, 
                                           state,
                                           agent_descriptor.sourceMD5,
                                           agent_descriptor.sourceSignedBy,
                                           agent_descriptor.sourceSignature);

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
        bs.interval = agent_descriptor.interval ?: 1000*60*60*4
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

  private Map fetchAndValidateCode(String source_url, 
                                   String language, 
                                   Class required_interface, 
                                   Map state,
                                   String md5,
                                   String signedBy,
                                   String signature) {

    log.debug("SourceRegisterService::fetchAndValidateCode(${source_url},${language})")

    boolean secure_mode = true

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

        boolean passed_security = true;

        if ( secure_mode ) {
          log.info("Secure mode - assert that ${result?.hash} == ${md5}");
          if ( ! result?.hash?.equalsIgnoreCase(md5) ) {
            state.messages.add("${source_url} - calculated MD5: ${result?.hash} stated MD5: ${md5} - FAIL");
            passed_security = false;
          }
          else {
            state.messages.add("${source_url} - calculated MD5: ${result?.hash} stated MD5: ${md5} - PASS");
            if ( signedBy != null ) {
              CodeSigningAuthority csa = CodeSigningAuthority.findByName(signedBy)
              if ( csa != null ) {
                RSAPublicKey pk = getPublicKey(csa.publicKey)
                log.info("Decoded public key ${pk}");
                byte[] decoded_sig = Base64.decodeBase64(signature)
                if ( verifySignature(result.plugin_content.toString().getBytes(), decoded_sig, pk) ) {
                  state.messages.add("${source_url} - signature validated");
                }
                else {
                  state.messages.add("${source_url} - signature did not validate");
                  passed_security = false;
                }
              }
              else {
                state.messages.add("${source_url} - unable to lookup signing authority ${signedBy}");
                passed_security = false;
              }
            }
            else {
              state.messages.add("${source_url} - has no signedBy property - unable to validate");
              passed_security = false;
            }
          }

        }

        if ( passed_security ) {
          // Step 2 - Validate the script
          switch ( language ) {
            case 'groovy':
              result.is_valid = validateGroovyScript(result.plugin_content, required_interface, state)
              if ( result.is_valid ) {
                state.messages.add("${source_url} : Validated")
              }
              else {
                log.error("Invalid groovy script");
                state.messages.add("${source_url} : FAIL (Script validation)")
                state.status='ERROR'
              }
              break;
            default:
              log.warn("unhandled language: ${descriptor.language}");
              break;
          }
        }
        else {
          state.messages.add("${source_url} did not pass security constraints. Not processed.");
          state.status='ERROR'
        }
      }
    }

    log.debug("fetchAndValidateCode returns ${result}")
    return result;
  }

  private boolean validateGroovyScript(String code, Class required_interface, Map state) {

    boolean result = false;

    try {
      // Parse the class
      Class clazz = new DynamicClassLoader().parseClass(code)
      log.debug("Got class ${clazz}");

      if ( required_interface.isAssignableFrom(clazz) ) {
        log.debug("${clazz.getName()} implements RemoteSyncActivity interface");
        result = true;
      }
      else {
        log.warn("Acquired class ${clazz} does not implement ${required_interface}.. Skip");
        state.messages.add("Provided script does not implement required interface ${required_interface}. FAIL");
        state.status='ERROR'
      }
      // clazz
    }
    catch ( Exception e ) {
      log.error("Error",e);
      state.messages.add("Exception validating groovy script: ${e.message}");
      state.status='ERROR'
    }

    log.debug("validateGroovyScript returns ${result}");
    return result;
  }

  private void processMappings(Map descriptor, Map state) {
    descriptor.mappings?.each { mapping ->
      // log.debug("Process mapping: ${mapping}");
      if ( mapping ) {
        if ( resourceMappingService.lookupMapping(mapping.srcCtx, mapping.srcValue, mapping.mappingContext) == null ) {
          ResourceMapping rm = resourceMappingService.registerMapping(mapping.srcCtx,
                                                                      mapping.srcValue,
                                                                      mapping.mappingContext,
                                                                      mapping.mappingStatus?:'M',
                                                                      mapping.targetCtx,
                                                                      mapping.targetValue);
          log.debug("Created resource mapping: ${rm}");
        }
      }
    }
  }

  private void processAuthorityControlSources(descriptor, result) {
    descriptor.authorities?.each { k, v ->
      log.debug("Process authority: ${k} => ${v}");
      AuthorityControlSource acs = AuthorityControlSource.createCriteria().get {
        vocabUri: k
      }
      if ( acs == null ) {
        acs = new AuthorityControlSource()
      }
      acs.vocabUri = k
      acs.vocabType = v.type
      acs.serviceUrl = v.service
      acs.label = v.label
      acs.save(flush:true)
    }
  }

  public List<Map> getCrosswalks() {
    if ( this.crosswalk_cache == null ) {
      synchronized(this) {
        ResourceMapping.executeQuery('select distinct rm.srcCtx, rm.targetCtx, rm.mappingContext from ResourceMapping as rm').each {
          this.crosswalk_cache.add( [ srcCtx: it[0], targetCtx:it[1], mappingContext: it[2] ] );
        }
      }
    }

    return this.crosswalk_cache;
  }

  public static RSAPublicKey getPublicKey(String key) throws Exception {

    String publicKeyPEM = key
      .replace("-----BEGIN PUBLIC KEY-----", "")
      .replaceAll(System.lineSeparator(), "")
      .replace("-----END PUBLIC KEY-----", "");

    byte[] encoded = Base64.decodeBase64(publicKeyPEM);

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
    return (RSAPublicKey) keyFactory.generatePublic(keySpec);
  }


  private boolean verifySignature(byte[] bytes, byte[] sig, PublicKey pub_key) {
    Signature sig_inst = Signature.getInstance( "SHA1withRSA" );
    sig_inst.initVerify( pub_key );
    sig_inst.update( bytes );
    return sig_inst.verify( sig );
  }

}
