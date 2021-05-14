package mod_remote_sync

class UrlMappings {

  static mappings = {
    "/"(controller: 'application', action:'index');

    '/remote-sync/refdata'(resources: 'refdata') {
      collection {
        "/$domain/$property" (controller: 'refdata', action: 'lookup', method: 'GET')
      }
    }

    "/remote-sync/settings/worker" (controller: 'Setting', action: 'worker');
    "/remote-sync/settings/appSettings" (resources: 'setting');
    "/remote-sync/sources" (resources: 'sources') {
      collection {
        "/bespoke" (resources: 'bespokeSources')
      }
    }
    "/remote-sync/authorities" (resources: 'authorities');

  }
}
