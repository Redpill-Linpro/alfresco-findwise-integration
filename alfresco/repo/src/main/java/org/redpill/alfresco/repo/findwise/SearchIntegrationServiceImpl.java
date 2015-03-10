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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.UrlUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.redpill.alfresco.repo.findwise.bean.FindwiseFieldBean;
import org.redpill.alfresco.repo.findwise.bean.FindwiseObjectBean;
import org.redpill.alfresco.repo.findwise.model.FindwiseIntegrationModel;
import org.redpill.alfresco.repo.findwise.processor.NodeVerifierProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.util.Assert;

import com.google.gson.Gson;

public class SearchIntegrationServiceImpl implements SearchIntegrationService, InitializingBean {
  private static final Logger LOG = Logger.getLogger(SearchIntegrationServiceImpl.class);
  protected NodeService nodeService;
  protected DictionaryService dictionaryService;
  protected NamespaceService namespaceService;
  protected ContentService contentService;
  protected BehaviourFilter behaviourFilter;
  protected Boolean pushEnabled;
  protected String pushUrl;
  protected NodeVerifierProcessor nodeVerifierProcessor;
  protected SysAdminParams sysAdminParams;
  protected SiteService siteService;
  protected PersonService personService;

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(nodeService);
    Assert.notNull(namespaceService);
    Assert.notNull(dictionaryService);
    Assert.notNull(contentService);
    Assert.notNull(pushEnabled);
    Assert.notNull(pushUrl);
    Assert.notNull(nodeVerifierProcessor);
    Assert.notNull(behaviourFilter);
    Assert.notNull(sysAdminParams);
    Assert.notNull(siteService);
    Assert.notNull(personService);
  }

  public void setPersonService(PersonService personService) {
    this.personService = personService;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void setSysAdminParams(SysAdminParams sysAdminParams) {
    this.sysAdminParams = sysAdminParams;
  }

  public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
    this.behaviourFilter = behaviourFilter;
  }

  @Override
  public void setNodeVerifierProcessor(NodeVerifierProcessor nodeVerifierProcessor) {
    this.nodeVerifierProcessor = nodeVerifierProcessor;
  }

  public void setNodeService(NodeService nodeService) {
    this.nodeService = nodeService;
  }

  public void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  public void setNamespaceService(NamespaceService namespaceService) {
    this.namespaceService = namespaceService;
  }

  public void setContentService(ContentService contentService) {
    this.contentService = contentService;
  }

  public void setPushEnabled(Boolean pushEnabled) {
    this.pushEnabled = pushEnabled;
  }

  public void setPushUrl(String pushUrl) {
    this.pushUrl = pushUrl;
  }

  @Override
  public boolean pushUpdateToIndexService(final Set<NodeRef> documentNodeRefs, final String action) {
    boolean result = true;
    Iterator<NodeRef> iterator = documentNodeRefs.iterator();
    while (iterator.hasNext()) {
      boolean thisResult = pushUpdateToIndexService(iterator.next(), action);
      result = result && thisResult;
    }
    return result;
  }

  @Override
  public boolean pushUpdateToIndexService(final NodeRef nodeRef, String action) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("pushUpdateToIndexService begin");
    }
    boolean send = false;
    List<FindwiseObjectBean> fobs = new ArrayList<FindwiseObjectBean>();
    if (!ACTION_CREATE.equals(action) && !ACTION_DELETE.equals(action)) {
      throw new UnsupportedOperationException(action + " is not a supported operation");
    }

    if (ACTION_CREATE.equals(action)) {
      if (nodeRef == null || !nodeService.exists(nodeRef)) {
        LOG.debug(nodeRef + " does not exist");
      } else if (!nodeVerifierProcessor.verifyDocument(nodeRef)) {
        Boolean isInIndex = (Boolean) nodeService.getProperty(nodeRef, FindwiseIntegrationModel.PROP_IN_INDEX);
        if (Boolean.TRUE.equals(isInIndex)) {
          action = ACTION_DELETE;
        } else {
          LOG.debug(nodeRef + " did not pass final node verification");
        }
      } else {
        FindwiseObjectBean fob = createFindwiseObjectBean(nodeRef, false);
        fobs.add(fob);
        send = true;
      }
    }

    if (ACTION_DELETE.equals(action)) {
      FindwiseObjectBean fob = createFindwiseObjectBean(nodeRef, true);
      fobs.add(fob);
      send = true;
    }
    boolean pushResult = false;
    if (send && ACTION_CREATE.equals(action)) {
      Gson gson = new Gson();
      String json = gson.toJson(fobs);
      if (LOG.isTraceEnabled()) {
        String jsonString = (json.length() > 2048) ? json.substring(0, 2048) : json;
        LOG.trace("Json: " + jsonString + "...");
      }
      if (Boolean.TRUE.equals(pushEnabled)) {
        pushResult = doPost(json);
      } else {
        LOG.info("Push is disabled");
      }
    } else if (send && ACTION_DELETE.equals(action)) {
      if (Boolean.TRUE.equals(pushEnabled)) {
        pushResult = doDelete(nodeRef);
      } else {
        LOG.info("Push is disabled");
      }
    }

    if (send && nodeService.exists(nodeRef) && !nodeService.hasAspect(nodeRef, ContentModel.ASPECT_PENDING_DELETE)) {
      LOG.debug("Setting push result on node " + nodeRef);

      behaviourFilter.disableBehaviour(nodeRef);

      nodeService.setProperty(nodeRef, FindwiseIntegrationModel.PROP_LAST_PUSH_TO_INDEX, new Date());
      if (pushResult == true) {
        // Success
        nodeService.setProperty(nodeRef, FindwiseIntegrationModel.PROP_LAST_PUSH_FAILED, false);
        nodeService.setProperty(nodeRef, FindwiseIntegrationModel.PROP_IN_INDEX, !ACTION_DELETE.equals(action));
      } else {
        // Failed
        nodeService.setProperty(nodeRef, FindwiseIntegrationModel.PROP_LAST_PUSH_FAILED, true);
      }
      behaviourFilter.enableBehaviour(nodeRef);
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace("pushUpdateToIndexService end");
    }
    return pushResult;
  }

  protected boolean isPropertyAllowedToIndex(QName property) {
    if (ContentModel.PROP_AUTO_VERSION.equals(property)) {
      return false;
    } else if (ContentModel.PROP_AUTO_VERSION_PROPS.equals(property)) {
      return false;
    } else if (ContentModel.PROP_INITIAL_VERSION.equals(property)) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * Create bean object from node properties
   * 
   * @param nodeRef
   * @return
   */
  protected FindwiseObjectBean createFindwiseObjectBean(final NodeRef nodeRef, boolean empty) {
    FindwiseObjectBean fob = new FindwiseObjectBean();
    fob.setId(nodeRef.toString());
    if (empty == false) {
      List<FindwiseFieldBean> fields = new ArrayList<FindwiseFieldBean>();

      Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

      // Get node type
      QName nodeType = nodeService.getType(nodeRef);
      String title = nodeType.toPrefixString(namespaceService);
      fields.add(new FindwiseFieldBean("type", "string", title));

      // Get site info
      SiteInfo site = siteService.getSite(nodeRef);
      String siteName = site.getTitle();
      String siteShortName = site.getShortName();
      fields.add(new FindwiseFieldBean("siteName", "string", siteName));
      fields.add(new FindwiseFieldBean("siteShortName", "string", siteShortName));

      // Get download & details url
      String shareUrl = UrlUtil.getShareUrl(sysAdminParams);
      String downloadPath = "/proxy/alfresco/api/node/content/" + nodeRef.toString().replace("://", "/");
      downloadPath += "/" + URLEncoder.encode((String) properties.get(ContentModel.PROP_NAME));
      downloadPath += "?a=true";
      String downloadUrl = shareUrl + downloadPath;
      String detailsPath = "/page/site/" + siteShortName + "/document-details?nodeRef=" + URLEncoder.encode(nodeRef.toString());
      String detailsUrl = shareUrl + detailsPath;
      fields.add(new FindwiseFieldBean("downloadUrl", "string", downloadUrl));
      fields.add(new FindwiseFieldBean("detailsUrl", "string", detailsUrl));

      // Add more user info for creator
      NodeRef creatorNodeRef = personService.getPersonOrNull((String) properties.get(ContentModel.PROP_CREATOR));
      if (creatorNodeRef != null) {
        Map<QName, Serializable> personProperties = nodeService.getProperties(creatorNodeRef);
        String firstName = (String) personProperties.get(ContentModel.PROP_FIRSTNAME);
        if (firstName == null) {
          firstName = "";
        }
        fields.add(new FindwiseFieldBean("creatorFirstName", "string", firstName));
        String lastName = (String) personProperties.get(ContentModel.PROP_LASTNAME);
        if (lastName == null) {
          lastName = "";
        }
        fields.add(new FindwiseFieldBean("creatorLastName", "string", lastName));
        fields.add(new FindwiseFieldBean("creatorFullName", "string", firstName + " " + lastName));
        String email = (String) personProperties.get(ContentModel.PROP_EMAIL);
        if (email == null) {
          email = "";
        }
        fields.add(new FindwiseFieldBean("creatorEmail", "string", email));
      }

      // Add more user info for modifier
      NodeRef modifierNodeRef = personService.getPersonOrNull((String) properties.get(ContentModel.PROP_MODIFIER));
      if (modifierNodeRef != null) {
        Map<QName, Serializable> personProperties = nodeService.getProperties(modifierNodeRef);
        String firstName = (String) personProperties.get(ContentModel.PROP_FIRSTNAME);
        if (firstName == null) {
          firstName = "";
        }
        fields.add(new FindwiseFieldBean("modifierFirstName", "string", firstName));
        String lastName = (String) personProperties.get(ContentModel.PROP_LASTNAME);
        if (lastName == null) {
          lastName = "";
        }
        fields.add(new FindwiseFieldBean("modifierLastName", "string", lastName));
        fields.add(new FindwiseFieldBean("modifierFullName", "string", firstName + " " + lastName));
        String email = (String) personProperties.get(ContentModel.PROP_EMAIL);
        if (email == null) {
          email = "";
        }
        fields.add(new FindwiseFieldBean("modifierEmail", "string", email));
      }

      Iterator<QName> it = properties.keySet().iterator();
      while (it.hasNext()) {
        FindwiseFieldBean ffb = new FindwiseFieldBean();
        QName property = it.next();
        if (LOG.isTraceEnabled()) {
          LOG.trace("Handling property " + property.toString());
        }
        Serializable value = properties.get(property);
        if (NamespaceService.SYSTEM_MODEL_1_0_URI.equals(property.getNamespaceURI()) || FindwiseIntegrationModel.URI.equals(property.getNamespaceURI()) || !isPropertyAllowedToIndex(property)) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Skiping property " + property.toString());
          }
          continue;
        }
        String javaClassName = "unknown";
        PropertyDefinition propertyDefinition = dictionaryService.getProperty(property);
        // The following condition is needed to properly handle residual
        // properties
        if (propertyDefinition != null) {
          DataTypeDefinition dataType = propertyDefinition.getDataType();
          if (dataType != null) {
            javaClassName = dataType.getJavaClassName();
          }
        }
        String type;
        if (LOG.isTraceEnabled()) {
          LOG.trace("Detected " + javaClassName + " java type for property " + property.toString());
        }

        if ("java.util.Date".equals(javaClassName)) {
          if (LOG.isTraceEnabled()) {
            LOG.trace("Converting " + property.toString() + " to date");
          }
          type = "string";
          DateTime date = new DateTime((Date) value, DateTimeZone.UTC);
          ffb.setValue(date.toString());
        } else if ("org.alfresco.service.cmr.repository.ContentData".equals(javaClassName)) {
          // Create Base64 data
          if (LOG.isTraceEnabled()) {
            LOG.trace("Handling content on property " + property.toString());
          }
          ContentReader contentReader = contentService.getReader(nodeRef, property);
          if (contentReader != null) {
            InputStream nodeIS = new BufferedInputStream(contentReader.getContentInputStream(), 4096);

            try {
              byte[] nodeBytes = IOUtils.toByteArray(nodeIS);
              ffb.setValue(new String(Base64.encodeBase64(nodeBytes)));
            } catch (IOException e) {
              LOG.warn("Error while reading content", e);
            }
          } else {
            LOG.warn(nodeRef + " had no content");
          }
          type = "binary";
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Unhandled property type, using default conversion");
          }
          type = "string";
          ffb.setValue(value.toString());
        }
        ffb.setType(type);

        String name = property.toPrefixString(namespaceService);
        if (LOG.isTraceEnabled()) {
          LOG.trace("Short name for property " + property.toString() + ": " + name);
        }

        ffb.setName(name);

        fields.add(ffb);
      }
      fob.setFields(fields);
    }
    return fob;
  }

  protected boolean doPost(final String json) {
    boolean result = false;
    DefaultHttpClient httpclient = new DefaultHttpClient();
    try {
      HttpPost httpPost = new HttpPost(pushUrl);
      StringEntity entity = new StringEntity(json, "UTF-8");
      httpPost.setEntity(entity);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Executing request: " + httpPost.getRequestLine());
      }
      httpPost.addHeader("Content-Type", "application/json;charset=UTF-8");
      HttpResponse response = httpclient.execute(httpPost);
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Response" + response.getStatusLine());
        }
        EntityUtils.consume(response.getEntity());
        result = true;
      } finally {
        // response.close();
      }
    } catch (UnsupportedEncodingException e) {
      String jsonString = (json.length() > 2048) ? json.substring(0, 2048) : json;
      LOG.warn("Error transforming json to http entity. Json: " + jsonString + "...", e);
    } catch (Exception e) {
      String jsonString = (json.length() > 2048) ? json.substring(0, 2048) : json;
      LOG.warn("Error executing http post to " + pushUrl + " Json: " + jsonString + "...", e);
    } finally {
      /*
       * try { //httpclient.close(); } catch (IOException e) {
       * LOG.warn("Error making post to " + pushUrl, e); }
       */
    }
    return result;
  }

  // TODO delete
  protected boolean doDelete(final NodeRef nodeRef) {
    boolean result = false;
    DefaultHttpClient httpclient = new DefaultHttpClient();
    try {

      String queryString = "?ids=" + URLEncoder.encode(nodeRef.toString());
      HttpDelete httpDelete = new HttpDelete(pushUrl + queryString);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Executing request: " + httpDelete.getRequestLine());
      }
      httpDelete.addHeader("Content-Type", "application/json;charset=UTF-8");
      HttpResponse response = httpclient.execute(httpDelete);
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Response" + response.getStatusLine());
        }
        EntityUtils.consume(response.getEntity());
        result = true;
      } finally {
        // response.close();
      }
    } catch (Exception e) {
      LOG.warn("Error executing http delete to " + pushUrl + " for " + nodeRef, e);
    } finally {
      /*
       * try { //httpclient.close(); } catch (IOException e) {
       * LOG.warn("Error making post to " + pushUrl, e); }
       */
    }
    return result;
  }

}
