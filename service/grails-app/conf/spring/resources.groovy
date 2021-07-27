// Place your Spring DSL code here
import grails.util.Environment
import mod_remote_sync.folio.*

beans = {

  switch(Environment.current) {
    case Environment.TEST:
      // In the test environment src/test/groovy/... will be included in classpath and the import mod_remote_sync.folio.*
      // above will provide us with a mock implementaiton.
      folioHelperService(MockFolioHelperService)
      break
    default:
      folioHelperService(FolioHelperServiceImpl)
      break
  }
}

