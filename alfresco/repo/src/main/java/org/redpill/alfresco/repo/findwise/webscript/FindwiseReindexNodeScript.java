package org.redpill.alfresco.repo.findwise.webscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.admin.RepositoryState;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.repo.findwise.ClusteredExecuter;
import org.redpill.alfresco.repo.findwise.SearchIntegrationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.util.Assert;

public class FindwiseReindexNodeScript extends DeclarativeWebScript implements InitializingBean {
  private static final Logger LOG = Logger.getLogger(FindwiseReindexNodeScript.class);
  protected SearchIntegrationService searchIntegrationService;
  protected NodeService nodeService;
  protected SearchService searchService;
  protected TransactionService transactionService;
  protected JobLockService jobLockService;
  protected long lockTTL;
  protected RepositoryState repositoryState;

  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(FindwiseReindexNodeScript.class.getName() + " begin");
    }
    boolean result = false;
    Map<String, Object> tmplMap = new HashMap<String, Object>(1);

    if (req.getPathInfo().equals("/findwise/node/reindex/everything")) {
      // Reindex everything
      LOG.info("Request to reindex everything received by " + AuthenticationUtil.getFullyAuthenticatedUser());
      new Thread(new FullReindexer()).run();
      result = true;
    } else {
      // Reindex one
      Map<String, String> templateVars = req.getServiceMatch().getTemplateVars();
      String nodeRefStr = templateVars.get("protocol") + "://" + templateVars.get("identifier") + "/" + templateVars.get("id");
      if (LOG.isInfoEnabled()) {
        LOG.info("Request to reindex " + nodeRefStr + " received by " + AuthenticationUtil.getFullyAuthenticatedUser());
      }
      if (NodeRef.isNodeRef(nodeRefStr)) {
        NodeRef nodeRef = new NodeRef(nodeRefStr);
        result = searchIntegrationService.pushUpdateToIndexService(nodeRef, SearchIntegrationService.ACTION_CREATE);
      }
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace(FindwiseReindexNodeScript.class.getName() + " end");
    }
    tmplMap.put("success", result);
    return tmplMap;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setSearchIntegrationService(SearchIntegrationService searchIntegrationService) {
    this.searchIntegrationService = searchIntegrationService;
  }

  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  public void setTransactionService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  public void setJobLockService(JobLockService jobLockService) {
    this.jobLockService = jobLockService;
  }

  public void setLockTTL(int lockTTL) {
    this.lockTTL = lockTTL;
  }

  public void setRepositoryState(RepositoryState repositoryState) {
    this.repositoryState = repositoryState;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(searchIntegrationService);
    Assert.notNull(nodeService);
    Assert.notNull(searchService);
    Assert.notNull(jobLockService);
    Assert.notNull(transactionService);
    Assert.notNull(repositoryState);
  }

  protected class FullReindexer extends ClusteredExecuter implements Runnable {
    public static final String NAME = "FullFindwiseReindexer";
    public static final int BATCH_SIZE_QUERY = 500;
    public static final int BATCH_SIZE_INDEX = 500;
    public static final String QUERY = "ASPECT:\"fwi:findwiseIndexable\"";

    @Override
    public void run() {
      try {
        super.setJobLockService(jobLockService);
        super.setTransactionService(transactionService);
        super.setRepositoryState(repositoryState);
        super.setLockTTL(lockTTL);
        super.afterPropertiesSet();
      } catch (Exception e) {
        throw new AlfrescoRuntimeException("Initialization exception", e);
      }
      super.execute();
    }

    @Override
    protected String getJobName() {
      return NAME;
    }

    @Override
    protected void executeInternal() {
      // Find all nodes that should be indexed
      int skip = 0;
      SearchParameters searchParameters = new SearchParameters();
      searchParameters.setQuery(QUERY);
      searchParameters.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
      searchParameters.setMaxItems(BATCH_SIZE_QUERY);
      searchParameters.setSkipCount(skip);
      searchParameters.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

      ResultSet result = searchService.query(searchParameters);
      List<NodeRef> resultNodeRefs = result.getNodeRefs();
      List<NodeRef> allNodeRefs = new ArrayList<NodeRef>();
      allNodeRefs.addAll(resultNodeRefs);
      LOG.debug("Adding first " + resultNodeRefs.size() + " objects to list (Limit is " + BATCH_SIZE_QUERY + ")");
      while (resultNodeRefs.size() == BATCH_SIZE_QUERY) {
        skip = skip + BATCH_SIZE_QUERY;
        searchParameters.setSkipCount(skip);
        result = searchService.query(searchParameters);
        resultNodeRefs = result.getNodeRefs();
        LOG.debug("Adding another " + resultNodeRefs.size() + " objects to list");
        allNodeRefs.addAll(resultNodeRefs);
        refreshLock();

        if ((skip + BATCH_SIZE_QUERY * 2) >= Integer.MAX_VALUE) {
          throw new AlfrescoRuntimeException("Something is wrong. We run into integer max value");
        }
      }

      // Start pushing in batches
      LOG.info("Pushing batches of size " + BATCH_SIZE_INDEX + " to index. " + allNodeRefs.size() + " nodes are scheducled for index.");
      final Set<NodeRef> nodeSet = new HashSet<NodeRef>();
      while (allNodeRefs.size() > 0) {
        nodeSet.add(allNodeRefs.remove(0));
        if (nodeSet.size() >= BATCH_SIZE_INDEX || allNodeRefs.size() == 0) {
          LOG.info("Pushing batch of " + nodeSet.size() + " to index. " + allNodeRefs.size() + " nodes left to index");
          final RetryingTransactionHelper retryingTransactionHelper = transactionService.getRetryingTransactionHelper();
          Boolean indexResult = AuthenticationUtil.runAsSystem(new RunAsWork<Boolean>() {
            @Override
            public Boolean doWork() throws Exception {
              return retryingTransactionHelper.doInTransaction(new RetryingTransactionCallback<Boolean>() {
                @Override
                public Boolean execute() throws Throwable {
                  return searchIntegrationService.pushUpdateToIndexService(nodeSet, SearchIntegrationService.ACTION_CREATE);
                }
              }, false, true);
            }
          });
          if (indexResult) {
            LOG.info("All nodes in this batch were successfully pushed");
          } else {
            LOG.info("One or more nodes in this batch were not successfully pushed");
          }
          nodeSet.clear();
          refreshLock();
        }
      }
    }
  }
}
