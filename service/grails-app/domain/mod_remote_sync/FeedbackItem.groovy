package mod_remote_sync

import grails.gorm.MultiTenant;
import mod_remote_sync.source.RemoteSyncActivity;
import com.k_int.web.toolkit.refdata.Defaults
import com.k_int.web.toolkit.refdata.RefdataValue


/**
 *
 */
public class FeedbackItem implements MultiTenant<FeedbackItem> {

  String id

  String correlationId
  String question
  String response

  static constraints = {
    correlationId (nullable : false)
         question (nullable : false)
         response (nullable : true)
  }

  static mapping = {
    table 'feedback_item'
                           id column:'fb_id', generator: 'uuid2', length:36
                      version column:'fb_version'
                correlationId column:'fb_correlation_id'
                     question column:'fb_question'
                     response column:'fb_response'
  }

}
