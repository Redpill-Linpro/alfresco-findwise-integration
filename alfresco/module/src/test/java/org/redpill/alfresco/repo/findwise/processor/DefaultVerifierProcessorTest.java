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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.i18n.MessageLookup;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.repo.findwise.processor.NodeVerifierProcessor;

public class DefaultVerifierProcessorTest {

  DefaultVerifierProcessor defaultVerifierProcessor;
  NodeService nodeService;
  DictionaryService dictionaryService;
  Mockery m;
  final NodeRef nodeRef1 = new NodeRef("workspace://SpacesStore/nodeRef1");
  final NodeRef nodeRef2 = new NodeRef("workspace://SpacesStore/nodeRef2");

  @Before
  public void setup() throws Exception {
    m = new Mockery();
    defaultVerifierProcessor = new DefaultVerifierProcessor();
    nodeService = m.mock(NodeService.class);
    dictionaryService = m.mock(DictionaryService.class);

    defaultVerifierProcessor.setDictionaryService(dictionaryService);
    defaultVerifierProcessor.setNodeService(nodeService);
    defaultVerifierProcessor.afterPropertiesSet();
  }

  @After
  public void teardown() {

  }

  @Test
  public void nodeDoesNotExist() {
    m.checking(new Expectations() {
      {
        oneOf(nodeService).exists(nodeRef1);
        will(returnValue(false));
      }
    });
    
    assertFalse(defaultVerifierProcessor.verifyDocument(nodeRef1));    
    m.assertIsSatisfied();
  }
  
  @Test
  public void nodeIsNull() {
    m.checking(new Expectations() {
      {
        oneOf(nodeService).exists((NodeRef) null);
        will(returnValue(false));
      }
    });
    
    assertFalse(defaultVerifierProcessor.verifyDocument(null));    
    m.assertIsSatisfied();
  }
  
  @Test
  public void verifyOk() {
    final QName dummyQName = QName.createQName("test");
    m.checking(new Expectations() {
      {
        oneOf(nodeService).exists(nodeRef1);
        will(returnValue(true));
        oneOf(nodeService).getType(nodeRef1);
        will(returnValue(dummyQName));
        oneOf(dictionaryService).isSubClass(dummyQName, ContentModel.TYPE_FOLDER);
        will(returnValue(false));
      }
    });
    
    assertTrue(defaultVerifierProcessor.verifyDocument(nodeRef1));    
    m.assertIsSatisfied();
  }
  
  @Test
  public void verifyInvalidType() {
    final QName dummyQName = QName.createQName("test");
    m.checking(new Expectations() {
      {
        oneOf(nodeService).exists(nodeRef1);
        will(returnValue(true));
        oneOf(nodeService).getType(nodeRef1);
        will(returnValue(dummyQName));
        oneOf(dictionaryService).isSubClass(dummyQName, ContentModel.TYPE_FOLDER);
        will(returnValue(true));
      }
    });
    
    assertFalse(defaultVerifierProcessor.verifyDocument(nodeRef1));    
    m.assertIsSatisfied();
  }
  
}
