package nl.buildforce.sequoia.jpa.processor.core.modify;

import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAElement;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAPath;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPAProcessException;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPAProcessorException;
import nl.buildforce.sequoia.jpa.processor.core.query.EdmEntitySetInfo;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.AbcClassification;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.AccessRights;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.AccessRightsConverter;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.BusinessPartnerRole;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.DateConverter;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

import jakarta.persistence.AttributeConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJPACUDRequestHelper {
  private static final String COMMENT_INT_PROPERTY_NAME = "comment";
  private static final String COMMENT_EXT_PROPERTY_NAME = "Comment";
  private static final String INHOUSE_EXT_PROPERTY_NAME = "InhouseAddress";
  private final String nameSpace = "testns";
  private JPAConversionHelper cut;
  private List<UriResource> uriResourceParts;
  private ODataRequest request;
  private List<String> headers;

  @BeforeEach
  public void setUp() {
    request = mock(ODataRequest.class);
    headers = new ArrayList<>(1);
    uriResourceParts = new ArrayList<>();
    cut = new JPAConversionHelper();
  }

  @Test
  public void testConvertEmptyInputStream() {

    final EdmEntitySetInfo etsInfo = mock(EdmEntitySetInfo.class);
    final EdmEntitySet ets = mock(EdmEntitySet.class);
    final UriResourceEntitySet uriEs = mock(UriResourceEntitySet.class);

    final InputStream is = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
    uriResourceParts.add(uriEs);

    when(uriEs.getEntitySet()).thenReturn(ets);
    when(uriEs.getKind()).thenReturn(UriResourceKind.entitySet);
    when(request.getBody()).thenReturn(is);
    when(etsInfo.getEdmEntitySet()).thenReturn(ets);
    when(etsInfo.getTargetEdmEntitySet()).thenReturn(ets);

    try {
      cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    } catch (ODataJPAProcessorException e) {
      assertEquals(HttpStatusCode.BAD_REQUEST.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertInputStreamComplexCollectionProperty() throws
          ODataJPAProcessorException, EdmPrimitiveTypeException {

    final EdmEntitySet edmEntitySet = mock(EdmEntitySet.class);
    final EdmEntityType edmEntityType = mock(EdmEntityType.class);
    final EdmProperty edmPropertyInhouse = mock(EdmProperty.class);
    final EdmComplexType edmTypeInhouse = mock(EdmComplexType.class);
    final UriResourceEntitySet uriEs = mock(UriResourceEntitySet.class);
    final UriResourceComplexProperty uriProperty = mock(UriResourceComplexProperty.class);
    FullQualifiedName fqn = new FullQualifiedName(nameSpace, "Person");
    FullQualifiedName fqnString = new FullQualifiedName(nameSpace, "Person");

    List<String> propertyNames = new ArrayList<>();
    propertyNames.add(INHOUSE_EXT_PROPERTY_NAME);

    uriResourceParts.add(uriEs);
    uriResourceParts.add(uriProperty);

    when(uriEs.getEntitySet()).thenReturn(edmEntitySet);
    when(uriEs.getKind()).thenReturn(UriResourceKind.entitySet);

    when(uriProperty.getProperty()).thenReturn(edmPropertyInhouse);
    when(uriProperty.getKind()).thenReturn(UriResourceKind.complexProperty);

    when(edmTypeInhouse.getFullQualifiedName()).thenReturn(fqnString);
    when(edmTypeInhouse.getKind()).thenReturn(EdmTypeKind.COMPLEX);
    when(edmTypeInhouse.getName()).thenReturn(INHOUSE_EXT_PROPERTY_NAME);
    when(edmTypeInhouse.getPropertyNames()).thenReturn(Arrays.asList("RoomNumber", "Floor", "TaskID", "Building"));
    EdmProperty edmProperty = createPropertyMock("RoomNumber", EdmPrimitiveTypeKind.Int32, Integer.class, 25);
    when(edmTypeInhouse.getProperty("RoomNumber")).thenReturn(edmProperty);
    edmProperty = createPropertyMock("Floor", EdmPrimitiveTypeKind.Int16, Short.class, 2);
    when(edmTypeInhouse.getProperty("Floor")).thenReturn(edmProperty);
    edmProperty = createPropertyMock("TaskID", EdmPrimitiveTypeKind.String, String.class, "DEV");
    when(edmTypeInhouse.getProperty("TaskID")).thenReturn(edmProperty);
    edmProperty = createPropertyMock("Building", EdmPrimitiveTypeKind.String, String.class, "2");
    when(edmTypeInhouse.getProperty("Building")).thenReturn(edmProperty);

    when(edmEntitySet.getEntityType()).thenReturn(edmEntityType);
    when(edmEntityType.getFullQualifiedName()).thenReturn(fqn);
    when(edmEntityType.getPropertyNames()).thenReturn(propertyNames);
    when(edmEntityType.getProperty(INHOUSE_EXT_PROPERTY_NAME)).thenReturn(edmPropertyInhouse);
    when(edmPropertyInhouse.getName()).thenReturn(INHOUSE_EXT_PROPERTY_NAME);
    when(edmPropertyInhouse.getType()).thenReturn(edmTypeInhouse);
    when(edmPropertyInhouse.isCollection()).thenReturn(true);
    InputStream is = new ByteArrayInputStream(
        "{\"value\": [{\"RoomNumber\": 25, \"Floor\": 2,\"TaskID\": \"DEV\", \"Building\": \"2\" }]}".getBytes(
                StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals(ValueType.COLLECTION_COMPLEX, act.getProperty(INHOUSE_EXT_PROPERTY_NAME).getValueType());
    final List<ComplexValue> actValue = (List<ComplexValue>) act.getProperty(INHOUSE_EXT_PROPERTY_NAME).getValue();
    assertEquals(1, actValue.size());
    final ComplexValue actInhouseMail = actValue.get(0);
    assertNotNull(actInhouseMail.getValue().get(0).getValue());
  }

  @Test
  public void testConvertInputStreamEntitySet() throws ODataJPAProcessorException,
      EdmPrimitiveTypeException {

    prepareEntitySet();
    InputStream is = new ByteArrayInputStream("{\"ID\" : \"35\"}".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals("35", act.getProperty("ID").getValue());
  }

  @Test
  public void testConvertInputStreamEntitySetWithAnnotationV400() throws
          ODataJPAProcessorException, EdmPrimitiveTypeException {

    headers.add("4.00");
    prepareEntitySet();
    InputStream is = new ByteArrayInputStream(
            ("{ \"@odata.context\": \"$metadata#" + nameSpace + ".Organisation\", \"@odata.type\": \"#" + nameSpace + ".Organisation\", \"ID\" : \"35\"}")
                    .getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);
    when(request.getHeaders(HttpHeader.ODATA_VERSION)).thenReturn(headers);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals("35", act.getProperty("ID").getValue());
  }

  @Test
  public void testConvertInputStreamEntitySetWithAnnotationV401() throws ODataJPAProcessorException, EdmPrimitiveTypeException {

    headers.add("4.01");
    prepareEntitySet();
    InputStream is = new ByteArrayInputStream(
        ("{\"@context\": \"$metadata#" + nameSpace +".Organisation\", \"@type\": \"#" + nameSpace + ".Organisation\", \"ID\" : \"35\"}")
            .getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);
    when(request.getHeaders(HttpHeader.ODATA_VERSION)).thenReturn(headers);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals("35", act.getProperty("ID").getValue());
  }

  @Test
  public void testConvertInputStreamEntitySetThrowsExceptionOnAnnotationMismatch() throws
          EdmPrimitiveTypeException {

    prepareEntitySet();
    InputStream is = new ByteArrayInputStream(
        "{\"@context\": \"$metadata#nl.buildforce.sequoia.jpa.Organization\", \"@type\": \"#nl.buildforce.sequoia.jpa.Organization\", \"ID\" : \"35\"}"
            .getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);

    assertThrows(ODataJPAProcessorException.class, () -> cut.convertInputStream(OData.newInstance(), request,
        ContentType.APPLICATION_JSON, uriResourceParts));
  }

  // @SuppressWarnings("unchecked")
  @Test
  public void testConvertInputStreamPrimitiveCollectionProperty() throws ODataJPAProcessorException, EdmPrimitiveTypeException {
    final EdmEntitySet edmEntitySet = mock(EdmEntitySet.class);
    final EdmEntityType edmEntityType = mock(EdmEntityType.class);
    final EdmProperty edmPropertyName = mock(EdmProperty.class);
    final EdmPrimitiveType edmTypeName = mock(EdmPrimitiveType.class);
    final UriResourceEntitySet uriEs = mock(UriResourceEntitySet.class);
    final UriResourceProperty uriProperty = mock(UriResourceProperty.class);
    FullQualifiedName fqn = new FullQualifiedName(nameSpace, "Organisation");
    FullQualifiedName fqnString = new FullQualifiedName(nameSpace, "Organisation");

    List<String> propertyNames = new ArrayList<>();
    propertyNames.add(COMMENT_EXT_PROPERTY_NAME);

    uriResourceParts.add(uriEs);
    uriResourceParts.add(uriProperty);

    when(uriEs.getEntitySet()).thenReturn(edmEntitySet);
    when(uriEs.getKind()).thenReturn(UriResourceKind.entitySet);

    when(uriProperty.getProperty()).thenReturn(edmPropertyName);
    when(uriProperty.getKind()).thenReturn(UriResourceKind.primitiveProperty);

    when(edmTypeName.getFullQualifiedName()).thenReturn(fqnString);
    when(edmTypeName.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);
    when(edmTypeName.getName()).thenReturn("String");
    when(edmTypeName.valueOfString(ArgumentMatchers.eq("YAC"), ArgumentMatchers.anyBoolean(), ArgumentMatchers
        .anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean(),
        (Class<String>) ArgumentMatchers.any())).thenReturn("YAC");
    when(edmTypeName.valueOfString(ArgumentMatchers.eq("WTN"), ArgumentMatchers.anyBoolean(), ArgumentMatchers
        .anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean(),
        (Class<String>) ArgumentMatchers.any())).thenReturn("WTN");

    when(edmEntitySet.getEntityType()).thenReturn(edmEntityType);
    when(edmEntityType.getFullQualifiedName()).thenReturn(fqn);
    when(edmEntityType.getPropertyNames()).thenReturn(propertyNames);
    when(edmEntityType.getProperty(COMMENT_EXT_PROPERTY_NAME)).thenReturn(edmPropertyName);
    when(edmPropertyName.getName()).thenReturn(COMMENT_EXT_PROPERTY_NAME);
    when(edmPropertyName.getType()).thenReturn(edmTypeName);
    when(edmPropertyName.isCollection()).thenReturn(true);
    InputStream is = new ByteArrayInputStream("{ \"value\": [\"YAC\",\"WTN\"] }".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals(ValueType.COLLECTION_PRIMITIVE, act.getProperty(COMMENT_EXT_PROPERTY_NAME).getValueType());
    @SuppressWarnings("unchecked")
    final List<String> actValue = (List<String>) act.getProperty(COMMENT_EXT_PROPERTY_NAME).getValue();
    assertEquals(2, actValue.size());
    assertEquals("YAC", actValue.get(0));
    assertEquals("WTN", actValue.get(1));
  }

  @Test
  public void testConvertInputStreamPrimitiveSimpleProperty() throws ODataJPAProcessorException, EdmPrimitiveTypeException {

    final ODataRequest request = preparePrimitiveSimpleProperty();
    InputStream is = new ByteArrayInputStream("{\"value\" : \"Willi\"}".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals("Willi", act.getProperty("Name2").getValue());
  }

  @Test
  public void testConvertInputStreamWithAnnotationV400PrimitiveSimpleProperty() throws ODataJPAProcessorException, EdmPrimitiveTypeException {

    final ODataRequest request = preparePrimitiveSimpleProperty();
    InputStream is = new ByteArrayInputStream(
        "{ \"@jpa.odata.context\": \"$metadata#Organisations\", \"value\" : \"Willi\"}".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals("Willi", act.getProperty("Name2").getValue());
  }

  @Test
  public void testConvertInputStreamWithAnnotationV401PrimitiveSimpleProperty() throws ODataJPAProcessorException, EdmPrimitiveTypeException {

    final ODataRequest request = preparePrimitiveSimpleProperty();
    InputStream is = new ByteArrayInputStream(
        "{ \"@context\": \"$metadata#Organisations\", \"value\" : \"Willi\"}".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);

    Entity act = cut.convertInputStream(OData.newInstance(), request, ContentType.APPLICATION_JSON, uriResourceParts);
    assertEquals("Willi", act.getProperty("Name2").getValue());
  }

  @Test
  public void testConvertPropertiesConvertException() throws ODataJPAModelException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);

    when(propertyID.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyID.getName()).thenReturn("iD");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);
    when(st.getPath(ArgumentMatchers.anyString())).thenThrow(new ODataJPAModelException(new NullPointerException()));
    try {
      cut.convertProperties(OData.newInstance(), st, odataProperties);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertPropertiesEmptyComplexCollectionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {

    final List<Property> odataProperties = new ArrayList<>();
    final List<ComplexValue> odataComment = new ArrayList<>();
    final JPAStructuredType st = createMetadataForSimpleProperty("Address", "address");
    final JPAStructuredType nb = createMetadataForSimpleProperty("Number", "number");
    final JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(attributeAddress.getStructuredType()).thenReturn(nb);
    when(st.getAttribute("address")).thenReturn(attributeAddress);

    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COLLECTION_COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("address"));
    assertEquals(0, ((List<Map<String, Object>>) act.get("address")).size());
  }

  @Test
  public void testConvertPropertiesEmptyList() throws ODataJPAProcessException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(0, act.size());
  }

  @Test
  public void testConvertPropertiesEmptySimpleCollectionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {

    final List<Property> odataProperties = new ArrayList<>();
    final List<String> odataComment = new ArrayList<>();

    final JPAStructuredType st = createMetadataForSimpleProperty(COMMENT_EXT_PROPERTY_NAME, COMMENT_INT_PROPERTY_NAME);

    Property propertyComment = mock(Property.class);
    when(propertyComment.getValueType()).thenReturn(ValueType.COLLECTION_PRIMITIVE);
    when(propertyComment.getName()).thenReturn(COMMENT_EXT_PROPERTY_NAME);
    when(propertyComment.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyComment);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get(COMMENT_INT_PROPERTY_NAME));
    assertTrue(((List<?>) act.get(COMMENT_INT_PROPERTY_NAME)).isEmpty());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertPropertiesOneComplexCollectionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {

    final List<Property> odataProperties = new ArrayList<>();
    final List<ComplexValue> odataComment = new ArrayList<>();
    final List<Property> addressProperties = new ArrayList<>();
    final JPAStructuredType st = createMetadataForSimpleProperty("Address", "address");
    final JPAStructuredType nb = createMetadataForSimpleProperty("Number", "number");
    final JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(attributeAddress.getStructuredType()).thenReturn(nb);
    when(st.getAttribute("address")).thenReturn(attributeAddress);
    final ComplexValue cv1 = mock(ComplexValue.class);

    final Property propertyNumber = mock(Property.class);
    when(propertyNumber.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyNumber.getName()).thenReturn("Number");
    when(propertyNumber.getValue()).thenReturn(32);
    addressProperties.add(propertyNumber);
    when(cv1.getValue()).thenReturn(addressProperties);

    odataComment.add(cv1);
    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COLLECTION_COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("address"));
    assertEquals(1, ((List<Map<String, Object>>) act.get("address")).size());
    Map<String, Object> actAddr = (Map<String, Object>) ((List<?>) act.get("address")).get(0);
    assertEquals(32, actAddr.get("number"));
  }

  @Test
  public void testConvertPropertiesOneComplexProperty() throws ODataJPAProcessException, ODataJPAModelException {

    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath pathID = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("ID")).thenReturn(pathID);
    when(pathID.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("iD");

    Answer<?> a = ((Answer<Object>) invocation -> String.class);
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(edmProperty.getMaxLength()).thenReturn(100);
    when(propertyID.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyID.getName()).thenReturn("ID");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);

    ComplexValue cv = new ComplexValue();
    List<JPAElement> addressPathElements = new ArrayList<>();
    JPAElement addressElement = mock(JPAElement.class);
    addressPathElements.add(addressElement);
    when(addressElement.getInternalName()).thenReturn("address");

    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(cv);
    odataProperties.add(propertyAddress);
    JPAPath pathAddress = mock(JPAPath.class);
    when(st.getPath("Address")).thenReturn(pathAddress);
    when(pathAddress.getPath()).thenReturn(addressPathElements);
    JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(st.getAttribute("address")).thenReturn(attributeAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(2, act.size());
    assertTrue(act.get("address") instanceof Map<?, ?>);
  }

  @Test
  public void testConvertPropertiesOneEnumPropertyWithConverter() throws ODataJPAProcessException,
      ODataJPAModelException {

    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath path = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("AccessRights")).thenReturn(path);
    when(path.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("accessRights");

    Answer<?> a = ((Answer<Object>) invocation -> AccessRights.class);
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(attribute.getConverter()).thenAnswer((Answer<AttributeConverter<?, ?>>) invocation -> new AccessRightsConverter());
    when(edmProperty.getMaxLength()).thenReturn(100);
    when(propertyID.getValueType()).thenReturn(ValueType.ENUM);
    when(propertyID.getName()).thenReturn("AccessRights");
    when(propertyID.getValue()).thenReturn((short) 8);
    odataProperties.add(propertyID);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(1, act.size());
    AccessRights[] actProperty = (AccessRights[]) act.get("accessRights");
    assertArrayEquals(new Object[] { AccessRights.Delete }, actProperty);
  }

  @Test
  public void testConvertPropertiesOneEnumPropertyWithoutConverter() throws ODataJPAProcessException,
      ODataJPAModelException {

    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath path = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("ABCClass")).thenReturn(path);
    when(path.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("aBCClass");

    Answer<?> a = ((Answer<Object>) invocation -> AbcClassification.class);
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(attribute.isEnum()).thenReturn(true);
    when(propertyID.getValueType()).thenReturn(ValueType.ENUM);
    when(propertyID.getName()).thenReturn("ABCClass");
    when(propertyID.getValue()).thenReturn(1);
    odataProperties.add(propertyID);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals(AbcClassification.B, act.get("aBCClass"));
  }

  @Test
  public void testConvertPropertiesOnePrimitiveProperty() throws ODataJPAProcessException, ODataJPAModelException {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);
    JPAAttribute attribute = mock(JPAAttribute.class);
    JPAPath path = mock(JPAPath.class);
    CsdlProperty edmProperty = mock(CsdlProperty.class);

    when(st.getPath("ID")).thenReturn(path);
    when(path.getLeaf()).thenReturn(attribute);
    when(attribute.getInternalName()).thenReturn("iD");

    Answer<?> a = ((Answer<Object>) invocation -> String.class);
    when(attribute.getType()).thenAnswer(a);
    when(attribute.getProperty()).thenReturn(edmProperty);
    when(edmProperty.getMaxLength()).thenReturn(100);
    when(propertyID.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyID.getName()).thenReturn("ID");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);

    assertNotNull(act);
    assertEquals(1, act.size());
    assertEquals("35", act.get("iD"));
  }

  @Test
  public void testConvertPropertiesOneSimpleCollectionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {

    final List<Property> odataProperties = new ArrayList<>();
    final List<String> odataComment = new ArrayList<>();

    final JPAStructuredType st = createMetadataForSimpleProperty(COMMENT_EXT_PROPERTY_NAME, COMMENT_INT_PROPERTY_NAME);

    odataComment.add("First Test");
    Property propertyComment = mock(Property.class);
    when(propertyComment.getValueType()).thenReturn(ValueType.COLLECTION_PRIMITIVE);
    when(propertyComment.getName()).thenReturn(COMMENT_EXT_PROPERTY_NAME);
    when(propertyComment.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyComment);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get(COMMENT_INT_PROPERTY_NAME));
    assertEquals(1, ((List<?>) act.get(COMMENT_INT_PROPERTY_NAME)).size());
    assertEquals("First Test", ((List<?>) act.get(COMMENT_INT_PROPERTY_NAME)).get(0));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testConvertPropertiesTwoComplexCollectionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {

    final List<Property> odataProperties = new ArrayList<>();
    final List<ComplexValue> odataComment = new ArrayList<>();
    final JPAStructuredType st = createMetadataForSimpleProperty("Address", "address");
    final JPAStructuredType nb = createMetadataForSimpleProperty("Number", "number");
    final JPAAttribute attributeAddress = mock(JPAAttribute.class);
    when(attributeAddress.getStructuredType()).thenReturn(nb);
    when(st.getAttribute("address")).thenReturn(attributeAddress);

    List<Property> addressProperties = new ArrayList<>();
    final ComplexValue cv1 = mock(ComplexValue.class);
    Property propertyNumber = mock(Property.class);
    when(propertyNumber.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyNumber.getName()).thenReturn("Number");
    when(propertyNumber.getValue()).thenReturn(32);
    addressProperties.add(propertyNumber);
    when(cv1.getValue()).thenReturn(addressProperties);

    addressProperties = new ArrayList<>();
    final ComplexValue cv2 = mock(ComplexValue.class);
    propertyNumber = mock(Property.class);
    when(propertyNumber.getValueType()).thenReturn(ValueType.PRIMITIVE);
    when(propertyNumber.getName()).thenReturn("Number");
    when(propertyNumber.getValue()).thenReturn(16);
    addressProperties.add(propertyNumber);
    when(cv2.getValue()).thenReturn(addressProperties);

    odataComment.add(cv1);
    odataComment.add(cv2);
    Property propertyAddress = mock(Property.class);
    when(propertyAddress.getValueType()).thenReturn(ValueType.COLLECTION_COMPLEX);
    when(propertyAddress.getName()).thenReturn("Address");
    when(propertyAddress.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyAddress);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get("address"));
    assertEquals(2, ((List<Map<String, Object>>) act.get("address")).size());
    Map<String, Object> actAddr1 = (Map<String, Object>) ((List<?>) act.get("address")).get(0);
    assertEquals(32, actAddr1.get("number"));

    Map<String, Object> actAddr2 = (Map<String, Object>) ((List<?>) act.get("address")).get(1);
    assertEquals(16, actAddr2.get("number"));
  }

  @Test
  public void testConvertPropertiesTwoSimpleCollectionProperty() throws ODataJPAProcessException,
      ODataJPAModelException {

    final List<Property> odataProperties = new ArrayList<>();
    final List<String> odataComment = new ArrayList<>();

    final JPAStructuredType st = createMetadataForSimpleProperty(COMMENT_EXT_PROPERTY_NAME, COMMENT_INT_PROPERTY_NAME);

    odataComment.add("First Test");
    odataComment.add("Second Test");
    Property propertyComment = mock(Property.class);
    when(propertyComment.getValueType()).thenReturn(ValueType.COLLECTION_PRIMITIVE);
    when(propertyComment.getName()).thenReturn(COMMENT_EXT_PROPERTY_NAME);
    when(propertyComment.getValue()).thenReturn(odataComment);
    odataProperties.add(propertyComment);

    Map<String, Object> act = cut.convertProperties(OData.newInstance(), st, odataProperties);
    assertNotNull(act.get(COMMENT_INT_PROPERTY_NAME));
    assertEquals(2, ((List<?>) act.get(COMMENT_INT_PROPERTY_NAME)).size());
    assertEquals("First Test", ((List<?>) act.get(COMMENT_INT_PROPERTY_NAME)).get(0));
    assertEquals("Second Test", ((List<?>) act.get(COMMENT_INT_PROPERTY_NAME)).get(1));
  }

  @Test
  public void testConvertPropertiesUnknownValueType() {
    List<Property> odataProperties = new ArrayList<>();
    JPAStructuredType st = mock(JPAStructuredType.class);
    Property propertyID = mock(Property.class);

    when(propertyID.getValueType()).thenReturn(ValueType.COLLECTION_ENTITY);
    when(propertyID.getName()).thenReturn("ID");
    when(propertyID.getValue()).thenReturn("35");
    odataProperties.add(propertyID);

    try {
      cut.convertProperties(OData.newInstance(), st, odataProperties);
    } catch (ODataJPAProcessException e) {
      assertEquals(HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Disabled
  @Test
  public void testDifferentInstanceWhenReadingDifferentInstance() throws ODataJPAProcessorException {

    Map<String, Object> exp = cut.buildGetterMap(new BusinessPartnerRole("100", "A"));
    Map<String, Object> act = cut.buildGetterMap(new BusinessPartnerRole("100", "A"));

    assertNotSame(exp, act);
  }

  @Test
  public void testInstanceNull() {

    try {
      cut.buildGetterMap(null);
    } catch (ODataJPAProcessorException e) {
      assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), e.getStatusCode());
      return;
    }
    fail();
  }

  @Test
  public void testInstanceWithGetter() throws ODataJPAProcessorException {
    BusinessPartnerRole role = new BusinessPartnerRole();
    role.setBusinessPartnerID("ID");

    Map<String, Object> act = cut.buildGetterMap(role);
    assertNotNull(act);
    assertEquals(5, act.size());
    assertEquals("ID", act.get("businessPartnerID"));
  }

  @Test
  public void testInstanceWithoutGetter() throws ODataJPAProcessorException {

    Map<String, Object> act = cut.buildGetterMap(new DateConverter());
    assertNotNull(act);
    assertEquals(1, act.size());
    assertNotNull(act.get("class"));
  }

  @Test
  public void testSameInstanceWhenReadingTwice() throws ODataJPAProcessorException {
    BusinessPartnerRole role = new BusinessPartnerRole();

    Map<String, Object> exp = cut.buildGetterMap(role);
    Map<String, Object> act = cut.buildGetterMap(role);

    assertSame(exp, act);
  }

  private JPAStructuredType createMetadataForSimpleProperty(final String externalName, final String internalName)
      throws ODataJPAModelException {
    final JPAStructuredType st = mock(JPAStructuredType.class);
    final JPAAttribute attribute = mock(JPAAttribute.class);
    final JPAPath pathID = mock(JPAPath.class);
    final List<JPAElement> pathElements = new ArrayList<>();
    pathElements.add(attribute);
    when(st.getPath(externalName)).thenReturn(pathID);
    when(pathID.getLeaf()).thenReturn(attribute);
    when(pathID.getPath()).thenReturn(pathElements);
    when(attribute.getInternalName()).thenReturn(internalName);
    return st;
  }

  private EdmProperty createPropertyMock(final String propertyName, final EdmPrimitiveTypeKind propertyType,
      final Class<?> defaultJavaType, final Object value) throws EdmPrimitiveTypeException {

    final EdmProperty edmProperty = mock(EdmProperty.class);
    final EdmPrimitiveType edmType = mock(EdmPrimitiveType.class);
    when(edmType.getFullQualifiedName()).thenReturn(propertyType.getFullQualifiedName());
    when(edmType.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);
    when(edmType.getName()).thenReturn(propertyType.getFullQualifiedName().getName());
    when(edmType.getDefaultType()).thenAnswer((Answer<Class<?>>) invocation -> defaultJavaType);
    when(edmType.valueOfString(value.toString(), true, 0, 0, 0, true, defaultJavaType)).thenAnswer(
            (Answer<Object>) invocation -> value);
    when(edmProperty.getName()).thenReturn(propertyName);
    when(edmProperty.getType()).thenReturn(edmType);
    when(edmProperty.isUnicode()).thenReturn(true);
    when(edmProperty.isPrimitive()).thenReturn(true);
    when(edmProperty.isCollection()).thenReturn(false);
    when(edmProperty.isNullable()).thenReturn(true);
    return edmProperty;
  }

  @SuppressWarnings("unchecked")
  private void prepareEntitySet() throws EdmPrimitiveTypeException {
    final EdmEntitySet edmEntitySet = mock(EdmEntitySet.class);
    final EdmEntityType edmEntityType = mock(EdmEntityType.class);
    final EdmProperty edmPropertyId = mock(EdmProperty.class);
    final EdmPrimitiveType edmTypeId = mock(EdmPrimitiveType.class);
    final UriResourceEntitySet uriEs = mock(UriResourceEntitySet.class);

    FullQualifiedName fqn = new FullQualifiedName(nameSpace, "Organisation");
    FullQualifiedName fqnString = new FullQualifiedName(nameSpace, "Organisation");

    List<String> propertyNames = new ArrayList<>();
    propertyNames.add("ID");

    uriResourceParts.add(uriEs);

    when(uriEs.getEntitySet()).thenReturn(edmEntitySet);
    when(uriEs.getKind()).thenReturn(UriResourceKind.entitySet);
    when(edmTypeId.getFullQualifiedName()).thenReturn(fqnString);
    when(edmTypeId.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);
    when(edmTypeId.getName()).thenReturn("String");
    when(edmTypeId.valueOfString(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean(),
        (Class<String>) ArgumentMatchers.any())).thenReturn("35");

    when(edmEntitySet.getEntityType()).thenReturn(edmEntityType);
    when(edmEntityType.getFullQualifiedName()).thenReturn(fqn);
    when(edmEntityType.getPropertyNames()).thenReturn(propertyNames);
    when(edmEntityType.getProperty("ID")).thenReturn(edmPropertyId);
    when(edmPropertyId.getName()).thenReturn("ID");
    when(edmPropertyId.getType()).thenReturn(edmTypeId);
  }

  @SuppressWarnings("unchecked")
  private ODataRequest preparePrimitiveSimpleProperty() throws EdmPrimitiveTypeException {
    final ODataRequest request = mock(ODataRequest.class);

    final EdmEntitySet edmEntitySet = mock(EdmEntitySet.class);
    final EdmEntityType edmEntityType = mock(EdmEntityType.class);
    final EdmProperty edmPropertyName = mock(EdmProperty.class);
    final EdmPrimitiveType edmTypeName = mock(EdmPrimitiveType.class);
    final UriResourceEntitySet uriEs = mock(UriResourceEntitySet.class);
    final UriResourceProperty uriProperty = mock(UriResourceProperty.class);
    FullQualifiedName fqn = new FullQualifiedName(nameSpace, "Organisation");
    FullQualifiedName fqnString = new FullQualifiedName(nameSpace, "Organisation");

    List<String> propertyNames = new ArrayList<>();
    propertyNames.add("Name2");

    uriResourceParts.add(uriEs);
    uriResourceParts.add(uriProperty);

    when(uriEs.getEntitySet()).thenReturn(edmEntitySet);
    when(uriEs.getKind()).thenReturn(UriResourceKind.entitySet);

    when(uriProperty.getProperty()).thenReturn(edmPropertyName);
    when(uriProperty.getKind()).thenReturn(UriResourceKind.primitiveProperty);

    when(edmTypeName.getFullQualifiedName()).thenReturn(fqnString);
    when(edmTypeName.getKind()).thenReturn(EdmTypeKind.PRIMITIVE);
    when(edmTypeName.getName()).thenReturn("String");
    when(edmTypeName.valueOfString(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(), ArgumentMatchers
        .anyInt(),
        ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyBoolean(),
        (Class<String>) ArgumentMatchers.any())).thenReturn("Willi");

    when(edmEntitySet.getEntityType()).thenReturn(edmEntityType);
    when(edmEntityType.getFullQualifiedName()).thenReturn(fqn);
    when(edmEntityType.getPropertyNames()).thenReturn(propertyNames);
    when(edmEntityType.getProperty("Name2")).thenReturn(edmPropertyName);
    when(edmPropertyName.getName()).thenReturn("Name2");
    when(edmPropertyName.getType()).thenReturn(edmTypeName);
    return request;
  }

}