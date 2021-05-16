package mod_remote_sync.source

import java.security.CodeSource
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.Phases
import grails.util.Holders 
import org.grails.compiler.injection.GrailsAwareClassLoader
 
class DynamicClassLoader extends GrailsAwareClassLoader {
 
   // private final ClassInjector[] _classInjectors = [new DynamicDomainClassInjector()]
 
   DynamicClassLoader() {
      super(Holders.grailsApplication.classLoader, CompilerConfiguration.DEFAULT)
      // classInjectors = _classInjectors
   }
 
   @Override
   protected CompilationUnit createCompilationUnit( CompilerConfiguration config, CodeSource source) {
     CompilationUnit cu = super.createCompilationUnit(config, source)
     // cu.addPhaseOperation(new GrailsAwareInjectionOperation( getResourceLoader(), _classInjectors), Phases.CANONICALIZATION)
     cu
   }
}
