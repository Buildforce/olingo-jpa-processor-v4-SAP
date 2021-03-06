package nl.buildforce.sequoia.jpa.processor.core.query;

import nl.buildforce.sequoia.jpa.metadata.api.JPAEdmProvider;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAPath;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.exception.ODataJPAException;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataCRUDContextAccess;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataContextAccessDouble;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataGroupsProvider;
import nl.buildforce.sequoia.jpa.processor.core.exception.JPAIllegalAccessException;
import nl.buildforce.sequoia.jpa.processor.core.processor.JPAODataRequestContextImpl;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.Organization;
import nl.buildforce.sequoia.jpa.processor.core.util.TestBase;
import nl.buildforce.sequoia.jpa.processor.core.util.TestHelper;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestJPAQueryFromClause extends TestBase {
  private JPAAbstractJoinQuery cut;
  private JPAEntityType jpaEntityType;
  private JPAODataCRUDContextAccess sessionContext;

  @BeforeEach
  public void setup() throws ODataJPAException, JPAIllegalAccessException, ODataApplicationException {
    final UriInfo uriInfo = Mockito.mock(UriInfo.class);
    final EdmEntitySet odataEs = Mockito.mock(EdmEntitySet.class);
    final EdmType odataType = Mockito.mock(EdmEntityType.class);
    final List<UriResource> resources = new ArrayList<>();
    final UriResourceEntitySet esResource = Mockito.mock(UriResourceEntitySet.class);
    Mockito.when(uriInfo.getUriResourceParts()).thenReturn(resources);
    Mockito.when(esResource.getKeyPredicates()).thenReturn(new ArrayList<>(0));
    Mockito.when(esResource.getEntitySet()).thenReturn(odataEs);
    Mockito.when(esResource.getKind()).thenReturn(UriResourceKind.entitySet);
    Mockito.when(esResource.getType()).thenReturn(odataType);
    Mockito.when(odataEs.getName()).thenReturn("Organizations");
    Mockito.when(odataType.getNamespace()).thenReturn(PUNIT_NAME);
    Mockito.when(odataType.getName()).thenReturn("Organization");
    resources.add(esResource);

    helper = new TestHelper(emf, PUNIT_NAME);
    jpaEntityType = helper.getJPAEntityType("Organizations");
    sessionContext = new JPAODataContextAccessDouble(new JPAEdmProvider(PUNIT_NAME, emf, null, TestBase.enumPackages),
        ds, null);
    createHeaders();

    final JPAODataRequestContextImpl requestContext = new JPAODataRequestContextImpl();
    requestContext.setEntityManager(emf.createEntityManager());
    requestContext.setUriInfo(uriInfo);
    cut = new JPAJoinQuery(null, sessionContext, headers, requestContext);
  }

  @Test
  public void checkFromListContainsRoot() throws ODataApplicationException, JPANoSelectionException {

    Map<String, From<?, ?>> act = cut.createFromClause(Collections.emptyList(), Collections.emptyList(), cut.cq, null);
    assertNotNull(act.get(jpaEntityType.getExternalFQN().getFullQualifiedNameAsString()));
  }

  @Test
  public void checkFromListOrderByContainsOne() throws ODataJPAModelException, ODataApplicationException,
      JPANoSelectionException {
    final List<JPAAssociationPath> orderBy = new ArrayList<>();
    final JPAAssociationPath exp = buildRoleAssociationPath(orderBy);

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, new ArrayList<>(), cut.cq, null);
    assertNotNull(act.get(exp.getAlias()));
  }

  @Test
  public void checkFromListOrderByOuterJoinOne() throws ODataJPAModelException, ODataApplicationException,
      JPANoSelectionException {
    final List<JPAAssociationPath> orderBy = new ArrayList<>();
    buildRoleAssociationPath(orderBy);

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, new ArrayList<>(), cut.cq, null);

    @SuppressWarnings("unchecked")
    Root<Organization> root = (Root<Organization>) act.get(jpaEntityType.getExternalFQN()
        .getFullQualifiedNameAsString());
    Set<Join<Organization, ?>> joins = root.getJoins();
    assertEquals(1, joins.size());

    for (Join<Organization, ?> join : joins) {
      assertEquals(JoinType.LEFT, join.getJoinType());
    }
  }

  @Test
  public void checkFromListOrderByOuterJoinOnConditionOne() throws ODataJPAModelException, ODataApplicationException,
      JPANoSelectionException {
    final List<JPAAssociationPath> orderBy = new ArrayList<>();
    buildRoleAssociationPath(orderBy);

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, new ArrayList<>(), cut.cq, null);

    @SuppressWarnings("unchecked")
    Root<Organization> root = (Root<Organization>) act.get(jpaEntityType.getExternalFQN()
        .getFullQualifiedNameAsString());
    Set<Join<Organization, ?>> joins = root.getJoins();
    assertEquals(1, joins.size());

    for (Join<Organization, ?> join : joins) {
      assertNull(join.getOn());
    }
  }

  @Test
  public void checkFromListDescriptionAssociationAllFields() throws ODataApplicationException, ODataJPAModelException,
      JPANoSelectionException {
    List<JPAAssociationPath> orderBy = new ArrayList<>();
    List<JPAPath> descriptionPathList = new ArrayList<>();
    JPAEntityType entity = helper.getJPAEntityType("Organizations");
    descriptionPathList.add(entity.getPath("Address/CountryName"));

    JPAAttribute attri = helper.getJPAAttribute("Organizations", "address");
    JPAAttribute exp = attri.getStructuredType().getAttribute("countryName");

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, descriptionPathList, cut.cq, null);
    assertEquals(2, act.size());
    assertNotNull(act.get(exp.getInternalName()));
  }

  @Test
  public void checkFromListDescriptionAssociationAllFields2() throws ODataApplicationException, ODataJPAModelException,
      JPANoSelectionException {
    List<JPAAssociationPath> orderBy = new ArrayList<>();
    List<JPAPath> descriptionPathList = new ArrayList<>();
    JPAEntityType entity = helper.getJPAEntityType("Organizations");
    descriptionPathList.add(entity.getPath("Address/RegionName"));

    JPAAttribute attri = helper.getJPAAttribute("Organizations", "address");
    JPAAttribute exp = attri.getStructuredType().getAttribute("regionName");

    Map<String, From<?, ?>> act = cut.createFromClause(orderBy, descriptionPathList, cut.cq, null);
    assertEquals(2, act.size());
    assertNotNull(act.get(exp.getInternalName()));
  }

  @Test
  public void checkThrowsIfEliminatedByGroups() throws JPAIllegalAccessException, ODataException {

    final JPAODataRequestContextImpl requestContext = buildRequestContextToTestGroups();

    final List<JPAPath> collectionPathList = new ArrayList<>();
    JPAEntityType entity = helper.getJPAEntityType("BusinessPartnerWithGroupss");
    collectionPathList.add(entity.getPath("ID"));
    collectionPathList.add(entity.getPath("Comment"));

    cut = new JPAJoinQuery(null, sessionContext, headers, requestContext);

    assertThrows(JPANoSelectionException.class,
        () -> cut.createFromClause(Collections.emptyList(), collectionPathList, cut.cq, cut.lastInfo));

  }

  @Test
  public void checkDoesNotThrowsIfGroupProvided() throws JPAIllegalAccessException, ODataException,
      JPANoSelectionException {
    final JPAODataGroupsProvider groups = new JPAODataGroupsProvider();
    final JPAODataRequestContextImpl requestContext = buildRequestContextToTestGroups();
    requestContext.setGroupsProvider(groups);
    groups.addGroup("Company");
    final List<JPAPath> collectionPathList = new ArrayList<>();
    JPAEntityType entity = helper.getJPAEntityType("BusinessPartnerWithGroupss");
    collectionPathList.add(entity.getPath("ID"));
    collectionPathList.add(entity.getPath("Comment"));

    cut = new JPAJoinQuery(null, sessionContext, headers, requestContext);

    Map<String, From<?, ?>> act = cut.createFromClause(Collections.emptyList(), collectionPathList, cut.cq,
        cut.lastInfo);
    assertEquals(2, act.size());

  }

  private JPAODataRequestContextImpl buildRequestContextToTestGroups() throws JPAIllegalAccessException {
    final UriInfo uriInfo = Mockito.mock(UriInfo.class);
    final EdmEntitySet odataEs = Mockito.mock(EdmEntitySet.class);
    final EdmType odataType = Mockito.mock(EdmEntityType.class);
    final List<UriResource> resources = new ArrayList<>();
    final UriResourceEntitySet esResource = Mockito.mock(UriResourceEntitySet.class);
    final UriResourcePrimitiveProperty ppResource = Mockito.mock(UriResourcePrimitiveProperty.class);
    final EdmProperty ppProperty = Mockito.mock(EdmProperty.class);
    Mockito.when(uriInfo.getUriResourceParts()).thenReturn(resources);
    Mockito.when(esResource.getKeyPredicates()).thenReturn(new ArrayList<>(0));
    Mockito.when(esResource.getEntitySet()).thenReturn(odataEs);
    Mockito.when(esResource.getKind()).thenReturn(UriResourceKind.entitySet);
    Mockito.when(esResource.getType()).thenReturn(odataType);
    Mockito.when(odataEs.getName()).thenReturn("BusinessPartnerWithGroupss");
    Mockito.when(odataType.getNamespace()).thenReturn(PUNIT_NAME);
    Mockito.when(odataType.getName()).thenReturn("BusinessPartnerWithGroups");
    Mockito.when(ppResource.isCollection()).thenReturn(true);
    Mockito.when(ppResource.getProperty()).thenReturn(ppProperty);
    Mockito.when(ppProperty.getName()).thenReturn("Comment");
    resources.add(esResource);
    resources.add(ppResource);

    final JPAODataRequestContextImpl requestContext = new JPAODataRequestContextImpl();
    requestContext.setEntityManager(emf.createEntityManager());
    requestContext.setUriInfo(uriInfo);
    return requestContext;
  }

  private JPAAssociationPath buildRoleAssociationPath(final List<JPAAssociationPath> orderBy)
      throws ODataJPAModelException {
    JPAAssociationPath exp = helper.getJPAAssociationPath("Organizations", "Roles");
    orderBy.add(exp);
    return exp;
  }

}