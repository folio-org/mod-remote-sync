import grails.gorm.multitenancy.Tenants
import grails.events.annotation.Subscriber
import grails.gorm.multitenancy.WithoutTenant
import grails.gorm.transactions.Transactional
import com.k_int.web.toolkit.refdata.RefdataValue
import com.k_int.web.toolkit.refdata.RefdataCategory
import com.k_int.web.toolkit.custprops.types.CustomPropertyRefdataDefinition
import com.k_int.web.toolkit.custprops.types.CustomPropertyText;
import com.k_int.web.toolkit.custprops.CustomPropertyDefinition
import grails.databinding.SimpleMapDataBindingSource
import static grails.async.Promises.*
import com.k_int.web.toolkit.settings.AppSetting
import mod_remote_sync.*

log.info 'Importing sample data'

println("\n\n***Completed tenant setup***");
