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

import java.util.HashSet;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Verify that a node should be sent for indexing
 * 
 * @author Marcus Svensson - Redpill Linpro AB
 *
 */
public class DefaultVerifierProcessor implements NodeVerifierProcessor, InitializingBean {

  private static final Logger LOG = Logger.getLogger(DefaultVerifierProcessor.class);

  protected NodeService nodeService;
  protected DictionaryService dictionaryService;
  protected ContentService contentService;
  protected Long maxFileSize;
  protected Set<String> fileExtensions;

  public boolean verifyDocument(final NodeRef node) {

    if (LOG.isTraceEnabled()) {
      LOG.trace("Starting to execute " + DefaultVerifierProcessor.class.getName() + ".verifyDocument");
    }

    // Check that the node exists
    if (!nodeService.exists(node)) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Node " + node + " ignored (node does not exist)");
      }
      return false;
    }

    // Check that the node is not of type folder
    if (dictionaryService.isSubClass(nodeService.getType(node), ContentModel.TYPE_FOLDER)) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Node " + node + " ignored, its a sub type of folder");
      }

      return false;
    }

    // Check file size
    ContentReader reader = contentService.getReader(node, ContentModel.PROP_CONTENT);
    if (reader.getSize() > maxFileSize) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("File size for node " + node + " is too big");
      }
      return false;
    }

    // Check file extension
    String fileName = (String) nodeService.getProperty(node, ContentModel.PROP_NAME);
    String extension = FilenameUtils.getExtension(fileName);
    if (!fileExtensions.contains(extension)) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("File extension " + extension + " for " + node + " is not allowed");
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
    if (LOG.isTraceEnabled()) {
      LOG.trace("Max file size set to: " + maxFileSize);
    }
    this.maxFileSize = maxFileSize;
  }

  public void setContentService(ContentService contentService) {
    this.contentService = contentService;
  }

  public void setFileExtensions(String fileExtensions) {
    this.fileExtensions = new HashSet<String>();
    String[] split = fileExtensions.split(",");
    for (String ext : split) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Allowing file extension: " + ext);
      }
      this.fileExtensions.add(ext.trim());
    }   
    
    if (split.length <= StringUtils.countMatches(fileExtensions, ",")) {
      this.fileExtensions.add("");
      if (LOG.isTraceEnabled()) {
        LOG.trace("Allowing empty file extension");
      }
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(nodeService);
    Assert.notNull(dictionaryService);
    Assert.notNull(contentService);
    Assert.notNull(maxFileSize);
    Assert.notNull(fileExtensions);
    LOG.info("Initialized " + DefaultVerifierProcessor.class.getName());
  }
}
