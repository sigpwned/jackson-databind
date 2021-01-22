package com.fasterxml.jackson.databind.deser.std;

import java.util.Collection;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Base class that simplifies implementations of {@link JsonDeserializer}s
 * that mostly delegate functionality to another deserializer implementation
 * (possibly forming a chain of deserializers delegating functionality
 * in some cases)
 */
public abstract class DelegatingDeserializer
    extends StdDeserializer<Object>
{
    protected final JsonDeserializer<?> _delegatee;
    
    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    public DelegatingDeserializer(JsonDeserializer<?> d)
    {
        super(d.handledType());
        _delegatee = d;
    }

    /*
    /**********************************************************************
    /* Abstract methods to implement
    /**********************************************************************
     */
    
    protected abstract JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee);
    
    /*
    /**********************************************************************
    /* Overridden methods for contextualization, resolving
    /**********************************************************************
     */

    @Override
    public void resolve(DeserializationContext ctxt) {
        if (_delegatee != null) {
            _delegatee.resolve(ctxt);
        }
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        JavaType vt = ctxt.constructType(_delegatee.handledType());
        JsonDeserializer<?> del = ctxt.handleSecondaryContextualization(_delegatee,
                property, vt);
        if (del == _delegatee) {
            return this;
        }
        return newDelegatingInstance(del);
    }

    @Override
    public SettableBeanProperty findBackReference(String logicalName) {
        // [databind#253]: Hope this works....
        return _delegatee.findBackReference(logicalName);
    }

    @Override
    public JsonDeserializer<?> replaceDelegatee(JsonDeserializer<?> delegatee)
    {
        if (delegatee == _delegatee) {
            return this;
        }
        return newDelegatingInstance(delegatee);
    }

    /*
    /**********************************************************************
    /* Overridden deserialization methods
    /**********************************************************************
     */

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        return _delegatee.deserialize(p,  ctxt);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt,
            Object intoValue)
        throws JacksonException
    {
        return ((JsonDeserializer<Object>)_delegatee).deserialize(p, ctxt, intoValue);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws JacksonException
    {
        return _delegatee.deserializeWithType(p, ctxt, typeDeserializer);
    }

    /*
    /**********************************************************************
    /* Overridden other methods
    /**********************************************************************
     */

    @Override
    public JsonDeserializer<?> getDelegatee() {
        return _delegatee;
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return _delegatee.getNullAccessPattern();
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt) {
        return _delegatee.getNullValue(ctxt);
    }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return _delegatee.getEmptyValue(ctxt);
    }

    @Override
    public LogicalType logicalType() {
        return _delegatee.logicalType();
    }

    @Override
    public Collection<Object> getKnownPropertyNames() { return _delegatee.getKnownPropertyNames(); }

    @Override
    public ObjectIdReader getObjectIdReader(DeserializationContext ctxt) {
        return _delegatee.getObjectIdReader(ctxt);
    }

    @Override
    public boolean isCachable() {
        return (_delegatee != null) && _delegatee.isCachable();
    }

    @Override
    public Boolean supportsUpdate(DeserializationConfig config) {
        return _delegatee.supportsUpdate(config);
    }
}
