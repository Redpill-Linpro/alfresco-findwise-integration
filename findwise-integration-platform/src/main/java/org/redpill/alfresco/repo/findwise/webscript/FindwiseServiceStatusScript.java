package org.redpill.alfresco.repo.findwise.webscript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.slingshot.web.scripts.NodeBrowserScript;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.redpill.alfresco.repo.findwise.model.FindwiseIntegrationModel;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Script which enriches node browser search script with properties
 * 
 * @author Marcus Svensson - Redpill Linpro AB
 *
 */
public class FindwiseServiceStatusScript extends NodeBrowserScript {
  private static final Logger LOG = Logger.getLogger(FindwiseServiceStatusScript.class);

  private NodeService nodeService;
  private SiteService siteService;
  private FileFolderService fileFolderService;

  @SuppressWarnings("unchecked")
  @Override
  protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
    if (LOG.isTraceEnabled()) {
      LOG.trace(FindwiseServiceStatusScript.class.getName() + " begin");
    }
    Map<String, Object> tmplMap = new HashMap<String, Object>(1);
    if (req.getPathInfo().equals("/findwise/node/search")) {
      List<Node> nodes;

      try {
        if (req.getParameter("store") == null || req.getParameter("store").length() == 0) {
          status.setCode(HttpServletResponse.SC_BAD_REQUEST);
          status.setMessage("Store name not provided");
          status.setRedirect(true);
          return null;
        }
        if (req.getParameter("q") == null || req.getParameter("q").length() == 0) {
          status.setCode(HttpServletResponse.SC_BAD_REQUEST);
          status.setMessage("Search query not provided");
          status.setRedirect(true);
          return null;
        }
        if (req.getParameter("lang") == null || req.getParameter("lang").length() == 0) {
          status.setCode(HttpServletResponse.SC_BAD_REQUEST);
          status.setMessage("Search language not provided");
          status.setRedirect(true);
          return null;
        }

        int maxResult = 0;
        try {
          maxResult = Integer.parseInt(req.getParameter("maxResults"));
        } catch (NumberFormatException ex) {
        }

        nodes = submitSearch(req.getParameter("store"), req.getParameter("q"), req.getParameter("lang"), maxResult);
        List<FindwiseNode> decoratedNodes = new ArrayList<FindwiseNode>();
        decoratedNodes.addAll((List<FindwiseNode>) CollectionUtils.collect(nodes, new Transformer() {
          @Override
          public Object transform(Object input) {
            return new FindwiseNode((Node) input);
          }
        }));
        tmplMap.put("results", decoratedNodes);
      } catch (IOException e) {
        status.setCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        status.setMessage(e.getMessage());
        status.setException(e);
        status.setRedirect(true);
      }

    }
    if (LOG.isTraceEnabled()) {
      LOG.trace(FindwiseServiceStatusScript.class.getName() + " end");
    }
    return tmplMap;
  }

  @Override
  public void setNodeService(NodeService nodeService) {
    super.setNodeService(nodeService);
    this.nodeService = nodeService;
  }

  public void setSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  public void setFileFolderService(FileFolderService fileFolderService) {
    this.fileFolderService = fileFolderService;
  }

  /**
   * Node wrapper class
   */
  public class FindwiseNode extends Node {
    private Date lastPushToIndex;
    private Boolean lastPushFailed;
    private Boolean inIndex;
    private String siteName;
    private String siteShortName;
    private String path;
    private String name;

    public FindwiseNode(NodeRef nodeRef) {
      super(nodeRef);
      this.name = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
      if (nodeService.hasAspect(nodeRef, FindwiseIntegrationModel.ASPECT_FINDWISE_INDEXABLE)) {
        this.lastPushToIndex = (Date) nodeService.getProperty(nodeRef, FindwiseIntegrationModel.PROP_LAST_PUSH_TO_INDEX);
        this.lastPushFailed = (Boolean) nodeService.getProperty(nodeRef, FindwiseIntegrationModel.PROP_LAST_PUSH_FAILED);
        this.inIndex = (Boolean) nodeService.getProperty(nodeRef, FindwiseIntegrationModel.PROP_IN_INDEX);
      }
      SiteInfo site = siteService.getSite(nodeRef);
      if (site != null) {
        this.siteName = site.getTitle();
        this.siteShortName = site.getShortName();
      }

      try {
        if (site == null) {
          List<String> nameOnlyPath = fileFolderService.getNameOnlyPath(null, nodeRef);
          path = "/" + StringUtils.join(nameOnlyPath.subList(0, nameOnlyPath.size() - 2), "/");
        } else {
          List<String> nameOnlyPath = fileFolderService.getNameOnlyPath(site.getNodeRef(), nodeRef);
          path = "/" + StringUtils.join(nameOnlyPath.subList(1, nameOnlyPath.size() - 2), "/");
        }
      } catch (FileNotFoundException e) {
        path = "";
      }

    }

    public String getSiteName() {
      return siteName;
    }

    public void setSiteName(String siteName) {
      this.siteName = siteName;
    }

    public String getSiteShortName() {
      return siteShortName;
    }

    public void setSiteShortName(String siteShortName) {
      this.siteShortName = siteShortName;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public FindwiseNode(Node node) {
      this(node.getNodeRef());
    }

    public Date getLastPushToIndex() {
      return lastPushToIndex;
    }

    public void setLastPushToIndex(Date lastPushToIndex) {
      this.lastPushToIndex = lastPushToIndex;
    }

    public Boolean getLastPushFailed() {
      return lastPushFailed;
    }

    public void setLastPushFailed(Boolean lastPushFailed) {
      this.lastPushFailed = lastPushFailed;
    }

    public Boolean getInIndex() {
      return inIndex;
    }

    public void setInIndex(Boolean inIndex) {
      this.inIndex = inIndex;
    }

  }
}
