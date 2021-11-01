package mod_remote_sync

class UrlMappings {

  static mappings = {
    "/"(controller: 'application', action:'index');
    "/remote-sync/statusReport" (controller: 'application', action:'statusReport');
    "/remote-sync/crosswalks" (controller: 'application', action:'crosswalks');

    '/remote-sync/refdata'(resources: 'refdata') {
      collection {
        "/$domain/$property" (controller: 'refdata', action: 'lookup', method: 'GET')
      }
    }

    "/remote-sync/settings/worker" (controller: 'Setting', action: 'worker');
    "/remote-sync/settings/configureFromRegister" (controller: 'Setting', action: 'configureFromRegister');
    "/remote-sync/settings/currentDefinitions" (controller: 'Setting', action: 'currentDefinitions');
    "/remote-sync/settings/appSettings" (resources: 'setting');
    "/remote-sync/sources" (resources: 'sources') {
      collection {
        "/bespoke" (resources: 'bespokeSources')
      }
    }
    "/remote-sync/authorities" (resources: 'authorities');
    "/remote-sync/records" (resources: 'transformationRecord');
    "/remote-sync/feedback" (resources: 'feedbackItem') {
      collection {
        "/todo" ( action:'todo', method: 'GET')
      }
      collection {
        "/done" ( action:'done', method: 'GET')
      }

    }
  }
}
