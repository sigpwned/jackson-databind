package com.fasterxml.jackson.databind.jsontype;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

// for [databind#43], deduction-based polymorphism
public class TestPolymorphicDeduction extends DatabindTestUtil {

  @JsonTypeInfo(use = DEDUCTION)
  @JsonSubTypes( {@Type(LiveCat.class), @Type(DeadCat.class), @Type(Fleabag.class)})
  // A general supertype with no properties - used for tests involving {}
  interface Feline {}

  @JsonTypeInfo(use = DEDUCTION)
  @JsonSubTypes( {@Type(LiveCat.class), @Type(DeadCat.class)})
  // A supertype containing common properties
  public static class Cat implements Feline {
    public String name;
  }

  // Distinguished by its parent and a unique property
  static class DeadCat extends Cat {
    public String causeOfDeath;
  }

  // Distinguished by its parent and a unique property
  static class LiveCat extends Cat {
    public boolean angry;
  }

  // No distinguishing properties whatsoever
  static class Fleabag implements Feline {
    // NO OP
  }

  // Something to put felines in
  static class Box {
    public Feline feline;
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
  static class Bean3711 {
      @JsonValue
      public String ser = "value";
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
  static enum Enum3711 { A, B }

  /*
  /**********************************************************
  /* Mock data
  /**********************************************************
   */

  private static final String deadCatJson = a2q("{'causeOfDeath':'entropy','name':'Felix'}");
  private static final String liveCatJson = a2q("{'angry':true,'name':'Felix'}");
  private static final String luckyCatJson = a2q("{'name':'Felix','angry':true,'lives':8}");
  private static final String ambiguousCatJson = a2q("{'name':'Felix','age':2}");
  private static final String fleabagJson = a2q("{}");
  private static final String box1Json = a2q("{'feline':" + liveCatJson + "}");
  private static final String box2Json = a2q("{'feline':" + deadCatJson + "}");
  private static final String box3Json = a2q("{'feline':" + fleabagJson + "}");
  private static final String box4Json = a2q("{'feline':null}");
  private static final String box5Json = a2q("{}");
  private static final String arrayOfCatsJson = a2q("[" + liveCatJson + "," + deadCatJson + "]");
  private static final String mapOfCatsJson = a2q("{'live':" + liveCatJson + "}");

  /*
  /**********************************************************
  /* Test methods
  /**********************************************************
   */

  private final ObjectMapper MAPPER = jsonMapperBuilder()
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .build();

  @Test
  public void testSimpleInference() throws Exception {
    Cat cat = MAPPER.readValue(liveCatJson, Cat.class);
    assertTrue(cat instanceof LiveCat);
    assertSame(cat.getClass(), LiveCat.class);
    assertEquals("Felix", cat.name);
    assertTrue(((LiveCat)cat).angry);

    cat = MAPPER.readValue(deadCatJson, Cat.class);
    assertTrue(cat instanceof DeadCat);
    assertSame(cat.getClass(), DeadCat.class);
    assertEquals("Felix", cat.name);
    assertEquals("entropy", ((DeadCat)cat).causeOfDeath);
  }

  @Test
  public void testSimpleInferenceOfEmptySubtype() throws Exception {
    // Given:
    ObjectMapper mapper = MAPPER;
    // When:
    Feline feline = mapper.readValue(fleabagJson, Feline.class);
    // Then:
    assertTrue(feline instanceof Fleabag);
  }

  @Test
  public void testSimpleInferenceOfEmptySubtypeDoesntMatchNull() throws Exception {
    // Given:
    ObjectMapper mapper = MAPPER;
    // When:
    Feline feline = mapper.readValue("null", Feline.class);
    // Then:
    assertNull(feline);
  }

  @Test
  public void testCaseInsensitiveInference() throws Exception {
    Cat cat = JsonMapper.builder() // Don't use shared mapper!
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build()
      .readValue(deadCatJson.toUpperCase(), Cat.class);
    assertTrue(cat instanceof DeadCat);
    assertSame(cat.getClass(), DeadCat.class);
    assertEquals("FELIX", cat.name);
    assertEquals("ENTROPY", ((DeadCat)cat).causeOfDeath);
  }

  // TODO not currently supported
//  public void testCaseInsensitivePerFieldInference() throws Exception {
//    ObjectMapper mapper = JsonMapper.builder() // Don't use shared mapper!
//       .configOverride(DeadCat.class)
//      .setFormat(JsonFormat.Value.empty()
//      .withFeature(JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES));
//    Cat cat = mapper.readValue(deadCatJson.replace("causeOfDeath", "CAUSEOFDEATH"), Cat.class);
//    assertTrue(cat instanceof DeadCat);
//    assertSame(cat.getClass(), DeadCat.class);
//    assertEquals("Felix", cat.name);
//    assertEquals("Entropy", ((DeadCat)cat).causeOfDeath);
//  }

  @Test
  public void testContainedInference() throws Exception {
    Box box = MAPPER.readValue(box1Json, Box.class);
    assertTrue(box.feline instanceof LiveCat);
    assertSame(box.feline.getClass(), LiveCat.class);
    assertEquals("Felix", ((LiveCat)box.feline).name);
    assertTrue(((LiveCat)box.feline).angry);

    box = MAPPER.readValue(box2Json, Box.class);
    assertTrue(box.feline instanceof DeadCat);
    assertSame(box.feline.getClass(), DeadCat.class);
    assertEquals("Felix", ((DeadCat)box.feline).name);
    assertEquals("entropy", ((DeadCat)box.feline).causeOfDeath);
  }

