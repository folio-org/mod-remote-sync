package mod_remote_sync.source

public interface RemoteSyncActivity {

  /**
   * get the next batch of records given the map of state information.
   * on each new record you must call RecordSourceController.update(RECORD, STATE)
   */
  public void getNextBatch(Map state, RecordSourceController rsc);
}
