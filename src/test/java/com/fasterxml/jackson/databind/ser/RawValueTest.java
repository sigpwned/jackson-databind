package com.fasterxml.jackson.databind.ser;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.util.RawValue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This unit test suite tests functioning of {@link JsonRawValue}
 * annotation with bean serialization.
 */
public class RawValueTest
    extends DatabindTestUtil
{
    /*
    /*********************************************************
    /* Helper bean classes
    /*********************************************************
     */

    /// Class for testing {@link JsonRawValue} annotations with getters returning String
    @JsonPropertyOrder(alphabetic=true)
    final static class ClassGetter<T>
    {
        protected final T _value;

        protected ClassGetter(T v) { _value = v;}

        public T getNonRaw() { return _value; }

        @JsonProperty("raw") @JsonRawValue public T foobar() { return _value; }

        @JsonProperty @JsonRawValue protected T value() { return _value; }
    }

    // [databind#348]
    static class RawWrapped
    {
        @JsonRawValue
        private final String json;

        public RawWrapped(String str) {
            json = str;
        }
    }

    /*
    /*********************************************************
    /* Test cases
    /*********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSimpleStringGetter() throws Exception
    {
        String value = "abc";
        String result = MAPPER.writeValueAsString(new ClassGetter<String>(value));
        String expected = String.format("{\"nonRaw\":\"%s\",\"raw\":%s,\"value\":%s}", value, value, value);
        assertEquals(expected, result);
    }

    @Test
    public void testSimpleNonStringGetter() throws Exception
    {
        int value = 123;
        String result = MAPPER.writeValueAsString(new ClassGetter<Integer>(value));
        String expected = String.format("{\"nonRaw\":%d,\"raw\":%d,\"value\":%d}", value, value, value);
        assertEquals(expected, result);
    }

    @Test
    public void testNullStringGetter() throws Exception
    {
        String result = MAPPER.writeValueAsString(new ClassGetter<String>(null));
        String expected = "{\"nonRaw\":null,\"raw\":null,\"value\":null}";
        assertEquals(expected, result);
    }

    @Test
    public void testWithValueToTree() throws Exception
    {
        JsonNode w = MAPPER.valueToTree(new RawWrapped("{ }"));
        assertNotNull(w);
        assertEquals("{\"json\":{ }}", MAPPER.writeValueAsString(w));
    }

    // for [databind#743]
    @Test
    public void testRawFromMapToTree() throws Exception
    {
        RawValue myType = new RawValue("Jackson");

        Map<String, Object> object = new HashMap<String, Object>();
        object.put("key", myType);
        JsonNode jsonNode = MAPPER.valueToTree(object);
        String json = MAPPER.writeValueAsString(jsonNode);
        assertEquals("{\"key\":Jackson}", json);
    }
}
