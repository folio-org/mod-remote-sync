package mod_remote_sync.source

public interface RemoteSyncActivity {

  /**
   * get the next batch of records given the map of state information.
   * on each new record you must call RecordSourceController.update(RECORD, STATE)
   */
  public void getNextBatch(String source_id,
                           Map state, 
                           RecordSourceController rsc);
}
