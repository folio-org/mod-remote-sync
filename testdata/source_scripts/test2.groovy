import mod_remote_sync.source.RemoteSyncActivity;
import mod_remote_sync.source.RecordSourceController;

public class RemoteSyncTwo implements RemoteSyncActivity {

  public void getLatestUpdates() {
    println("Test::getLatestUpdates");
  }

  public void getNextBatch(Map state, RecordSourceController rsc) {
    println("RemoteSyncTwo::getNextBatch");
  }
}
