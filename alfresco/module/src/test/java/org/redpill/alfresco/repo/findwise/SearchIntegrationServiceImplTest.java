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

import static org.junit.Assert.assertFalse;

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

public class SearchIntegrationServiceImplTest {

  SearchIntegrationServiceImpl searchIntegrationService;
  NodeService nodeService;
  NamespaceService namespaceService;
  DictionaryService dictionaryService;
  ContentService contentService;
  NodeVerifierProcessor nodeVerifierProcessor;
  BehaviourFilter behaviourFilter;
  Mockery m;
  final NodeRef nodeRef1 = new NodeRef("workspace://SpacesStore/nodeRef1");
  final NodeRef nodeRef2 = new NodeRef("workspace://SpacesStore/nodeRef2");

  @Before
  public void setup() throws Exception {
    m = new Mockery();
    searchIntegrationService = new SearchIntegrationServiceImpl();
    nodeService = m.mock(NodeService.class);
    namespaceService = m.mock(NamespaceService.class);
    dictionaryService = m.mock(DictionaryService.class);
    contentService = m.mock(ContentService.class);
    nodeVerifierProcessor = m.mock(NodeVerifierProcessor.class);
    behaviourFilter = m.mock(BehaviourFilter.class);
    searchIntegrationService.setNodeService(nodeService);
    searchIntegrationService.setDictionaryService(dictionaryService);
    searchIntegrationService.setNamespaceService(namespaceService);
    searchIntegrationService.setContentService(contentService);
    searchIntegrationService.setPushEnabled(false);
    searchIntegrationService.setPushUrl("http://localhost:8083");
    searchIntegrationService.setNodeVerifierProcessor(nodeVerifierProcessor);
    searchIntegrationService.setBehaviourFilter(behaviourFilter);

    searchIntegrationService.afterPropertiesSet();
  }

  @After
  public void teardown() {

  }

  @Test
  public void pushUpdateToIndexServiceInvalidOperation() throws Exception {
    m.checking(new Expectations() {
      {
        allowing(nodeService).exists(nodeRef1);
        will(returnValue(true));
        allowing(nodeVerifierProcessor).verifyDocument(nodeRef1);
        will(returnValue(true));
      }
    });
    try {
      searchIntegrationService.pushUpdateToIndexService(nodeRef1, "someDummyValue");
      assertFalse(true);
    } catch (UnsupportedOperationException e) {

    }

    m.assertIsSatisfied();
  }

  @Test
  public void pushUpdateToIndexServiceVerifyFail() throws Exception {
    m.checking(new Expectations() {
      {
        allowing(nodeService).exists(nodeRef1);
        will(returnValue(true));
        allowing(nodeVerifierProcessor).verifyDocument(nodeRef1);
        will(returnValue(false));
      }
    });
    searchIntegrationService.pushUpdateToIndexService(nodeRef1, SearchIntegrationService.ACTION_CREATE);

    m.assertIsSatisfied();
  }

  @Test
  public void pushUpdateToIndexService() throws Exception {
    searchIntegrationService.afterPropertiesSet();
    final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
    properties.put(ContentModel.PROP_NAME, "Name.txt");
    final PropertyDefinition propDefString = new PropertyDefinition() {

      @Override
      public ModelDefinition getModel() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public QName getName() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getTitle() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getDescription() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getTitle(MessageLookup messageLookup) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getDescription(MessageLookup messageLookup) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getDefaultValue() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public DataTypeDefinition getDataType() {
        // TODO Auto-generated method stub
        return new DataTypeDefinition() {

          @Override
          public ModelDefinition getModel() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public QName getName() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getTitle() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getDescription() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getTitle(MessageLookup messageLookup) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getDescription(MessageLookup messageLookup) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getAnalyserResourceBundleName() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getJavaClassName() {
            // TODO Auto-generated method stub
            return "java.lang.String";
          }

          @Override
          public String getDefaultAnalyserClassName() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String resolveAnalyserClassName(Locale locale) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String resolveAnalyserClassName() {
            // TODO Auto-generated method stub
            return null;
          }

        };
      }

      @Override
      public ClassDefinition getContainerClass() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isOverride() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isMultiValued() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isMandatory() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isMandatoryEnforced() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isProtected() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isIndexed() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public boolean isStoredInIndex() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public IndexTokenisationMode getIndexTokenisationMode() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isIndexedAtomically() {
        // TODO Auto-generated method stub
        return false;
      }

      @Override
      public List<ConstraintDefinition> getConstraints() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getAnalyserResourceBundleName() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String resolveAnalyserClassName(Locale locale) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String resolveAnalyserClassName() {
        // TODO Auto-generated method stub
        return null;
      }

    };

    final Set<String> prefixes = new HashSet<String>();
    prefixes.add("cm");

    m.checking(new Expectations() {
      {
        allowing(nodeService).exists(nodeRef1);
        will(returnValue(false));
        allowing(nodeService).exists(nodeRef2);
        will(returnValue(true));
        oneOf(nodeService).getProperties(nodeRef2);
        will(returnValue(properties));
        oneOf(dictionaryService).getProperty(ContentModel.PROP_NAME);
        will(returnValue(propDefString));
        oneOf(namespaceService).getPrefixes(NamespaceService.CONTENT_MODEL_1_0_URI);
        will(returnValue(prefixes));
        oneOf(nodeVerifierProcessor).verifyDocument(nodeRef2);
        will(returnValue(true));
      }
    });
    searchIntegrationService.pushUpdateToIndexService(nodeRef1, SearchIntegrationService.ACTION_CREATE);
    searchIntegrationService.pushUpdateToIndexService(nodeRef2, SearchIntegrationService.ACTION_CREATE);

    m.assertIsSatisfied();
  }
}
