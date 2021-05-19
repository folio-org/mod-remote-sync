import mod_remote_sync.source.RemoteSyncActivity;
import mod_remote_sync.source.RecordSourceController;
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.apache.commons.codec.binary.Hex

public class RemoteSyncOne implements RemoteSyncActivity {

  private String makeAuth(String path, String timestamp, String nonce, String q) {
    String string_to_hash = "GET${path}${timestamp}${nonce}${q}".toString()

    Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
    SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
    sha256_HMAC.init(secret_key);
    return Hex.encodeHexString(sha256_HMAC.doFinal(string_to_hash.getBytes("UTF-8")));
  }


  public void getLatestUpdates() {
    println("Test::getLatestUpdates");
  }

  public void getNextBatch(Map state, RecordSourceController rsc) {
    println("RemoteSyncOne::getNextBatch");
  }
}
