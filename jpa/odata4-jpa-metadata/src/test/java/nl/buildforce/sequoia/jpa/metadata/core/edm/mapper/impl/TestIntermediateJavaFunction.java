package nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.impl;

import nl.buildforce.sequoia.jpa.metadata.core.edm.annotation.EdmFunction;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAParameter;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.extension.ODataFunction;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaEmConstructor;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaFunctions;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaOneFunction;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaPrivateConstructor;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.testobjects.ExampleJavaTwoParameterConstructor;
// import org.apache.olingo.commons.api.edm.geo.Geospatial.Dimension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class TestIntermediateJavaFunction extends TestMappingRoot {
  private TestHelper helper;

  @BeforeEach
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
  }

  @Test
  public void checkInternalNameEqualMethodName() throws ODataJPAModelException {
    IntermediateFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertEquals("sum", act.getInternalName());
  }

  @Test
  public void checkExternalNameEqualMethodName() throws ODataJPAModelException {
    IntermediateFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertEquals("Sum", act.getExternalName());
  }

  @Test
  public void checkReturnsConvertedPrimitiveReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertNotNull(act.getEdmItem());
    assertNotNull(act.getEdmItem().getReturnType());
    assertEquals("Edm.Int32", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsConvertedPrimitiveParameterTypes() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertNotNull(act.getEdmItem());
    assertNotNull(act.getEdmItem().getParameters());
    assertEquals(2, act.getEdmItem().getParameters().size());
    assertNotNull(act.getEdmItem().getParameter("A"));
    assertNotNull(act.getEdmItem().getParameter("B"));
    assertEquals("Edm.Int16", act.getEdmItem().getParameter("A").getType());
    assertEquals("Edm.Int32", act.getEdmItem().getParameter("B").getType());
  }

  @Test
  public void checkThrowsExceptionForNonPrimitiveParameter() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "errorNonPrimitiveParameter");

    assertThrows(ODataJPAModelException.class, act::getEdmItem);
  }

  @Test
  public void checkReturnsFalseForIsBound() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertNotNull(act.getEdmItem());
    assertFalse(act.getEdmItem().isBound());
  }

  @Test
  public void checkReturnsTrueForHasFunctionImport() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaOneFunction.class, "sum");

    assertTrue(act.hasImport());
  }

  @Test
  public void checkReturnsAnnotatedName() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");

    assertEquals("Add", act.getExternalName());
  }

  @Test
  public void checkIgnoresGivenIsBound() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");

    assertFalse(act.getEdmItem().isBound());
    assertFalse(act.isBound());
  }

  @Test
  public void checkIgnoresGivenHasFunctionImport() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");

    assertTrue(act.hasImport());
  }

  @Test
  public void checkReturnsEnumerationTypeAsParameter() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEnumerationType");

    assertEquals("nl.buildforce.sequoia.jpa.AccessRights", act.getEdmItem().getParameters().get(0).getTypeFQN()
        .getFullQualifiedNameAsString());
    JPAParameter param = act.getParameter("arg0");
    if (param == null)
      param = act.getParameter("rights");
    assertNotNull(param);
    assertEquals("nl.buildforce.sequoia.jpa.AccessRights", param.getTypeFQN().getFullQualifiedNameAsString());
  }

  @Test
  public void checkIgnoresParameterAsPartFromEdmFunction() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "div");

    assertNotNull(act.getEdmItem());
    assertEquals(2, act.getEdmItem().getParameters().size());
    assertNotNull(act.getEdmItem().getParameter("A"));
    assertNotNull(act.getEdmItem().getParameter("B"));
  }

  @Test
  public void checkThrowsExceptionIfAnnotatedReturnTypeNEDeclaredType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "errorReturnType");
    assertThrows(ODataJPAModelException.class, act::getEdmItem);
  }

  @Test
  public void checkReturnsFacetForNumbersOfReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "now");
    assertFalse(act.getEdmItem().getReturnType().isNullable());
    assertEquals(Integer.valueOf(9), act.getEdmItem().getReturnType().getPrecision());
    assertEquals(Integer.valueOf(3), act.getEdmItem().getReturnType().getScale());
  }

  @Test
  public void checkReturnsFacetForStringsAndGeoOfReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "determineLocation");
    // assertEquals(Integer.valueOf(60), act.getEdmItem().getReturnType().getMaxLength());
    // assertEquals(Dimension.GEOGRAPHY, act.getEdmItem().getReturnType().getSrid().getDimension());
    // assertEquals("4326", act.getEdmItem().getReturnType().getSrid().toString());
  }

  @Test
  public void checkReturnsIsCollectionIfDefinedReturnTypeIsSubclassOfCollection() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnCollection");

    assertTrue(act.getEdmItem().getReturnType().isCollection());
    assertEquals("Edm.String", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkThrowsExceptionIfCollectionAndReturnTypeEmpty() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class,
        "returnCollectionWithoutReturnType");
    assertThrows(ODataJPAModelException.class, act::getEdmItem);
  }

  @Test
  public void checkReturnsEmbeddableTypeAsReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEmbeddable");

    assertEquals("nl.buildforce.sequoia.jpa.ChangeInformation", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsEmbeddableCollectionTypeAsReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEmbeddableCollection");

    assertEquals("nl.buildforce.sequoia.jpa.ChangeInformation", act.getEdmItem().getReturnType().getType());
    assertTrue(act.getEdmItem().getReturnType().isCollection());
  }

  @Test
  public void checkReturnsEntityTypeAsReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEntity");
    assertEquals("nl.buildforce.sequoia.jpa.Person", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsEnumerationTypeAsReturnType() throws ODataJPAModelException {

    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEnumerationType");
    assertEquals("nl.buildforce.sequoia.jpa.AbcClassification", act.getEdmItem().getReturnType().getType());
  }

  @Test
  public void checkReturnsEnumerationCollectionTypeAsReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "returnEnumerationCollection");

    assertEquals("nl.buildforce.sequoia.jpa.AbcClassification", act.getEdmItem().getReturnType().getType());
    assertTrue(act.getEdmItem().getReturnType().isCollection());
  }

  @Test
  public void checkThrowsExceptionOnNotSupportedReturnType() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "wrongReturnType");
    assertThrows(ODataJPAModelException.class, act::getEdmItem);
  }

  @Test
  public void checkExceptConstructorWithoutParameter() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaFunctions.class, "sum");
    act.getEdmItem();
  }

  @Test
  public void checkExceptConstructorWithEntityManagerParameter() throws ODataJPAModelException {
    final IntermediateJavaFunction act = createFunction(ExampleJavaEmConstructor.class, "sum");
    act.getEdmItem();
  }

  @Test
  public void checkThrowsExceptionOnPrivateConstructor() {
    assertThrows(ODataJPAModelException.class, () -> createFunction(ExampleJavaPrivateConstructor.class, "sum"));
  }

  @Test
  public void checkThrowsExceptionOnNoConstructorAsSpecified() {
    assertThrows(ODataJPAModelException.class, () -> createFunction(ExampleJavaTwoParameterConstructor.class, "sum"));
  }

  private IntermediateJavaFunction createFunction(Class<? extends ODataFunction> clazz, String method)
      throws ODataJPAModelException {
    for (Method m : clazz.getMethods()) {
      EdmFunction functionDescription = m.getAnnotation(EdmFunction.class);
      if (functionDescription != null && method.equals(m.getName())) {
        return new IntermediateJavaFunction(new JPADefaultEdmNameBuilder(PUNIT_NAME), functionDescription, m,
            helper.schema);
      }
    }
    return null;
  }
}