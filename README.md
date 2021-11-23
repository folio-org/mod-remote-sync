# mod-remote-sync

Mod-remote-sync is a generic FOLIO module for system librarians which helps reconcile and keep in sync a remote set of resources with a local FOLIO system. 

The module is generic in the sense that without configuration it has no utility. System librarians must develop and upload "Agent" modules for fetching data from remote systems and processing that data. mod-remote-sync provides services to these agents such as scheduling, error reporting, and feedback case management which makes the reconcilliation process interactive.

For example, a systems librarian wishes to keep a copy of their LASER licenses in FOLIO and to keep that subset of licenses up to date. For historical reasons, licenses may already exist in FOLIO and whenever FOLIO encounters a new license, the systems librarian wishes to have a choice to create a new FOLIO license to track the LASER license, or to map to an existing FOLIO license. mod-remote-sync provides a generic "Resource-Mapping-Feedback" process whereby an agent can request that the user provide such mappings and halts processing until the condition is satisfied.

THis is an EXPLORATORY MODULE : 

Backend Module to provide data synchronisation with arbitrary sources - initially focussed on LASER resources into Subscriptions and Agreements for Leipzig but with wider scope if we get it right.


How does mod-remote-sync work?

The <<controller>> class mod_remote_sync.SettingController exposes a "worker" endpoint that is called either
by a timer or manually invoked to trigger an invocation of the mod-remote-sync main control loop.

The main control loop triggers the <<service>> ExtractService via it's start() method using a <<promise>>

The main control loop started by mod_remote_sync.ExtractService performs the following 3 tasks

  1. runSourceTasks - Fetch records from remote sources using groovy scripts which implement the mod_remote_sync.source.RemoteSyncActivity interface
  2. runExtractTasks - Each set of source records is examined for new records and new transformation tasks created for those records
  3. runTransformationTasks - Try to complete each of the transformation tasks

Within step 3 above we may discover that we need to ask questions of the human operator. Transformation tasks in step three can register feedbackItems which
generate UI tasks for an operator to complete. Having registered their decision, subsequent loops should be able to complete record import.

## DEVOPS

### Container Params

This is a spring boot (grails 4) application which uses springs default environment overide conventions. As such, a line in the
service/grails-app/conf/application.yaml file like

     url: "jdbc:postgresql://${db.host:localhost}:${db.port:5432}/${db.database:olfdev}"

Can access environment variables provided through (for example) a conainer ENV setting. In the example above, the env DB_PORT would be mapped to db.port.
Other conventions apply - see here: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config

The following container params can be set

* REQUIRE_SIGNED_CODE - This is a special remote-sync setting that requires uploaded modules to be digitally signed "ON" | "OFF" (Default is OFF)

Standard env overrides:

* DB_DATABASE
* DB_HOST
* DB_USERNAME
* DB_PASWORD
* DB_MAXPOOLSIZE
* DB_PORT
* EVENTS_PUBLISHER_BOOTSTRAP_SERVERS
* EVENTS_CONSUMER_BOOTSTRAP_SERVERS
* EVENTS_PUBLISHER_ZK_CONNECT
* EVENTS_CONSUMER_ZK_CONNECT