  @Test
  public void testContainedInferenceOfEmptySubtype() throws Exception {
    Box box = MAPPER.readValue(box3Json, Box.class);
    assertTrue(box.feline instanceof Fleabag);

    box = MAPPER.readValue(box4Json, Box.class);
    assertNull(box.feline, "null != {}");

    box = MAPPER.readValue(box5Json, Box.class);
    assertNull(box.feline, "<absent> != {}");
  }

  @Test
  public void testListInference() throws Exception {
    JavaType listOfCats = defaultTypeFactory().constructParametricType(List.class, Cat.class);
    List<Cat> boxes = MAPPER.readValue(arrayOfCatsJson, listOfCats);
    assertTrue(boxes.get(0) instanceof LiveCat);
    assertTrue(boxes.get(1) instanceof DeadCat);
  }

  @Test
  public void testMapInference() throws Exception {
    JavaType mapOfCats = defaultTypeFactory().constructParametricType(Map.class, String.class, Cat.class);
    Map<String, Cat> map = MAPPER.readValue(mapOfCatsJson, mapOfCats);
    assertEquals(1, map.size());
    assertTrue(map.entrySet().iterator().next().getValue() instanceof LiveCat);
  }

  @Test
  public void testArrayInference() throws Exception {
    Cat[] boxes = MAPPER.readValue(arrayOfCatsJson, Cat[].class);
    assertTrue(boxes[0] instanceof LiveCat);
    assertTrue(boxes[1] instanceof DeadCat);
  }

  @Test
  public void testIgnoreProperties() throws Exception {
    Cat cat = MAPPER.reader()
      .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .readValue(luckyCatJson, Cat.class);
    assertTrue(cat instanceof LiveCat);
    assertSame(cat.getClass(), LiveCat.class);
    assertEquals("Felix", cat.name);
    assertTrue(((LiveCat)cat).angry);
  }

  static class AnotherLiveCat extends Cat {
    public boolean angry;
  }

  @Test
  public void testAmbiguousClasses() throws Exception {
    try {
      ObjectMapper mapper = JsonMapper.builder() // Don't use shared mapper!
              .registerSubtypes(AnotherLiveCat.class)
              .build();
      /*Cat cat =*/ mapper.readValue(liveCatJson, Cat.class);
      fail("Should not get here");
    } catch (InvalidDefinitionException e) {
        verifyException(e, "Subtypes ");
        verifyException(e, "have the same signature");
        verifyException(e, "cannot be uniquely deduced");
    }
  }

  @Test
  public void testAmbiguousProperties() throws Exception {
    try {
      /*Cat cat =*/ MAPPER.readValue(ambiguousCatJson, Cat.class);
      fail("Should not get here");
    } catch (InvalidTypeIdException e) {
        verifyException(e, "Cannot deduce unique subtype");
    }
  }

  @Test
  public void testFailOnInvalidSubtype() throws Exception {
    // Given:
    JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
      .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
      .build();
    // When:
    Cat cat = mapper.readValue(ambiguousCatJson, Cat.class);
    // Then:
    assertNull(cat);
  }

  @JsonTypeInfo(use = DEDUCTION, defaultImpl = Cat.class)
  abstract static class CatMixin {
  }

  @Test
  public void testDefaultImpl() throws Exception {
    // Given:
    JsonMapper mapper = JsonMapper.builder() // Don't use shared mapper!
      .addMixIn(Cat.class, CatMixin.class)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .build();
    // When:
    Cat cat = mapper.readValue(ambiguousCatJson, Cat.class);
    // Then:
    // Even though "age":2 implies this was a failed subtype, we are instructed to fallback to Cat regardless.
    assertTrue(cat instanceof Cat);
    assertSame(Cat.class, cat.getClass());
    assertEquals("Felix", cat.name);
  }

  @Test
  public void testSimpleSerialization() throws Exception {
    // Given:
    JavaType listOfCats = defaultTypeFactory().constructParametricType(List.class, Cat.class);
    List<Cat> list = MAPPER.readValue(arrayOfCatsJson, listOfCats);
    Cat cat = list.get(0);
    // When:
    String json = MAPPER.writeValueAsString(cat);
    // Then:
    assertEquals(liveCatJson, json);
  }

  @Test
  public void testListSerialization() throws Exception {
    // Given:
    JavaType listOfCats = defaultTypeFactory().constructParametricType(List.class, Cat.class);
    List<Cat> list = MAPPER.readValue(arrayOfCatsJson, listOfCats);
    // When:
    String json = MAPPER.writeValueAsString(list);
    // Then:
    assertEquals(arrayOfCatsJson, json);
  }

  // [databind#3711]
  @Test
  public void testWithPojoAsJsonValue() throws Exception
  {
      assertEquals(q("value"), MAPPER.writeValueAsString(new Bean3711()));
  }

  // [databind#3711]
  @Test
  public void testWithEnum() throws Exception
  {
      assertEquals(q("B"), MAPPER.writeValueAsString(Enum3711.B));
  }
}
