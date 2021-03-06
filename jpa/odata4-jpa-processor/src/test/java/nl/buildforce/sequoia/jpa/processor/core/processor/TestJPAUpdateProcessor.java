package nl.buildforce.sequoia.jpa.processor.core.processor;

import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import nl.buildforce.sequoia.jpa.metadata.core.edm.mapper.exception.ODataJPAException;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAAbstractCUDRequestHandler;
import nl.buildforce.sequoia.jpa.processor.core.api.JPACUDRequestHandler;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataClaimProvider;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataClaimsProvider;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataGroupProvider;
import nl.buildforce.sequoia.jpa.processor.core.api.JPAODataGroupsProvider;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPAProcessException;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPAProcessorException;
import nl.buildforce.sequoia.jpa.processor.core.exception.ODataJPASerializerException;
import nl.buildforce.sequoia.jpa.processor.core.modify.JPAUpdateResult;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.AdministrativeDivision;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.AdministrativeDivisionKey;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.InhouseAddress;
import nl.buildforce.sequoia.jpa.processor.core.testmodel.Organization;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.ex.ODataException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.*;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestJPAUpdateProcessor extends TestJPAModifyProcessor {

  @Test
  public void testHockIsCalled() throws ODataJPAException, ODataJPAProcessException, ODataLibraryException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertTrue(spy.called);
  }

  @Test
  public void testHttpMethodProvided() throws ODataJPAException, ODataLibraryException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpMethod.PATCH, spy.method);
  }

  @Test
  public void testEntityTypeProvided() throws ODataJPAException, ODataLibraryException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertTrue(spy.et instanceof JPAEntityType);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testJPAAttributes() throws ODataJPAException, ODataLibraryException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    InputStream is = new ByteArrayInputStream("{\"ID\" : \"35\", \"Country\" : \"USA\"}".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);
    Map<String, Object> jpaAttributes = new HashMap<>();
    jpaAttributes.put("id", "35");
    jpaAttributes.put("country", "USA");
    when(convHelper.convertProperties(any(OData.class), any(JPAStructuredType.class), any(List.class)))
        .thenReturn(jpaAttributes);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(2, spy.jpaAttributes.size());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testProvideSimplePrimitivePutAsPatch() throws
          ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    final EdmProperty odataProperty = mock(EdmProperty.class);
    final UriResourceProperty uriProperty = mock(UriResourceProperty.class);
    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(odataProperty);
    when(odataProperty.getName()).thenReturn("StreetName");

    when(request.getMethod()).thenReturn(HttpMethod.PUT);

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    InputStream is = new ByteArrayInputStream("{ \"value\": \"New Road\"}".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);
    Map<String, Object> jpaAttributes = new HashMap<>();
    jpaAttributes.put("streetName", "New Road");
    when(convHelper.convertProperties(any(OData.class), any(JPAStructuredType.class), any(List.class)))
        .thenReturn(jpaAttributes);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(1, spy.jpaAttributes.size());
    assertEquals(HttpMethod.PATCH, spy.method);
  }

  @Disabled
  @Test
  public void testProvideSimpleComplexPutAsPatch() {
    // Not implemented yet
    fail();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testProvidePrimitiveCollectionPutAsPatch() throws
          ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    final EdmProperty odataProperty = mock(EdmProperty.class);
    final UriResourceProperty uriProperty = mock(UriResourceProperty.class);
    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(odataProperty);
    when(odataProperty.getName()).thenReturn("Comments");
    when(odataProperty.isCollection()).thenReturn(true);

    when(request.getMethod()).thenReturn(HttpMethod.PUT);

    final RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    final InputStream is = new ByteArrayInputStream("{ \"value\": \"[\"YAC\",\"WTN\"]\"}".getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);
    final Map<String, Object> jpaAttributes = new HashMap<>();
    final List<String> lines = new ArrayList<>(2);
    lines.add("YAC");
    lines.add("WTN");
    jpaAttributes.put("comment", lines);
    when(convHelper.convertProperties(any(OData.class), any(JPAStructuredType.class), any(List.class)))
        .thenReturn(jpaAttributes);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(1, spy.jpaAttributes.size());
    assertEquals(HttpMethod.PATCH, spy.method);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testProvideComplexCollectionPutAsPatch() throws
          ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();
    final EdmProperty odataProperty = mock(EdmProperty.class);
    final UriResourceProperty uriProperty = mock(UriResourceProperty.class);
    pathParts.add(uriProperty);
    when(uriProperty.getProperty()).thenReturn(odataProperty);
    when(odataProperty.getName()).thenReturn("InhouseAddress");
    when(odataProperty.isCollection()).thenReturn(true);

    when(request.getMethod()).thenReturn(HttpMethod.PUT);

    final RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    final InputStream is = new ByteArrayInputStream(
        "{ \"value\": \"[{\"RoomNumber\": 25,\"Floor\": 2,\"TaskID\": \"DEV\",\"Building\": \"2\"}]\"}"
            .getBytes(StandardCharsets.UTF_8));
    when(request.getBody()).thenReturn(is);
    final Map<String, Object> jpaAttributes = new HashMap<>();
    final List<InhouseAddress> lines = new ArrayList<>(2);
    lines.add(new InhouseAddress("DEV", "2"));
    jpaAttributes.put("inhouseAddress", lines);
    when(convHelper.convertProperties(any(OData.class), any(JPAStructuredType.class), any(List.class)))
        .thenReturn(jpaAttributes);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(1, spy.jpaAttributes.size());
    assertEquals(HttpMethod.PATCH, spy.method);
  }

  @Test
  public void testHeadersProvided() throws
          ODataException {
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareSimpleRequest();
    final Map<String, List<String>> headers = new HashMap<>();

    when(request.getAllHeaders()).thenReturn(headers);
    headers.put("If-Match", Collections.singletonList("2"));

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertNotNull(spy.headers);
    assertEquals(1, spy.headers.size());
    assertNotNull(spy.headers.get("If-Match"));
    assertEquals("2", spy.headers.get("If-Match").get(0));
  }

  @Test
  public void testClaimsProvided() throws
          ODataException {
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareSimpleRequest();

    final RequestHandleSpy spy = new RequestHandleSpy();
    final JPAODataClaimProvider provider = new JPAODataClaimsProvider();
    final Optional<JPAODataClaimProvider> claims = Optional.of(provider);
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);
    when(requestContext.getClaimsProvider()).thenReturn(claims);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertNotNull(spy.claims);
    assertTrue(spy.claims.isPresent());
    assertEquals(provider, spy.claims.get());
  }

  @Test
  public void testGroupsProvided() throws
          ODataException {
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareSimpleRequest();

    final RequestHandleSpy spy = new RequestHandleSpy();
    final JPAODataGroupsProvider provider = new JPAODataGroupsProvider();
    provider.addGroup("Person");
    // final List<String> groups = new ArrayList<>(Arrays.asList("Person"));
    final Optional<JPAODataGroupProvider> groups = Optional.of(provider);
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);
    when(requestContext.getGroupsProvider()).thenReturn(groups);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertNotNull(spy.groups);
    assertFalse(spy.groups.isEmpty());
    assertEquals("Person", spy.groups.get(0));
  }

  @Test
  public void testMinimalResponseUpdateStatusCode() throws
          ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(false, new Organization()));
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testMinimalResponseCreatedStatusCode() throws
          ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(true, new Organization()));
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.NO_CONTENT.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testMinimalResponseUpdatePreferHeader() throws
          ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(false, new Organization()));
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(PREFERENCE_APPLIED, response.getHeader(HttpHeader.PREFERENCE_APPLIED));
  }

  @Test
  public void testMinimalResponseCreatedPreferHeader() throws
          ODataException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    RequestHandleSpy spy = new RequestHandleSpy(new JPAUpdateResult(true, new Organization()));
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(PREFERENCE_APPLIED, response.getHeader(HttpHeader.PREFERENCE_APPLIED));
  }

  @Test
  public void testRepresentationResponseUpdatedStatusCode() throws
          ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(false,
            new Organization())));

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testRepresentationResponseCreatedStatusCode() throws
          ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(true,
            new Organization())));

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.CREATED.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testRepresentationResponseUpdatedErrorMissingEntity() throws ODataJPAException, SerializerException, ODataJPASerializerException, ODataJPAProcessorException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(false, null)));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataJPAProcessException | ODataLibraryException e) {
        assertTrue(true);
      return;
    }
    fail();
  }

  @Test
  public void testRepresentationResponseCreatedErrorMissingEntity() throws ODataJPAException, SerializerException, ODataJPASerializerException, ODataJPAProcessorException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(true, null)));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataJPAProcessException | ODataLibraryException e) {
        assertTrue(true);
      return;
    }
    fail();
  }

  @Test
  public void testRepresentationResponseUpdatedWithKey() throws
          ODataException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(new JPAUpdateResult(false,
            new Organization())));

    final Map<String, Object> keys = new HashMap<>();
    keys.put("iD", "35");
    when(convHelper.convertUriKeys(any(), any(), any())).thenReturn(keys);
    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);

    assertEquals(HttpStatusCode.OK.getStatusCode(), response.getStatusCode());
  }

  @Test
  public void testCallsValidateChangesOnSuccessfulProcessing() throws ODataJPAException, ODataLibraryException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    assertEquals(1, spy.noValidateCalls);
  }

  @Test
  public void testDoesNotCallsValidateChangesOnForeignTransaction() throws ODataJPAException, ODataJPAProcessException, ODataLibraryException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);
    when(factory.hasActiveTransaction()).thenReturn(Boolean.TRUE);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    assertEquals(0, spy.noValidateCalls);
  }

  @Test
  public void testDoesNotCallsValidateChangesOnError() throws ODataJPAException, SerializerException, ODataJPAProcessException {
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareSimpleRequest();
    when(request.getMethod()).thenReturn(HttpMethod.PATCH);
    final JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(requestContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).updateEntity(any(JPARequestEntity.class), any(EntityManager.class),
            any(HttpMethod.class));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataApplicationException | ODataLibraryException e) {
      verify(handler, never()).validateChanges(em);
      return;
    }
    fail();
  }

  @Test
  public void testDoesRollbackIfValidateRaisesError() throws ODataJPAException, ODataJPAProcessException, SerializerException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(requestContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).validateChanges(em);

    assertThrows(ODataApplicationException.class,
        () -> processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON));
    verify(transaction, never()).commit();
    verify(transaction, times(1)).rollback();
  }

  @Test
  public void testDoesRollbackIfUpdateRaisesError() throws ODataJPAException, SerializerException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(requestContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new ODataJPAProcessorException(ODataJPAProcessorException.MessageKeys.NOT_SUPPORTED_DELETE,
        HttpStatusCode.BAD_REQUEST)).when(handler).updateEntity(any(), any(), any());

    assertThrows(ODataApplicationException.class,
        () -> processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON));
    verify(transaction, never()).commit();
    verify(transaction, times(1)).rollback();
  }

  @Test
  public void testDoesRollbackIfUpdateRaisesArbitraryError() throws ODataJPAException, SerializerException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(requestContext.getCUDRequestHandler()).thenReturn(handler);

    doThrow(new RuntimeException("Test")).when(handler).updateEntity(any(), any(), any());

    assertThrows(ODataException.class,
        () -> processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON));
    verify(transaction, never()).commit();
    verify(transaction, times(1)).rollback();
  }

  @Test
  public void testDoesRollbackOnEmptyResponse() throws ODataJPAException, ODataJPAProcessException, SerializerException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(requestContext.getCUDRequestHandler()).thenReturn(handler);
    when(handler.updateEntity(any(), any(), any())).thenReturn(null);

    assertThrows(ODataException.class,
        () -> processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON));
    verify(transaction, never()).commit();
    verify(transaction, times(1)).rollback();
  }

  @Test
  public void testDoesRollbackOnWrongResponse() throws ODataJPAException, SerializerException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();
    JPAUpdateResult result = new JPAUpdateResult(false, "");
    JPACUDRequestHandler handler = mock(JPACUDRequestHandler.class);
    when(requestContext.getCUDRequestHandler()).thenReturn(handler);
    when(handler.updateEntity(any(), any(), any())).thenReturn(result);

    assertThrows(ODataException.class,
        () -> processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON));
    verify(transaction, never()).commit();
    verify(transaction, times(1)).rollback();
  }

  @Test
  public void testResponseErrorIfNull() throws ODataJPAException, SerializerException, ODataJPASerializerException, ODataJPAProcessorException {

    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareRepresentationRequest(new RequestHandleSpy(null));

    try {
      processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    } catch (ODataJPAProcessException | ODataLibraryException e) {
        assertTrue(true);
      return;
    }
    fail();
  }

  @Test
  public void testResponseUpdateLink() throws ODataJPAException, ODataLibraryException, ODataJPAProcessException {

    final AdministrativeDivisionKey key = new AdministrativeDivisionKey("Eurostat", "NUTS2", "DE60");
    final AdministrativeDivision resultEntity = new AdministrativeDivision(key);

    final AdministrativeDivisionKey childKey = new AdministrativeDivisionKey("Eurostat", "NUTS3", "DE600");
    final AdministrativeDivision childEntity = new AdministrativeDivision(childKey);

    final JPAUpdateResult result = new JPAUpdateResult(false, resultEntity);
    final ODataResponse response = new ODataResponse();
    final ODataRequest request = prepareLinkRequest(new RequestHandleSpy(result));

    resultEntity.setChildren(new ArrayList<>(Collections.singletonList(childEntity)));

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    assertNotNull(response);

  }

  @Test
  public void testOwnTransactionCommitted() throws ODataJPAException, ODataLibraryException, ODataJPAProcessException {
    ODataResponse response = new ODataResponse();
    ODataRequest request = prepareSimpleRequest();

    RequestHandleSpy spy = new RequestHandleSpy();
    when(requestContext.getCUDRequestHandler()).thenReturn(spy);

    processor.updateEntity(request, response, ContentType.JSON, ContentType.JSON);
    verify(transaction, times(1)).commit();
  }

  static class RequestHandleSpy extends JPAAbstractCUDRequestHandler {
    public int noValidateCalls;
    public JPAEntityType et;
    public Map<String, Object> jpaAttributes;
    public EntityManager em;
    public boolean called = false;
    public HttpMethod method;
    public Map<String, List<String>> headers;
    private final JPAUpdateResult change;
    public Optional<JPAODataClaimProvider> claims;
    public List<String> groups;

    RequestHandleSpy() {
      this(new JPAUpdateResult(true, new Organization()));
    }

    RequestHandleSpy(final JPAUpdateResult typeOfChange) {
      this.change = typeOfChange;
    }

    @Override
    public JPAUpdateResult updateEntity(final JPARequestEntity requestEntity, final EntityManager em,
        final HttpMethod verb) {
      this.et = requestEntity.getEntityType();
      this.jpaAttributes = requestEntity.getData();
      // this.keys = requestEntity.getKeys();
      this.em = em;
      this.called = true;
      this.method = verb;
      this.headers = requestEntity.getAllHeader();
      this.claims = requestEntity.getClaims();
      this.groups = requestEntity.getGroups();
      return change;
    }

    @Override
    public void validateChanges(final EntityManager em) {
      this.noValidateCalls++;
    }
  }
}