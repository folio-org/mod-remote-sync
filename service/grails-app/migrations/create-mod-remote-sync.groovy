databaseChangeLog = {
  changeSet(author: "ianibbo (manual)", id: "i202105051310-001") {
    createSequence(sequenceName: "hibernate_sequence")
  }

  changeSet(author: "ianibbo (generated)", id: "i202105051311-001") {

    createTable(tableName: "mrs_authority") {
      column(name: "aut_id", type: "VARCHAR(36)") {
        constraints(nullable: "false")
      }

      column(name: "aut_version", type: "BIGINT") {
        constraints(nullable: "false")
      }

      column(name: "aut_date_created", type: "TIMESTAMP") {
        constraints(nullable: "true")
      }

      column(name: "aut_date_updated", type: "TIMESTAMP") {
        constraints(nullable: "true")
      }

      column(name: "aut_name", type: "VARCHAR(128)") {
        constraints(nullable: "false")
      }
    }

    createTable(tableName: "mrs_source") {
      column(name: "src_id", type: "VARCHAR(36)") {
        constraints(nullable: "false")
      }

      column(name: "src_version", type: "BIGINT") {
        constraints(nullable: "false")
      }

      column(name: "src_date_created", type: "TIMESTAMP") {
        constraints(nullable: "true")
      }

      column(name: "src_date_updated", type: "TIMESTAMP") {
        constraints(nullable: "true")
      }

      column(name: "src_authority_fk", type: "VARCHAR(36)") {
        constraints(nullable: "false")
      }
    }

    createTable(tableName: "mrs_oai_source") {

      column(name: "mos_id", type: "VARCHAR(36)") {
        constraints(nullable: "false")
      }

      column(name: "mos_base_url", type: "VARCHAR(256)") {
        constraints(nullable: "false")
      }

  }

}
