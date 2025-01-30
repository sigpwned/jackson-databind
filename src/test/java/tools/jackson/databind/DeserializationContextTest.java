package tools.jackson.databind;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DeserializationContextTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    static class Bean4934 {
        public String value;
    }

    // [databind#4934]
    @Test
    public void testTreeAsValueFromNulls() throws Exception
    {
        final JsonNodeFactory nodeF = MAPPER.getNodeFactory();
        try (JsonParser p = MAPPER.createParser("abc")) {
            DeserializationContext ctxt = MAPPER.readerFor(String.class)._deserializationContext(p);

            assertNull(ctxt.readTreeAsValue(nodeF.nullNode(), Boolean.class));

            // Fails on 3.0, investigate

            //assertEquals(Boolean.FALSE, ctxt.readTreeAsValue(nodeF.nullNode(), Boolean.TYPE));

            // Only fixed in 2.19:
            //assertNull(ctxt.readTreeAsValue(nodeF.nullNode(), Bean4934.class));

        }
    }

    // [databind#4934]
    @Test
    public void testTreeAsValueFromMissing() throws Exception
    {
        final JsonNodeFactory nodeF = MAPPER.getNodeFactory();
        try (JsonParser p = MAPPER.createParser("abc")) {
            DeserializationContext ctxt = MAPPER.readerFor(String.class)._deserializationContext(p);

            assertNull(ctxt.readTreeAsValue(nodeF.missingNode(), Boolean.class));
            // Absent becomes `null` for now as well
            assertNull(ctxt.readTreeAsValue(nodeF.missingNode(), Boolean.TYPE));

            // Only fixed in 2.19:
            //assertNull(ctxt.readTreeAsValue(nodeF.missingNode(), Bean4934.class));
        }
    }
}
