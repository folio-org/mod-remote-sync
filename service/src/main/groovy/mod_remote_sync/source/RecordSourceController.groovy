package mod_remote_sync.source

public interface RecordSourceController {

  /**
   * a record source is telling us about a new update record and it's associated
   * update state
   */
   
  public void update(String source, byte[] record, Map state)
}
