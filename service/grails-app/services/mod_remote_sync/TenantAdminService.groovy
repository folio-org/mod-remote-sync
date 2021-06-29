package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.settings.AppSetting
import com.k_int.web.toolkit.refdata.*
import com.k_int.okapi.OkapiTenantResolver

@Transactional
class TenantAdminService {

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

        AppSetting cert_st = AppSetting.findByKey('PublicKey') ?: new AppSetting(section: 'Secure Mode',
                                       key: 'PublicKey',
                                       settingType: 'String').save(flush:true, failOnError:true);

        AppSetting mode_st = AppSetting.findByKey('Enabled') ?: new AppSetting(section: 'Secure Mode',
                                       key: 'Enabled',
                                       vocab:'YN',
                                       settingType: 'Refdata').save(flush:true, failOnError:true);

      }
  }

}
