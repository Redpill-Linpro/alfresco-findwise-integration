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

package org.redpill.alfresco.repo.findwise.model;

import org.alfresco.service.namespace.QName;

public interface FindwiseIntegrationModel {
  public static final String URI = "http://www.redpill-linpro.com/model/findwise-integration-model/1.0";
  public static final String SHORT = "fwi";

  public final QName ASPECT_FINDWISE_INDEXABLE = QName.createQName(URI, "findwiseIndexable");
  public final QName PROP_LAST_PUSH_TO_INDEX = QName.createQName(URI, "lastPushToIndex");
  public final QName PROP_LAST_PUSH_FAILED = QName.createQName(URI, "lastPushFailed");
  public final QName PROP_IN_INDEX = QName.createQName(URI, "inIndex");
}
