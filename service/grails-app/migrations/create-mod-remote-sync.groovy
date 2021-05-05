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
  }

}
