package mod_remote_sync

import grails.gorm.MultiTenant;

public class Authority implements MultiTenant<Authority> {

  String id
  String name
  Date dateCreated
  Date lastUpdated

  static constraints = {
    name  (nullable : false, blank: false, unique: true)
  }

  static mapping = {
    table 'mrs_authority'
    id                     column : 'aut_id', generator: 'uuid2', length:36
    version                column : 'aut_version'
    dateCreated            column : 'aut_date_created'
    lastUpdated            column : 'aut_date_updated'
    name                   column : 'aut_name'
  }

}
