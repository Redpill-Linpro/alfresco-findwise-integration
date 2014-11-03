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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.dictionary.IndexTokenisationMode;
import org.alfresco.repo.i18n.StaticMessageLookup;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.dictionary.ClassDefinition;
import org.alfresco.service.cmr.dictionary.ConstraintDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.i18n.MessageLookup;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.repo.findwise.model.FindwiseIntegrationModel;
import org.redpill.alfresco.repo.findwise.processor.NodeVerifierProcessor;

import com.ibm.wsdl.util.IOUtils;

public class SearchIntegrationServiceImplTest {

  SearchIntegrationServiceImpl searchIntegrationService;
  NodeService nodeService;
  NamespaceService namespaceService;
  DictionaryService dictionaryService;
  ContentService contentService;
  NodeVerifierProcessor nodeVerifierProcessor;
  BehaviourFilter behaviourFilter;
  ContentReader contentReader;
  SysAdminParams sysAdminParams;
  SiteService siteService;
  ClassDefinition classDefinition;
  SiteInfo siteInfo;
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
    contentReader = m.mock(ContentReader.class);
    sysAdminParams = m.mock(SysAdminParams.class);
    siteService = m.mock(SiteService.class);
    classDefinition = m.mock(ClassDefinition.class);
    siteInfo = m.mock(SiteInfo.class);
    searchIntegrationService.setNodeService(nodeService);
    searchIntegrationService.setDictionaryService(dictionaryService);
    searchIntegrationService.setNamespaceService(namespaceService);
    searchIntegrationService.setContentService(contentService);
    searchIntegrationService.setPushEnabled(true);
    searchIntegrationService.setPushUrl("http://127.0.0.1:51234/dummy/noenpoint");
    searchIntegrationService.setNodeVerifierProcessor(nodeVerifierProcessor);
    searchIntegrationService.setBehaviourFilter(behaviourFilter);
    searchIntegrationService.setSysAdminParams(sysAdminParams);
    searchIntegrationService.setSiteService(siteService);
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
  public void pushCreateToIndexService() throws Exception {
    searchIntegrationService.afterPropertiesSet();
    final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
    properties.put(ContentModel.PROP_NAME, "Name.txt");
    properties.put(ContentModel.PROP_CREATED, new Date());
    ContentData contentData = new ContentData("/test.txt", "text/text", 0L, "UTF-8");
    properties.put(ContentModel.PROP_CONTENT, contentData);

    final PropertyDefinition stringDef = new MockPropertyDefinition("java.lang.String");
    final PropertyDefinition dateDef = new MockPropertyDefinition("java.util.Date");
    final PropertyDefinition contentDef = new MockPropertyDefinition("org.alfresco.service.cmr.repository.ContentData");

    final Set<String> prefixes = new HashSet<String>();
    prefixes.add("cm");
    final InputStream stream = new ByteArrayInputStream("test data".getBytes(StandardCharsets.UTF_8));
    
    
    
    m.checking(new Expectations() {
      {
        allowing(nodeService).exists(nodeRef1);
        will(returnValue(false));
        allowing(nodeService).exists(nodeRef2);
        will(returnValue(true));
        oneOf(nodeService).getProperties(nodeRef2);
        will(returnValue(properties));
        oneOf(dictionaryService).getProperty(ContentModel.PROP_NAME);
        will(returnValue(stringDef));
        oneOf(dictionaryService).getProperty(ContentModel.PROP_CREATED);
        will(returnValue(dateDef));
        oneOf(dictionaryService).getProperty(ContentModel.PROP_CONTENT);
        will(returnValue(contentDef));
        allowing(namespaceService).getPrefixes(NamespaceService.CONTENT_MODEL_1_0_URI);
        will(returnValue(prefixes));
        oneOf(nodeVerifierProcessor).verifyDocument(nodeRef2);
        will(returnValue(true));
        oneOf(contentService).getReader(nodeRef2, ContentModel.PROP_CONTENT);
        will(returnValue(contentReader));
        oneOf(contentReader).getContentInputStream();
        will(returnValue(stream));
        oneOf(behaviourFilter).disableBehaviour(nodeRef2);
        oneOf(nodeService).setProperty(nodeRef2, FindwiseIntegrationModel.PROP_LAST_PUSH_FAILED, true);
        oneOf(nodeService).setProperty(with(nodeRef2), with(FindwiseIntegrationModel.PROP_LAST_PUSH_TO_INDEX), with(any(Date.class)));
        oneOf(behaviourFilter).enableBehaviour(nodeRef2);
        oneOf(nodeService).getType(nodeRef2);
        will(returnValue(ContentModel.TYPE_CONTENT));
        //oneOf(dictionaryService).getClass(ContentModel.TYPE_CONTENT);
        //will(returnValue(classDefinition));
        //oneOf(classDefinition).getTitle(with(any(StaticMessageLookup.class)));
        //will(returnValue("CustomType name"));
        oneOf(siteService).getSite(nodeRef2);
        will(returnValue(siteInfo));
        oneOf(siteInfo).getTitle();
        will(returnValue("Site title"));
        oneOf(siteInfo).getShortName();
        will(returnValue("asite"));
        oneOf(sysAdminParams).getShareProtocol();
        will(returnValue("http"));
        oneOf(sysAdminParams).getShareHost();
        will(returnValue("localhost"));
        oneOf(sysAdminParams).getSharePort();
        will(returnValue(8081));
        oneOf(sysAdminParams).getShareContext();
        will(returnValue("share"));
        
        
      }
    });
    
    searchIntegrationService.pushUpdateToIndexService(nodeRef1, SearchIntegrationService.ACTION_CREATE);
    
    Set<NodeRef> set = new HashSet<NodeRef>(1);
    set.add(nodeRef2);
    searchIntegrationService.pushUpdateToIndexService(set, SearchIntegrationService.ACTION_CREATE);

    m.assertIsSatisfied();
  }
  
  @Test
  public void pushDeleteToIndexService() throws Exception {
    searchIntegrationService.afterPropertiesSet();
     
    searchIntegrationService.pushUpdateToIndexService(nodeRef1, SearchIntegrationService.ACTION_DELETE);
    
    Set<NodeRef> set = new HashSet<NodeRef>(1);
    set.add(nodeRef2);
    searchIntegrationService.pushUpdateToIndexService(set, SearchIntegrationService.ACTION_DELETE);

    m.assertIsSatisfied();
  }
  
  class MockPropertyDefinition implements PropertyDefinition {
    public String javaClassName = "";
    MockPropertyDefinition(String javaClassName) {
      this.javaClassName = javaClassName;
    }
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
          return javaClassName;
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
}
