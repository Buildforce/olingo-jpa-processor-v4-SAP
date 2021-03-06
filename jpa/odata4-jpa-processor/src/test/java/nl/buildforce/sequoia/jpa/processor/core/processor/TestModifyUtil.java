package nl.buildforce.sequoia.jpa.processor.core.processor;

import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAAssociationPath;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.exception.ODataJPAException;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPAInvocationTargetException;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPAProcessException;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPAProcessorException;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.*;
import nl.buildforce.sequoia.jpa.processor.core.testobjects.BusinessPartnerRoleWithoutSetter;
import nl.buildforce.sequoia.jpa.processor.core.testobjects.OrganizationWithoutGetter;
import nl.buildforce.sequoia.jpa.processor.core.util.TestBase;
import nl.buildforce.sequoia.jpa.processor.core.util.TestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestModifyUtil extends TestBase {
  private JPAModifyUtil cut;
  private Map<String, Object> jpaAttributes;
  private BusinessPartner partner;
  private JPAEntityType org;

  @BeforeEach
  public void setUp() throws ODataJPAException {
    cut = new JPAModifyUtil();
    jpaAttributes = new HashMap<>();
    partner = new Organization();
    helper = new TestHelper(emf, PUNIT_NAME);
    org = helper.getJPAEntityType("Organizations");
  }

  @Test
  public void testSetAttributeOneAttribute() throws ODataJPAProcessException {
    jpaAttributes.put("iD", "Willi");
    cut.setAttributes(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
  }

  @Test
  public void testSetAttributeMultipleAttribute() throws ODataJPAProcessException {
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("type", "2");
    cut.setAttributes(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
    assertEquals("2", partner.getType());
  }

  @Test
  public void testSetAttributeIfAttributeNull() throws ODataJPAProcessException {
    partner.setType("2");
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("type", null);
    cut.setAttributes(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
    assertNull(partner.getType());
  }

  @Test
  public void testDoNotSetAttributeIfNotInMap() throws ODataJPAProcessException {
    partner.setType("2");
    jpaAttributes.put("iD", "Willi");
    cut.setAttributes(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
    assertEquals("2", partner.getType());
  }

  @Test
  public void testSetAttributesDeepOneAttribute() throws ODataJPAProcessException {
    jpaAttributes.put("iD", "Willi");
    cut.setAttributesDeep(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
  }

  @Test
  public void testSetAttributesDeepMultipleAttribute() throws ODataJPAProcessException {
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("country", "DEU");
    cut.setAttributesDeep(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
    assertEquals("DEU", partner.getCountry());
  }

  @Test
  public void testSetAttributeDeepIfAttributeNull() throws ODataJPAProcessException {
    partner.setType("2");
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("type", null);
    cut.setAttributesDeep(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
    assertNull(partner.getType());
  }

  @Test
  public void testDoNotSetAttributeDeepIfNotInMap() throws ODataJPAProcessException {
    partner.setType("2");
    jpaAttributes.put("iD", "Willi");
    cut.setAttributesDeep(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
    assertEquals("2", partner.getType());
  }

  @Test
  public void testSetAttributesDeepShallIgnoreRequestEntities() throws ODataJPAProcessException {
    JPARequestEntity roles = mock(JPARequestEntity.class);
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("roles", roles);
    cut.setAttributesDeep(jpaAttributes, partner, org);
  }

  @Test
  public void testSetAttributesDeepOneLevelViaGetter() throws ODataJPAProcessException {
    Map<String, Object> embeddedAttributes = new HashMap<>();
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("address", embeddedAttributes);
    embeddedAttributes.put("cityName", "Test Town");
    cut.setAttributesDeep(jpaAttributes, partner, org);
    assertEquals("Willi", partner.getID());
    assertNotNull(partner.getAddress());
    assertEquals("Test Town", partner.getAddress().getCityName());
  }

  @Test
  public void testSetAttributesDeepOneLevelViaGetterWithWrongRequestData() throws Throwable {
    Map<String, Object> embeddedAttributes = new HashMap<>();
    Map<String, Object> innerEmbeddedAttributes = new HashMap<>();
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("administrativeInformation", embeddedAttributes);
    embeddedAttributes.put("updated", innerEmbeddedAttributes);
    innerEmbeddedAttributes.put("by", null);
    try {
      cut.setAttributesDeep(jpaAttributes, partner, org);
    } catch (ODataJPAInvocationTargetException e) {
      assertEquals("Organization/AdministrativeInformation/Updated/By", e.getPath());
      assertEquals(NullPointerException.class, e.getCause().getClass());
    }
  }

  @Test
  public void testDoNotSetAttributesDeepOneLevelIfNotProvided() throws ODataJPAProcessException {
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("address", null);
    cut.setAttributesDeep(jpaAttributes, partner, org);

    assertEquals("Willi", partner.getID());
    assertNull(partner.getAddress());
  }

  @Test
  public void testSetAttributesDeepOneLevelIfNull() throws ODataJPAProcessException {
    final PostalAddressData address = new PostalAddressData();
    address.setCityName("Test City");

    partner.setAddress(address);
    jpaAttributes.put("iD", "Willi");
    cut.setAttributesDeep(jpaAttributes, partner, org);

    assertEquals("Willi", partner.getID());
    assertNotNull(partner.getAddress());
    assertEquals("Test City", partner.getAddress().getCityName());
  }

  @Test
  public void testSetAttributesDeepOneLevelViaSetter() throws ODataJPAProcessException {
    Map<String, Object> embeddedAttributes = new HashMap<>();
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("communicationData", embeddedAttributes);
    embeddedAttributes.put("email", "Test@Town");
    cut.setAttributesDeep(jpaAttributes, partner, org);

    assertEquals("Willi", partner.getID());
    assertNotNull(partner.getCommunicationData());
    assertEquals("Test@Town", partner.getCommunicationData().getEmail());
  }

  @Test
  public void testSetAttributesDeepTwoLevel() throws ODataJPAProcessException {
    Map<String, Object> embeddedAttributes = new HashMap<>();
    Map<String, Object> innerEmbeddedAttributes = new HashMap<>();
    jpaAttributes.put("iD", "Willi");
    jpaAttributes.put("administrativeInformation", embeddedAttributes);
    embeddedAttributes.put("updated", innerEmbeddedAttributes);
    innerEmbeddedAttributes.put("by", "Hugo");
    cut.setAttributesDeep(jpaAttributes, partner, org);

    assertEquals("Willi", partner.getID());
    assertNotNull(partner.getAdministrativeInformation());
    assertNotNull(partner.getAdministrativeInformation().getUpdated());
    assertEquals("Hugo", partner.getAdministrativeInformation().getUpdated().getBy());
  }

  @Test
  public void testCreatePrimaryKeyOneStringKeyField() throws ODataJPAProcessException, ODataJPAModelException {
    final JPAEntityType et = createSingleKeyEntityType();

    when(et.getKeyType()).thenAnswer((Answer<Class<?>>) invocation -> String.class);

    jpaAttributes.put("iD", "Willi");
    String act = (String) cut.createPrimaryKey(et, jpaAttributes, org);
    assertEquals("Willi", act);
  }

  @Test
  public void testCreatePrimaryKeyOneIntegerKeyField() throws ODataJPAProcessException, ODataJPAModelException {
    final JPAEntityType et = createSingleKeyEntityType();

    when(et.getKeyType()).thenAnswer((Answer<Class<?>>) invocation -> Integer.class);

    jpaAttributes.put("iD", 10);
    Integer act = (Integer) cut.createPrimaryKey(et, jpaAttributes, org);
    assertEquals(Integer.valueOf(10), act);
  }

  @Test
  public void testCreatePrimaryKeyOneBigIntegerKeyField() throws ODataJPAProcessException, ODataJPAModelException {
    final JPAEntityType et = createSingleKeyEntityType();

    when(et.getKeyType()).thenAnswer((Answer<Class<?>>) invocation -> BigInteger.class);

    jpaAttributes.put("iD", new BigInteger("10"));
    BigInteger act = (BigInteger) cut.createPrimaryKey(et, jpaAttributes, org);
    assertEquals(new BigInteger("10"), act);
  }

  @Test
  public void testCreatePrimaryKeyMultipleField() throws ODataJPAProcessException {
    final JPAEntityType et = mock(JPAEntityType.class);

    when(et.getKeyType()).thenAnswer((Answer<Class<?>>) invocation -> AdministrativeDivisionKey.class);

    jpaAttributes.put("codePublisher", "Test");
    jpaAttributes.put("codeID", "10");
    jpaAttributes.put("divisionCode", "10.1");
    AdministrativeDivisionKey act = (AdministrativeDivisionKey) cut.createPrimaryKey(et, jpaAttributes, org);
    assertEquals("Test", act.getCodePublisher());
    assertEquals("10", act.getCodeID());
    assertEquals("10.1", act.getDivisionCode());
  }

  @Test
  public void testDeepLinkComplexNotExist() throws ODataJPAProcessorException, ODataJPAModelException {
    final Organization source = new Organization("100");
    final Person target = new Person();
    target.setID("A");
    final JPAAssociationPath path = helper.getJPAAssociationPath("Organizations",
        "AdministrativeInformation/Updated/User");

    cut.linkEntities(source, target, path);

    assertNotNull(source.getAdministrativeInformation());
    assertNotNull(source.getAdministrativeInformation().getUpdated());
    assertEquals(target, source.getAdministrativeInformation().getUpdated().getUser());
  }

  @Test
  public void testDirectLink() throws ODataJPAProcessorException, ODataJPAModelException {
    final Organization source = new Organization("100");
    final BusinessPartnerRole target = new BusinessPartnerRole();
    target.setBusinessPartnerID("100");
    target.setRoleCategory("A");
    final JPAAssociationPath path = helper.getJPAAssociationPath("Organizations",
        "Roles");

    cut.linkEntities(source, target, path);

    assertNotNull(source.getRoles());
    assertNotNull(source.getRoles().toArray()[0]);
    assertEquals(target, source.getRoles().toArray()[0]);
  }

  @Test
  public void testSetForeignKeyOneKey() throws ODataJPAModelException, ODataJPAProcessorException {
    final Organization source = new Organization("100");
    final BusinessPartnerRole target = new BusinessPartnerRole();
    target.setRoleCategory("A");
    final JPAAssociationPath path = helper.getJPAAssociationPath("Organizations",
        "Roles");

    cut.setForeignKey(source, target, path);
    assertEquals("100", target.getBusinessPartnerID());
  }

  @Test
  public void testSetForeignKeyThrowsExceptionOnMissingGetter() throws ODataJPAModelException {
    final OrganizationWithoutGetter source = new OrganizationWithoutGetter("100");
    final BusinessPartnerRole target = new BusinessPartnerRole();
    target.setRoleCategory("A");
    final JPAAssociationPath path = helper.getJPAAssociationPath("Organizations", "Roles");

    assertThrows(ODataJPAProcessorException.class, () -> cut.setForeignKey(source, target, path));
  }

  @Test
  public void testSetForeignKeyThrowsExceptionOnMissingSetter() throws ODataJPAModelException {
    final Organization source = new Organization("100");
    final BusinessPartnerRoleWithoutSetter target = new BusinessPartnerRoleWithoutSetter();
    final JPAAssociationPath path = helper.getJPAAssociationPath("Organizations", "Roles");

    assertThrows(ODataJPAProcessorException.class, () -> cut.setForeignKey(source, target, path));
  }

  private JPAEntityType createSingleKeyEntityType() throws ODataJPAModelException {
    final List<JPAAttribute> keyAttributes = new ArrayList<>();
    final JPAAttribute keyAttribute = mock(JPAAttribute.class);
    final JPAEntityType et = mock(JPAEntityType.class);

    when(keyAttribute.getInternalName()).thenReturn("iD");
    keyAttributes.add(keyAttribute);
    when(et.getKey()).thenReturn(keyAttributes);
    return et;
  }
}