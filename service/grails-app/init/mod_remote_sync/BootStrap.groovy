package mod_remote_sync

import com.k_int.okapi.OkapiTenantAdminService

class BootStrap {

  def grailsApplication
  OkapiTenantAdminService okapiTenantAdminService

  def init = { servletContext ->

    log.info("${grailsApplication.getMetadata().getApplicationName()}  (${grailsApplication.config?.info?.app?.version}) initialising");
    log.info("          build number -> ${grailsApplication.metadata['build.number']}");
    log.info("        build revision -> ${grailsApplication.metadata['build.git.revision']}");
    log.info("          build branch -> ${grailsApplication.metadata['build.git.branch']}");
    log.info("          build commit -> ${grailsApplication.metadata['build.git.commit']}");
    log.info("            build time -> ${grailsApplication.metadata['build.time']}");
    log.info("            build host -> ${grailsApplication.metadata['build.host']}");
    log.info("         Base JDBC URL -> ${grailsApplication.config.dataSource.url}");

    Map<String, String> env = System.getenv();
    env.each { name,value ->
      log.info("    ENV: ${name}=\"${value}\"");
    }

    // Check that the migrations file is present - it's absence indicates a build time failure
    def resFile = this.class.classLoader.getResource('module-tenant-changelog.groovy')
    if ( resFile == null ) {
      log.error("Resource /module-tenant-changelog.groovy not found. Critical error in build - service will not function properly");
    }
  }


  def destroy = {
  }
}
