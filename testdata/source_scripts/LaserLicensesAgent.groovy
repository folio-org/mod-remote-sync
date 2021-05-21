import mod_remote_sync.source.RemoteSyncActivity;
import mod_remote_sync.source.RecordSourceController;
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.apache.commons.codec.binary.Hex
import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import static groovyx.net.http.HttpBuilder.configure


public class LaserLicensesAgent implements RemoteSyncActivity {

  private String makeAuth(String path, String timestamp, String nonce, String q, String secret) {
    String string_to_hash = "GET${path}${timestamp}${nonce}${q}".toString()

    Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
    SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
    sha256_HMAC.init(secret_key);
    return Hex.encodeHexString(sha256_HMAC.doFinal(string_to_hash.getBytes("UTF-8")));
  }

  public void getNextBatch(Map state, 
                           RecordSourceController rsc) {

    String identifierType=rsc.getAppSetting('laser.identifierType')
    String identifier=rsc.getAppSetting('laser.identifier')
    String url=rsc.getAppSetting('laser.url')
    String secret=rsc.getAppSetting('laser.secret')
    String token=rsc.getAppSetting('laser.token')

    String auth = makeAuth('/api/v0/licenseList', '', '', 'q='+identifierType+'&v='+identifier,secret);

    println("gotAuth: ${auth} - request licenses of type ${identifierType}/id ${identifier} request URL is ${url}");

    def http = configure {
      request.uri = url
    }

    def result = http.get {
      request.uri.path = '/api/v0/licenseList'
      request.headers['x-authorization'] = "hmac $token:::$auth,hmac-sha256"
      request.headers['accept'] = 'application/json'
      request.uri.query = [
        q:identifierType,
        v:identifier
      ]
      response.when(200) { FromServer fs, Object body ->
        println("OK");
        body.each { license_info ->
          println("License ${JsonOutput.prettyPrint(JsonOutput.toJson(license))}")
        }
      }
      response.when(400) { FromServer fs, Object body ->
        println("Problem processing licenses ${body}");
      }
    }

  }
}
