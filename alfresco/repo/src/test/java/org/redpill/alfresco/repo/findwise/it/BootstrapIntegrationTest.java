package org.redpill.alfresco.repo.findwise.it;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.junit.Test;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;

/**
 * Bootstrap test, used to assure that everything is loaded okay
 * @author Marcus Svensson
 *
 */
public class BootstrapIntegrationTest extends AbstractRepoIntegrationTest {

  static SiteInfo site;
  static String siteManagerUser;

  @Override
  public void beforeClassSetup() {
    super.beforeClassSetup();

    _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());

    // Create a site
    site = createSite("site-dashboard");
    assertNotNull(site);

    // Create a user
    siteManagerUser = "sitemanager" + System.currentTimeMillis();
    createUser(siteManagerUser);

    // Make user the site manager of the site
    _siteService.setMembership(site.getShortName(), siteManagerUser, SiteModel.SITE_MANAGER);

    // Run the tests as this user
    _authenticationComponent.setCurrentUser(siteManagerUser);
  }

  @Override
  public void afterClassSetup() {
    super.afterClassSetup();
    _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
    _siteService.deleteSite(site.getShortName());
    _personService.deletePerson(siteManagerUser);
    if (_authenticationService.authenticationExists(siteManagerUser)) {
      _authenticationService.deleteAuthentication(siteManagerUser);
    }
    _authenticationComponent.clearCurrentSecurityContext();
  }

  @Test
  public void test() {

    Set<NodeRef> allRootNodes = _nodeService.getAllRootNodes(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

    assertNotNull(allRootNodes);
    assertTrue(allRootNodes.size() > 0);
  }

}
