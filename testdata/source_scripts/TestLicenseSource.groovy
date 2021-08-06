package folio.modrs.scripts

import mod_remote_sync.source.RemoteSyncActivity;
import mod_remote_sync.source.RecordSourceController;
import groovy.json.JsonOutput
import java.security.MessageDigest

public class TestLicenseSource implements RemoteSyncActivity {

  public void getNextBatch(String source_id,
                           Map state,
                           RecordSourceController rsc) {

    def test_records = [
      [ 
        id:'test-record-0001',
        licenseName:'Test Licenses 001',
        status:'UnMappedCurrent',
        type:'UnMappedConsortial',
        testRefdata:'One',
      ],
      [ 
        id:'test-record-0002',
        licenseName:'Test Licenses 002',
        status:'UnMappedCurrent',
        type:'UnMappedConsortial',
        testRefdata:'One',
      ],
      [
        id:'test-record-0003',
        licenseName:'Test Licenses 003',
        status:'Active',
        type:'Consortial',
        testRefdata:'Two',
        dynamicField: new Date()
      ],
    ]

    test_records.each { Map testrec ->

       def license_json = JsonOutput.toJson(testrec);
       MessageDigest md5_digest = MessageDigest.getInstance("MD5");
       byte[] license_json_bytes = license_json.toString().getBytes()
       md5_digest.update(license_json_bytes);
       byte[] md5sum = md5_digest.digest();
       String license_hash = new BigInteger(1, md5sum).toString(16);

       rsc.upsertSourceRecord(source_id,
                              'TEST',
                              'TEST:LICENSE:'+testrec.id,
                              'TEST:LICENSE',
                              testrec.licenseName,
                              license_hash,
                              license_json_bytes);
    }

  }
}
