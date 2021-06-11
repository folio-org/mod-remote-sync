package mod_remote_sync

import grails.gorm.transactions.Transactional
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.Tenants
import com.k_int.web.toolkit.custprops.types.CustomPropertyRefdataDefinition
import com.k_int.web.toolkit.custprops.types.CustomPropertyText;
import com.k_int.web.toolkit.custprops.types.CustomPropertyRefdata;
import com.k_int.web.toolkit.custprops.CustomPropertyDefinition

@Transactional
class TenantAdminService {

  private CustomPropertyDefinition ensureRefdataProperty(String name, boolean local, String category, String label = null) {

      CustomPropertyDefinition result = null;
      def rdc = RefdataCategory.findByDesc(category);

      if ( rdc != null ) {
        result = CustomPropertyDefinition.findByName(name)
        if ( result == null ) {
          result = new CustomPropertyRefdataDefinition(
                                            name:name,
                                            defaultInternal: local,
                                            label:label,
                                            category: rdc)
          // Not entirely sure why type can't be set in the above, but other bootstrap scripts do this
          // the same way, so copying. Type doesn't work when set as a part of the definition above
          result.type=CustomPropertyRefdata.class
          result.save(flush:true, failOnError:true);
        }
      }
      else {
        println("Unable to find category ${category}");
      }
      return result;
  }

  CustomPropertyDefinition ensureTextProperty(String name, boolean local = true, String label = null) {
    CustomPropertyDefinition result = CustomPropertyDefinition.findByName(name) ?: new CustomPropertyDefinition(
                                          name:name,
                                          type:CustomPropertyText.class,
                                          defaultInternal: local,
                                          label:label
                                        ).save(flush:true, failOnError:true);
    return result;
  }

  @Subscriber('okapi:tenant_load_reference')
  public void onTenantLoadReference(final String tenantId, final String value, final boolean existing_tenant, final boolean upgrading, final String toVersion, final String fromVersion) {
    log.debug("TenantAdminService::onTenantLoadReference");
      final String schemaName = OkapiTenantResolver.getTenantSchemaName(tenantId)
      Tenants.withId(schemaName) {
          // A category for Yes/No answers
          RefdataValue.lookupOrCreate('YN', 'Yes');
          RefdataValue.lookupOrCreate('YN', 'No');

          // A refdata setting - secureMode - should we only run signed code
          ensureRefdataProperty('secureMode',true,'SecureMode','Secure Mode')

          // a string - the public key to use when validating signed code
          ensureTextProperty('publicKey' )
      }
  }

}
