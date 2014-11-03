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

  @Override
  public void onUpdateNode(NodeRef nodeRef) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("onUpdateNode begin");
    }

    if (isValidDocument(nodeRef)) {
      LOG.debug(nodeRef + " is a valid document which will be scheduled for indexing");
      AlfrescoTransactionSupport.bindListener(transactionListener);
      AlfrescoTransactionSupport.bindResource(KEY_CREATE, nodeRef);
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
      AlfrescoTransactionSupport.bindListener(transactionListener);
      AlfrescoTransactionSupport.bindResource(KEY_CREATE, nodeRef);
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
      AlfrescoTransactionSupport.bindListener(transactionListener);
      AlfrescoTransactionSupport.bindResource(KEY_DELETE, nodeRef);
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
      NodeRef createdNodeRef = (NodeRef) AlfrescoTransactionSupport.getResource(KEY_CREATE);
      NodeRef deletedNodeRef = (NodeRef) AlfrescoTransactionSupport.getResource(KEY_DELETE);

      NodeRef nodeRef = null;
      String action = null;
      if (createdNodeRef != null) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Add to transaction queue for indexing " + createdNodeRef);
        }
        action = SearchIntegrationService.ACTION_CREATE;
        nodeRef = createdNodeRef;
      } else if (deletedNodeRef != null) {
        if (LOG.isTraceEnabled()) {
          LOG.trace("Add to transaction queue for remove from indexing " + deletedNodeRef);
        }
        action = SearchIntegrationService.ACTION_DELETE;
        nodeRef = deletedNodeRef;
      }

      if (nodeRef != null && action != null) {
        Runnable runnable = new QueueForIndexingWorker(nodeRef, action);
        threadPoolExecutor.execute(runnable);
      }
    }

    /**
     * Updates the person user with additional details from KIV
     */
    public class QueueForIndexingWorker implements Runnable {
      private NodeRef documentNodeRef;
      private String action;

      public QueueForIndexingWorker(NodeRef documentNodeRef, String action) {
        this.documentNodeRef = documentNodeRef;
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
                searchIntegrationService.pushUpdateToIndexService(documentNodeRef, action);
                } catch (Exception e) {
                  LOG.error("Exception when handling update to index service", e);
                  throw e;
                }
                return null;
              }

            }, false, true);

          }
        }, AuthenticationUtil.getSystemUserName());
      }
    }
  }
}
