package mod_remote_sync.source

public interface RecordSourceController {

  /**
   * a record source is telling us about a new update record and it's associated
   * update state
   */
   
  public void updateState(String source_id, Map state)

  public void upsertSourceRecord(String source_id,
                                 String resource_id, 
                                 String resource_type, 
                                 String hash,
                                 byte[] record)

  public String getAppSetting(String key);
}
