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

package org.redpill.alfresco.repo.findwise.bean;

import java.io.Serializable;
import java.util.List;

public class FindwiseObjectBean implements Serializable {
  private static final long serialVersionUID = 4279869574525460073L;
  private String id;
  private List<FindwiseFieldBean> fields;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<FindwiseFieldBean> getFields() {
    return fields;
  }

  public void setFields(List<FindwiseFieldBean> fields) {
    this.fields = fields;
  }
}
