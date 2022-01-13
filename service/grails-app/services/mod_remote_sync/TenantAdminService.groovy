package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver
import mod_remote_sync.CodeSigningAuthority

@Transactional
class TenantAdminService {

  private static final String DEFAULT_PUBLIC_KEY='''-----BEGIN PUBLIC KEY-----
MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAPHjPUURsO2YAQN5tsGKVAMe9qWMqwJ/
BILvnE5yNfQun1uI8UgsAiCzwH72jItjqSXyQLRVXN3vuW1LCz5eDR8CAwEAAQ==
-----END PUBLIC KEY-----
''';

  @Subscriber('okapi:tenant_load_reference')
  public void onTenantLoadReference(final String tenantId, 
                                    final String value, 
                                    final boolean existing_tenant, 
                                    final boolean upgrading, 
                                    final String toVersion, 
                                    final String fromVersion) {
    log.debug("TenantAdminService::onTenantLoadReference");
      final String schemaName = OkapiTenantResolver.getTenantSchemaName(tenantId)
      Tenants.withId(schemaName) {
        // A category for Yes/No answers
        RefdataValue.lookupOrCreate('YN', 'Yes');
        RefdataValue.lookupOrCreate('YN', 'No');

        RefdataValue.lookupOrCreate('YNO', 'Yes')
        RefdataValue.lookupOrCreate('YNO', 'No')
        RefdataValue.lookupOrCreate('YNO', 'Other')

        AppSetting cert_st = AppSetting.findByKey('PublicKey') ?: new AppSetting(
                                       section: 'Secure Mode',
                                       key: 'PublicKey',
                                       settingType: 'String').save(flush:true, failOnError:true);

        AppSetting mode_st = AppSetting.findByKey('Enabled') ?: new AppSetting(
                                       section: 'Secure Mode',
                                       key: 'Enabled',
                                       vocab:'YN',
                                       settingType: 'Refdata').save(flush:true, failOnError:true);


        CodeSigningAuthority csa = CodeSigningAuthority.findByName('k-int') ?: new CodeSigningAuthority(
                                       name:'k-int',
                                       publicKey:DEFAULT_PUBLIC_KEY).save(flush:true, failOnError:true);
      }
  }

}
