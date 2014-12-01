/*
 * Copyright (C) 2014 Redpill Linpro AB
 *
 * This file is part of Findwise Integration module for Alfresco
 *
 * Findwise Integration module for Alfresco is free software: 
 * you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Findwise Integration module for Alfresco is distributed in the 
 * hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Findwise Integration module for Alfresco. 
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.redpill.alfresco.repo.findwise;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdateNodePolicy;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.repo.findwise.model.FindwiseIntegrationModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Binds policies for content creation, update and delete
 * 
 * @author Marcus Svensson
 *
 */
public class QueueForIndexingPolicy implements InitializingBean, OnUpdateNodePolicy, OnAddAspectPolicy, OnDeleteNodePolicy {

  private static final Logger LOG = Logger.getLogger(QueueForIndexingPolicy.class);
  private static boolean isInitialized = false;

  protected NodeService nodeService;

  protected PolicyComponent policyComponent;

  protected SearchIntegrationService searchIntegrationService;

  private ThreadPoolExecutor threadPoolExecutor;
  private TransactionListener transactionListener;
  private TransactionService transactionService;
  private static final String KEY_CREATE = QueueForIndexingPolicy.class.getName() + ".create";
  private static final String KEY_DELETE = QueueForIndexingPolicy.class.getName() + ".delete";

  @Override
  public void afterPropertiesSet() throws Exception {
    if (!isInitialized()) {
      policyComponent.bindClassBehaviour(OnUpdateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onUpdateNode", NotificationFrequency.TRANSACTION_COMMIT));
      policyComponent.bindClassBehaviour(OnDeleteNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onDeleteNode", NotificationFrequency.TRANSACTION_COMMIT));

      policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, FindwiseIntegrationModel.ASPECT_FINDWISE_INDEXABLE, new JavaBehaviour(this, "onAddAspect",
          Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
    }
    Assert.notNull(nodeService);
    Assert.notNull(policyComponent);
    Assert.notNull(searchIntegrationService);

    Assert.notNull(threadPoolExecutor);
    Assert.notNull(transactionService);

    transactionListener = new QueueForIndexingTransactionListener();
    if (LOG.isInfoEnabled()) {
      LOG.info("Initalized QueueForIndexingPolicy");
    }
  }

  private Boolean isInitialized() {
    if (isInitialized == false) {
      isInitialized = true;
    } else {
      return true;
    }
    return false;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setPolicyComponent(PolicyComponent policyComponent) {
    this.policyComponent = policyComponent;
  }

  public void setSearchIntegrationService(SearchIntegrationService searchIntegrationService) {
    this.searchIntegrationService = searchIntegrationService;
  }

  public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
    this.threadPoolExecutor = threadPoolExecutor;
  }

  public void setTransactionService(TransactionService transactionService) {
    this.transactionService = transactionService;
  }

  protected boolean isValidDocument(NodeRef nodeRef) {

    if (nodeRef != null && nodeService.exists(nodeRef)) {
      if (!nodeService.hasAspect(nodeRef, FindwiseIntegrationModel.ASPECT_FINDWISE_INDEXABLE)) {
        return false;
      }
      if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY)) {
        return false;
      }
      if (nodeService.hasAspect(nodeRef, ContentModel.ASPECT_CHECKED_OUT)) {
        return false;
      }
    }
    return true;
  }

  protected void addToQueue(final NodeRef nodeRef, final String key) {
    AlfrescoTransactionSupport.bindListener(transactionListener);
    Set<NodeRef> nodeRefs = (Set<NodeRef>) AlfrescoTransactionSupport.getResource(key);
    if (nodeRefs == null) {
      nodeRefs = new HashSet<NodeRef>(5);
      AlfrescoTransactionSupport.bindResource(key, nodeRefs);
    }
    nodeRefs.add(nodeRef);
  }

  protected void addToCreateQueue(final NodeRef nodeRef) {
    addToQueue(nodeRef, KEY_CREATE);
  }

  protected void addToDeleteQueue(final NodeRef nodeRef) {
    addToQueue(nodeRef, KEY_DELETE);
  }

  @Override
  public void onUpdateNode(NodeRef nodeRef) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("onUpdateNode begin");
    }

    if (isValidDocument(nodeRef)) {
      LOG.debug(nodeRef + " is a valid document which will be scheduled for indexing");
      addToCreateQueue(nodeRef);
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("onUpdateNode end");
    }
  }

  @Override
  public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("onAddAspect begin");
    }

    if (isValidDocument(nodeRef)) {
      LOG.debug(nodeRef + " is a valid document which will be scheduled for indexing");
      addToCreateQueue(nodeRef);
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("onAddAspect end");
    }

  }

  @Override
  public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("onDeleteNode begin");
    }
    NodeRef nodeRef = childAssocRef.getChildRef();
    if (isValidDocument(nodeRef)) {
      LOG.debug(nodeRef + " is a valid document which will be removed from index");
      addToDeleteQueue(nodeRef);
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("onDeleteNode end");
    }

  }

  /**
   * Transaction listener, fires off the new thread after transaction commit.
   */
  private class QueueForIndexingTransactionListener extends TransactionListenerAdapter {

    @Override
    public void afterCommit() {

      Set<NodeRef> createdNodeRefs = (Set<NodeRef>) AlfrescoTransactionSupport.getResource(KEY_CREATE);
      Set<NodeRef> deletedNodeRefs = (Set<NodeRef>) AlfrescoTransactionSupport.getResource(KEY_DELETE);

      if (createdNodeRefs != null && createdNodeRefs.size() > 0) {
        Runnable runnable = new QueueForIndexingWorker(createdNodeRefs, SearchIntegrationService.ACTION_CREATE);
        try {
          threadPoolExecutor.execute(runnable);
        } catch (RejectedExecutionException e) {
          LOG.error("Could not spawn thread to handle indexing", e);
        }
      }

      if (deletedNodeRefs != null && deletedNodeRefs.size() > 0) {
        Runnable runnable = new QueueForIndexingWorker(deletedNodeRefs, SearchIntegrationService.ACTION_DELETE);
        try {
          threadPoolExecutor.execute(runnable);
        } catch (RejectedExecutionException e) {
          LOG.error("Could not spawn thread to handle removal of indexing", e);
        }
      }
    }

    /**
     * Worker which handles indexing of nodes
     */
    public class QueueForIndexingWorker implements Runnable {
      private Set<NodeRef> documentNodeRefs;
      private String action;

      public QueueForIndexingWorker(Set<NodeRef> documentNodeRefs, String action) {
        this.documentNodeRefs = documentNodeRefs;
        this.action = action;
      }

      /**
       * Runner
       */
      public void run() {
        AuthenticationUtil.runAs(new RunAsWork<Void>() {
          public Void doWork() throws Exception {
            return transactionService.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {

              @Override
              public Void execute() throws Throwable {
                try {
                  searchIntegrationService.pushUpdateToIndexService(documentNodeRefs, action);
                } catch (Exception e) {
                  LOG.error("Exception when handling update to index service", e);
                  throw e;
                }
                return null;
              }

            }, false, false);

          }
        }, AuthenticationUtil.getSystemUserName());
      }
    }
  }
}
