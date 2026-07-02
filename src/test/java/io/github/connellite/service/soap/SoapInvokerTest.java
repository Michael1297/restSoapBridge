package io.github.connellite.service.soap;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SoapInvokerTest {

    @Test
    void convertArgument_convertsMapToExpectedBeanType() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("query", "contract");
        value.put("limit", 10);

        Object converted = SoapInvoker.convertArgument(value, SimpleQueryParams.class);

        SimpleQueryParams params = assertInstanceOf(SimpleQueryParams.class, converted);
        assertEquals("contract", params.getQuery());
        assertEquals(10, params.getLimit());
    }

    @Test
    void convertArgument_acceptsSingleValueAsNestedList() {
        Map<String, Object> restriction = new LinkedHashMap<>();
        restriction.put("valueList", "qui");

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("query", java.util.List.of(restriction));

        Object converted = SoapInvoker.convertArgument(value, DtoDocumentQueryParams.class);

        DtoDocumentQueryParams params = assertInstanceOf(DtoDocumentQueryParams.class, converted);
        assertEquals("qui", params.getQuery().get(0).getValueList().get(0));
    }

    @Test
    void convertArgument_convertsSimpleValuesToExpectedType() {
        Object converted = SoapInvoker.convertArgument("10", Integer.class);

        assertEquals(10, converted);
    }

    public static class SimpleQueryParams {

        private String query;
        private int limit;

        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }
    }

    public static class DtoDocumentQueryParams {

        private java.util.List<DtoAttributeRestriction> query;

        public java.util.List<DtoAttributeRestriction> getQuery() {
            return query;
        }

        public void setQuery(java.util.List<DtoAttributeRestriction> query) {
            this.query = query;
        }
    }

    public static class DtoAttributeRestriction {

        private java.util.List<String> valueList;

        public java.util.List<String> getValueList() {
            return valueList;
        }

        public void setValueList(java.util.List<String> valueList) {
            this.valueList = valueList;
        }
    }
}
