package mod_remote_sync.folio

interface FolioClient {

  public Object okapiPost(String path, Object o);
  public Object okapiPut(String path, Object o);
  public Object okapiGet(String path, Map params);
}
