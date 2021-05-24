import mod_remote_sync.source.RemoteSyncActivity;
import mod_remote_sync.source.RecordSourceController;
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.apache.commons.codec.binary.Hex
import groovyx.net.http.HttpBuilder
import groovyx.net.http.FromServer
import groovy.json.JsonOutput
import static groovyx.net.http.HttpBuilder.configure
import java.security.MessageDigest

public class LaserLicensesAgent implements RemoteSyncActivity {

  private String makeAuth(String path, String timestamp, String nonce, String q, String secret) {
    String string_to_hash = "GET${path}${timestamp}${nonce}${q}".toString()

    Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
    SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
    sha256_HMAC.init(secret_key);
    return Hex.encodeHexString(sha256_HMAC.doFinal(string_to_hash.getBytes("UTF-8")));
  }

  private Map getLicense(String globalUID, String url, String secret, String token) {

    println("getLicense(${globalUID})");

    Map result = null;

    String auth = makeAuth('/api/v0/license', '', '', 'q=globalUID&v='+globalUID, secret);
    println("gotAuth: ${auth}");

    def http = configure {
      request.uri = url
    }

    http.get {
      request.uri.path = '/api/v0/license'
      request.headers['x-authorization'] = "hmac $token:::$auth,hmac-sha256"
      request.headers['accept'] = 'application/json'
      request.uri.query = [
        q:'globalUID',
        v:globalUID
      ]
      response.when(200) { FromServer fs, Object body, Object header ->
        result = body
      }
      response.when(400) { FromServer fs, Object body ->
        println("Problem getting license ${body}");
      }
    }
    return result;
  }


  public void getNextBatch(String source_id,
                           Map state, 
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
          println("Retrieve license ${license_info.globalUID}");
          def license = getLicense(license_info.globalUID, url, secret, token)
          if ( license ) {
            def license_json = JsonOutput.toJson(license);
            MessageDigest md5_digest = MessageDigest.getInstance("MD5");
            byte[] license_json_bytes = license_json.toString().getBytes()
            md5_digest.update(license_json_bytes);
            byte[] md5sum = md5_digest.digest();
            license_hash = new BigInteger(1, md5sum).toString(16);
            rsc.upsertSourceRecord(source_id,
                                   'LASER',
                                   'LASER:LICENSE:'+license_info.globalUID,
                                   'LASER:LICENSE',
                                   license_hash,
                                   license_json_bytes);

          }
        }
      }
      response.when(400) { FromServer fs, Object body ->
        println("Problem processing licenses ${body}");
      }
    }

  }
}
