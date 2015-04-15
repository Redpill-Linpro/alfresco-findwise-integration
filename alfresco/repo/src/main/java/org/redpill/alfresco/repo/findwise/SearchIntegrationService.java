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

import java.util.Set;

import org.alfresco.service.cmr.repository.NodeRef;
import org.redpill.alfresco.repo.findwise.processor.NodeVerifierProcessor;

public interface SearchIntegrationService {

  public static final String ACTION_CREATE = "create";
  public static final String ACTION_DELETE = "delete";

  /**
   * Push a node to the indexing service
   * 
   * @param nodeRef
   *          The nodeRef
   * @param action
   *          The action
   */
  public boolean pushUpdateToIndexService(NodeRef nodeRef, String action);

  /**
   * Push multiple nodes to the indexing service
   * 
   * @param nodeRefs
   *          The nodeRefs
   * @param action
   *          The action
   */
  public boolean pushUpdateToIndexService(Set<NodeRef> nodeRefs, String action);

  /**
   * Set the node verifier processor programmatically to override the default
   * 
   * @param nodeVerifierProcessor
   */
  public void setNodeVerifierProcessor(NodeVerifierProcessor nodeVerifierProcessor);
}
