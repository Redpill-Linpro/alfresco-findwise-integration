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

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultVerifierProcessorTest {

  DefaultVerifierProcessor defaultVerifierProcessor;
  NodeService nodeService;
  DictionaryService dictionaryService;
  ContentService contentService;
  ContentReader contentReader;
  Mockery m;
  final NodeRef nodeRef1 = new NodeRef("workspace://SpacesStore/nodeRef1");
  final NodeRef nodeRef2 = new NodeRef("workspace://SpacesStore/nodeRef2");
  final String fileExtensions = "doc,docx,";
  private long maxFileSize = 10L;

  @Before
  public void setup() throws Exception {
    m = new Mockery();
    defaultVerifierProcessor = new DefaultVerifierProcessor();
    nodeService = m.mock(NodeService.class);
    dictionaryService = m.mock(DictionaryService.class);
    contentService = m.mock(ContentService.class);
    contentReader = m.mock(ContentReader.class);
    defaultVerifierProcessor.setDictionaryService(dictionaryService);
    defaultVerifierProcessor.setNodeService(nodeService);
    defaultVerifierProcessor.setContentService(contentService);
    defaultVerifierProcessor.setFileExtensions(fileExtensions);
    defaultVerifierProcessor.setMaxFileSize(maxFileSize);
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
  
  @Test
  public void verifyTooBigFileSize() {
    final QName dummyQName = QName.createQName("test");
    m.checking(new Expectations() {
      {
        oneOf(nodeService).exists(nodeRef1);
        will(returnValue(true));
        oneOf(nodeService).getType(nodeRef1);
        will(returnValue(dummyQName));
        oneOf(dictionaryService).isSubClass(dummyQName, ContentModel.TYPE_FOLDER);
        will(returnValue(false));
        oneOf(contentService).getReader(nodeRef1, ContentModel.PROP_CONTENT);
        will(returnValue(contentReader));
        oneOf(contentReader).getSize();
        will(returnValue(15L));
      }
    });

    assertFalse(defaultVerifierProcessor.verifyDocument(nodeRef1));
    m.assertIsSatisfied();
  }
  
  @Test
  public void verifyInvalidFileExtension() {
    final QName dummyQName = QName.createQName("test");
    m.checking(new Expectations() {
      {
        oneOf(nodeService).exists(nodeRef1);
        will(returnValue(true));
        oneOf(nodeService).getType(nodeRef1);
        will(returnValue(dummyQName));
        oneOf(dictionaryService).isSubClass(dummyQName, ContentModel.TYPE_FOLDER);
        will(returnValue(false));
        oneOf(contentService).getReader(nodeRef1, ContentModel.PROP_CONTENT);
        will(returnValue(contentReader));
        oneOf(contentReader).getSize();
        will(returnValue(5L));
        oneOf(nodeService).getProperty(nodeRef1, ContentModel.PROP_NAME); 
        will(returnValue("test.bmp"));
      }
    });

    assertFalse(defaultVerifierProcessor.verifyDocument(nodeRef1));
    m.assertIsSatisfied();
  }
  
  @Test
  public void verifyEmptyFileExtension() {
    final QName dummyQName = QName.createQName("test");
    m.checking(new Expectations() {
      {
        oneOf(nodeService).exists(nodeRef1);
        will(returnValue(true));
        oneOf(nodeService).getType(nodeRef1);
        will(returnValue(dummyQName));
        oneOf(dictionaryService).isSubClass(dummyQName, ContentModel.TYPE_FOLDER);
        will(returnValue(false));
        oneOf(contentService).getReader(nodeRef1, ContentModel.PROP_CONTENT);
        will(returnValue(contentReader));
        oneOf(contentReader).getSize();
        will(returnValue(5L));
        oneOf(nodeService).getProperty(nodeRef1, ContentModel.PROP_NAME); 
        will(returnValue("test"));
      }
    });

    assertTrue(defaultVerifierProcessor.verifyDocument(nodeRef1));
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
        oneOf(contentService).getReader(nodeRef1, ContentModel.PROP_CONTENT);
        will(returnValue(contentReader));
        oneOf(contentReader).getSize();
        will(returnValue(5L));
        oneOf(nodeService).getProperty(nodeRef1, ContentModel.PROP_NAME); 
        will(returnValue("test.doc"));
      }
    });

    assertTrue(defaultVerifierProcessor.verifyDocument(nodeRef1));
    m.assertIsSatisfied();
  }

}
