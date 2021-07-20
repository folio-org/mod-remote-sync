# mod-remote-sync
EXPLORATORY MODULE : Backend Module to provide data synchronisation with arbitrary sources - initially focussed on LASER resources into Subscriptions and Agreements for Leipzig but with wider scope if we get it right.


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
