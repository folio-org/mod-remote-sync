package mod_remote_sync

import grails.gorm.MultiTenant;

/**
 */
public class CodeSigningAuthority  implements MultiTenant<CodeSigningAuthority> {

  String id
  String name
  String publicKey

  static constraints = {
         name (nullable : false)
    publicKey (nullable : true)
  }

  static mapping = {
    table 'mrs_code_signing_authority'
    id                     column : 'csa_id', generator: 'uuid2', length:36
    version                column : 'csa_version'
    name                   column : 'csa_name'
    publicKey              column : 'csa_public_key'
  }

  public String toString() {
    return "CodeSigningAuthority::id:${id}/name:${name}".toString()
  }
}
