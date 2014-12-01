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

package org.redpill.alfresco.repo.findwise.processor;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class DefaultVerifierProcessor implements NodeVerifierProcessor, InitializingBean {

  private static final Logger LOG = Logger.getLogger(DefaultVerifierProcessor.class);

  protected NodeService nodeService;
  protected DictionaryService dictionaryService;
  protected ContentService contentService;
  protected Long maxFileSize;

  public boolean verifyDocument(final NodeRef node) {

    if (LOG.isTraceEnabled()) {
      LOG.trace("Starting to execute DefaultVerifierProcessor#verifyDocument");
    }

    boolean verified = true;

    if (!nodeService.exists(node)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node " + node + " ignored (node does not exist)");
      }
      return false;
    } else if (dictionaryService.isSubClass(nodeService.getType(node), ContentModel.TYPE_FOLDER)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node " + node + " ignored, its a sub type of folder");
      }

      return false;
    }

    ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
    if (reader.getSize() > maxFileSize) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("File size for node " + node + " is too big");
      }
      return false;
    }
    return true;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  public void setMaxFileSize(Long maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  public void setContentService(ContentService contentService) {
    this.contentService = contentService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(nodeService);
    Assert.notNull(dictionaryService);
    Assert.notNull(contentService);
    Assert.notNull(maxFileSize);
  }
}
