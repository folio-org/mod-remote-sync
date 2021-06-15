databaseChangeLog = {
  changeSet(author: "ianibbo (manual)", id: "i202105051310-001") {
    createSequence(sequenceName: "hibernate_sequence")
  }

  changeSet(author: "ianibbo (generated)", id: "i202105051311-001") {

    createTable(tableName: "mrs_authority") {
      column(name: "aut_id", type: "VARCHAR(36)") { constraints(nullable: "false") }
      column(name: "aut_version", type: "BIGINT") { constraints(nullable: "false") }
      column(name: "aut_date_created", type: "TIMESTAMP") { constraints(nullable: "true") }
      column(name: "aut_date_updated", type: "TIMESTAMP") { constraints(nullable: "true") }
      column(name: "aut_name", type: "VARCHAR(128)") { constraints(nullable: "false") }
    }

    addPrimaryKey(columnNames: "aut_id", constraintName: "mrs_authorityPK", tableName: "mrs_authority")

    createTable(tableName: "mrs_source") {
      column(name: "src_id", type: "VARCHAR(36)") { constraints(nullable: "false") }
      column(name: "src_version", type: "BIGINT") { constraints(nullable: "false") }
      column(name: "src_date_created", type: "TIMESTAMP") { constraints(nullable: "true") }
      column(name: "src_date_updated", type: "TIMESTAMP") { constraints(nullable: "true") }
      column(name: "src_authority_fk", type: "VARCHAR(36)") { constraints(nullable: "false") }
      column(name: "src_name", type: "VARCHAR(128)") { constraints(nullable: "false") }
      column(name: 'src_enabled', type: "BOOLEAN");
      column(name: 'src_status',  type: "VARCHAR(32)");
      column(name: 'src_state_info', type: "TEXT");
      column(name: "src_emits", type: "VARCHAR(128)")
      column(name: 'src_next_due', type: "BIGINT");
      column(name: 'src_interval', type: "BIGINT");
    }

    addPrimaryKey(columnNames: "src_id", constraintName: "mrs_sourcePK", tableName: "mrs_source")

    createTable(tableName: "mrs_oai_source") {
      column(name: "mos_id", type: "VARCHAR(36)") { constraints(nullable: "false") }
      column(name: "mos_base_url", type: "VARCHAR(256)") { constraints(nullable: "false") }
    }

    addPrimaryKey(columnNames: "mos_id", constraintName: "mrs_oai_sourcePK", tableName: "mrs_oai_source")

    createTable(tableName: "mrs_bespoke_src") {
      column(name: "mbs_id",              type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mbs_script",          type: "TEXT")          { constraints(nullable: "true") }
      column(name: "mbs_lang",            type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mbs_packaging",       type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mbs_source_location", type: "VARCHAR(128)")  { constraints(nullable: "false") }
      column(name: "mbs_checksum",        type: "VARCHAR(32)")   { constraints(nullable: "true") }
      column(name: "mbs_last_pull",       type: "TIMESTAMP")     { constraints(nullable: "false") }
      column(name: "mbs_signed_by",       type: "VARCHAR(128)")  { constraints(nullable: "true") }
      column(name: "mbs_signature",       type: "VARCHAR(256)")  { constraints(nullable: "true") }
    }

    addPrimaryKey(columnNames: "mbs_id", constraintName: "mrs_bespoke_srcPK", tableName: "mrs_bespoke_src")

    createTable(tableName: "refdata_category") {
      column(name: "rdc_id", type: "VARCHAR(36)") { constraints(nullable: "false") }
      column(name: "rdc_version", type: "BIGINT") { constraints(nullable: "false") }
      column(name: "rdc_description", type: "VARCHAR(255)") { constraints(nullable: "false") }
      column(name: "internal", type: "boolean")
    }

    addPrimaryKey(columnNames: "rdc_id", constraintName: "refdata_categoryPK", tableName: "refdata_category")

    addNotNullConstraint (tableName: "refdata_category", columnName: "internal", defaultNullValue: false)


    createTable(tableName: "refdata_value") {
      column(name: "rdv_id", type: "VARCHAR(36)") { constraints(nullable: "false") }
      column(name: "rdv_version", type: "BIGINT") { constraints(nullable: "false") }
      column(name: "rdv_value", type: "VARCHAR(255)") { constraints(nullable: "false") }
      column(name: "rdv_owner", type: "VARCHAR(36)") { constraints(nullable: "false") }
      column(name: "rdv_label", type: "VARCHAR(255)") { constraints(nullable: "false") }
    }

    addPrimaryKey(columnNames: "rdv_id", constraintName: "refdata_valuePK", tableName: "refdata_value")

    createTable(tableName: "mrs_resource_stream") {
      column(name: "rs_id",             type: "VARCHAR(36)")       { constraints(nullable: "false") }
      column(name: "rs_version",        type: "BIGINT")            { constraints(nullable: "false") }
      column(name: "rs_date_created",   type: "TIMESTAMP")         { constraints(nullable: "true") }
      column(name: "rs_date_updated",   type: "TIMESTAMP")         { constraints(nullable: "true") }
      column(name: 'rs_name',           type: 'VARCHAR(128)')      { constraints(nullable: "true") }
      column(name: 'rs_source_fk',      type: 'VARCHAR(128)')      { constraints(nullable: "true") }
      column(name: 'rs_cusrsor',        type: 'TEXT')              { constraints(nullable: "true") }
      column(name: 'rs_stream_id',      type: 'VARCHAR(128)')      { constraints(nullable: "true") }
      column(name: 'rs_stream_status',  type: 'VARCHAR(128)')      { constraints(nullable: "true") }
      column(name: 'rs_next_due',       type: "BIGINT");
      column(name: 'rs_interval',       type: "BIGINT");
    }

    addPrimaryKey(columnNames: "rs_id", constraintName: "mrs_resource_streamPK", tableName: "mrs_resource_stream")

    createTable(tableName: "mrs_trans_process") {
      column(name: "mtp_id",              type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mtp_version",         type: "BIGINT")        { constraints(nullable: "false") }
      column(name: "mtp_name",            type: "VARCHAR(128)")  { constraints(nullable: "false") }
      column(name: "mtp_script",          type: "TEXT")          { constraints(nullable: "true") }
      column(name: "mtp_lang",            type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mtp_packaging",       type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mtp_source_location", type: "VARCHAR(128)")  { constraints(nullable: "false") }
      column(name: "mtp_checksum",        type: "VARCHAR(32)")   { constraints(nullable: "true") }
      column(name: "mtp_last_pull",       type: "TIMESTAMP")     { constraints(nullable: "false") }
      column(name: "mtp_signed_by",       type: "VARCHAR(128)")  { constraints(nullable: "true") }
      column(name: "mtp_signature",       type: "VARCHAR(256)")  { constraints(nullable: "true") }
      column(name: "mtp_accepts",         type: "VARCHAR(256)")  { constraints(nullable: "true") }
    }

    addPrimaryKey(columnNames: "mtp_id", constraintName: "mrs_trans_process_PK", tableName: "mrs_trans_process")

    createTable(tableName: "mrs_tp_record") {
      column(name: "mtr_id",                      type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mtr_version",                 type: "BIGINT")        { constraints(nullable: "false") }
      column(name: "mtr_transform_status",        type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mtr_owner_fk",                type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mtr_process_control_status",  type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "mtr_source_record_id",        type: "VARCHAR(255)")  { constraints(nullable: "false") }
      column(name: "mtr_input_data",              type: "BYTEA")         
      column(name: "mtr_status_report",           type: "TEXT")
    }

    addPrimaryKey(columnNames: "mtr_id", constraintName: "mrs_tp_record_PK", tableName: "mrs_tp_record")

    createTable(tableName: "mrs_source_resource_2") {
      column(name: "sr_id",              type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "sr_version",         type: "BIGINT")        { constraints(nullable: "false") }
      column(name: "sr_date_created",    type: "TIMESTAMP")     { constraints(nullable: "false") }
      column(name: "sr_date_updated",    type: "TIMESTAMP")     { constraints(nullable: "true") }
      column(name: "sr_auth_fk",         type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "sr_resource_uri",    type: "VARCHAR(128)")  { constraints(nullable: "false") }
      column(name: "sr_checksum",        type: "VARCHAR(32)")   { constraints(nullable: "false") }
      column(name: "sr_record",          type: "BYTEA")         { constraints(nullable: "false") }
      column(name: "sr_rectype",         type: "VARCHAR(128)")  { constraints(nullable: "false") }
      column(name: "sr_owner_source_fk", type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "sr_seqts",           type: "BIGINT")        { constraints(nullable: "false") }
    }

    addPrimaryKey(columnNames: "sr_id", constraintName: "mrs_source_resource_2_PK", tableName: "mrs_source_resource_2")

    createIndex(indexName: "source_resource_owner", tableName: "mrs_source_resource_2") {
      column(name: "sr_owner_source_fk")
      column(name: "sr_rectype")
      column(name: "sr_date_updated")
    }

    createTable(tableName: "app_setting") {
      column(name: "st_id", type: "VARCHAR(36)") {
        constraints(nullable: "false")
      }
      column(name: "st_version", type: "BIGINT") {
        constraints(nullable: "false")
      }
      column(name: 'st_section', type: "VARCHAR(255)")
      column(name: 'st_key', type: "VARCHAR(255)")
      column(name: 'st_setting_type', type: "VARCHAR(255)")
      column(name: 'st_vocab', type: "VARCHAR(255)")
      column(name: 'st_default_value', type: "VARCHAR(255)")
      column(name: 'st_value', type: "VARCHAR(255)")
    }

    addPrimaryKey(columnNames: "st_id", constraintName: "app_setting_PK", tableName: "app_setting")

    createTable(tableName: "mrs_resource_mapping") {
      column(name: "rm_id",              type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "rm_version",         type: "BIGINT")        { constraints(nullable: "false") }
      column(name: "rm_date_created",    type: "TIMESTAMP")     { constraints(nullable: "false") }
      column(name: "rm_date_updated",    type: "TIMESTAMP")     { constraints(nullable: "true") }
      column(name: 'rm_source',          type: "VARCHAR(255)")  
      column(name: 'rm_source_id',       type: "VARCHAR(255)")
      column(name: 'rm_mapping_context', type: "VARCHAR(255)")
      column(name: 'rm_mapping_status',  type: "VARCHAR(255)")
      column(name: 'rm_folio_context',   type: "VARCHAR(255)")
      column(name: 'rm_folio_id',        type: "VARCHAR(255)")
    }

    addPrimaryKey(columnNames: "rm_id", constraintName: "mrs_resource_mapping_PK", tableName: "mrs_resource_mapping")

    createIndex(indexName: "rm_source_id_idx", tableName: "mrs_resource_mapping") {
      column(name: "rm_source")
      column(name: "rm_source_id")
      column(name: "rm_mapping_context")
    }

    createTable(tableName: "feedback_item") {
      column(name: "fb_id",              type: "VARCHAR(36)")   { constraints(nullable: "false") }
      column(name: "fb_version",         type: "BIGINT")
      column(name: "fb_correlation_id",  type: "VARCHAR(255)")  { constraints(nullable: "false") }
      column(name: "fb_question",        type: "TEXT")          { constraints(nullable: "false") }
      column(name: "fb_response",        type: "TEXT")
    }

    addPrimaryKey(columnNames: "fb_id", constraintName: "feedback_item_PK", tableName: "feedback_item")

    createIndex(indexName: "feedback_item_res_idx", tableName: "feedback_item") {
      column(name: "fb_correlation_id")
    }

  }

}
