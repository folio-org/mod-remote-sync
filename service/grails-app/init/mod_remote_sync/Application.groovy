package mod_remote_sync

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.DefaultGrailsPluginManager
import groovy.transform.CompileStatic
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.context.annotation.Bean
import io.undertow.Undertow.Builder
import org.springframework.scheduling.annotation.EnableScheduling
import groovy.util.logging.Slf4j;


@CompileStatic
@Slf4j
class Application extends GrailsAutoConfiguration {
  static void main(String[] args) {
    // Ensure we have force UTC to be the local application TZ.
    final TimeZone utcTz = TimeZone.getTimeZone("UTC")
    if (TimeZone.default != utcTz) {
      TimeZone.default = utcTz
    }

    // This should be last...
    GrailsApp.run(Application, args)
  }

  @Bean
  public UndertowServletWebServerFactory embeddedServletContainerFactory() {

    UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory()
    factory.builderCustomizers << new UndertowBuilderCustomizer() {

      @Override
      public void customize(Builder builder) {

        // Min of 2, Max of 4 I/O threads
        final int ioThreadCount = Math.min(Math.max(Runtime.getRuntime().availableProcessors(), 2), 4)
        builder.ioThreads = ioThreadCount


        final int heap_coef = ((Runtime.getRuntime().maxMemory() / 1024 / 1024)/256) as int
        int workers_per_io = 8
        if (heap_coef <= 2) {
          workers_per_io = 6
        } else if (heap_coef <= 1) {
          workers_per_io = 4
        }

        // 8 Workers per I/O thread
        final int workerThreadCount = ioThreadCount * workers_per_io
        builder.workerThreads = workerThreadCount
        // Enable HTTP2
        // builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true)

        println "Runtime memory reported ${Runtime.getRuntime().maxMemory() / 1024 / 1024} mb"
        println "Current free memory: ${Runtime.getRuntime().freeMemory()}"
        println "Runtime CPUs reported ${Runtime.getRuntime().availableProcessors()}"
        println "Allocated ${ioThreadCount} IO Threads"
        println "Allocated ${workerThreadCount} worker threads"

      }
    }

    return factory
  }


}
