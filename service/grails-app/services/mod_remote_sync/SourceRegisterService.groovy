package mod_remote_sync

import grails.gorm.transactions.Transactional
import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import groovyx.net.http.ChainedHttpConfig
import groovyx.net.http.HttpBuilder
import grails.converters.JSON

@Transactional
class SourceRegisterService {

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
          process(entry)
        }
      }
    }
  }

  private void process(Map agent_descriptor) {
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
    if ( entry.sourceUrl ) {
      HttpBuilder plugin_fetch_agent = HttpBuilder.configure {
        request.uri = entry.sourceUrl
      }
      Object plugin_content = plugin_fetch_agent.get()

      switch ( agent_descriptor.language ) {
        case 'groovy':
          processGroovyScript(agent_descriptor, plugin_content)
          break;
        default:
          log.warn("unhandled language: ${agent_descriptor.language}");
          break;
      }
    }
  }

  private void processGroovyScript(Map agent_descriptor, String plugin_content) {
    println("process groovy script")
  }
}
