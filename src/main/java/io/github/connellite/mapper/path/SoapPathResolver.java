package io.github.connellite.mapper.path;

import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@UtilityClass
public class SoapPathResolver {

    private static final ExpressionParser SPEL = new SpelExpressionParser();

    public static Object read(Object soapResult, String spelExpression) {
        if (soapResult == null) {
            return null;
        }
        Object root = unwrapSoapContainer(soapResult);
        StandardEvaluationContext context = new StandardEvaluationContext(root);
        context.setRootObject(root);
        context.addPropertyAccessor(new MapAccessor());
        context.addPropertyAccessor(new SyntheticSoapWrapperAccessor());
        try {
            return SPEL.parseExpression(spelExpression).getValue(context);
        } catch (EvaluationException exception) {
            throw new IllegalStateException(
                    "Cannot evaluate SpEL '" + spelExpression + "' on " + root.getClass().getName(),
                    exception);
        }
    }

    private static Object unwrapSoapContainer(Object soapResult) {
        if (soapResult.getClass().isArray()) {
            Object[] array = (Object[]) soapResult;
            if (array.length == 1) {
                return unwrapSoapContainer(array[0]);
            }
        }
        if (soapResult instanceof Collection<?> collection && collection.size() == 1) {
            return unwrapSoapContainer(collection.iterator().next());
        }
        return soapResult;
    }

    private static final class SyntheticSoapWrapperAccessor implements PropertyAccessor {

        private static final List<String> WRAPPER_FIELDS = List.of("return", "result");

        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return null;
        }

        @Override
        public boolean canRead(EvaluationContext context, Object target, String name) {
            return target != null
                    && !(target instanceof Map<?, ?>)
                    && WRAPPER_FIELDS.contains(name)
                    && !new BeanWrapperImpl(target).isReadableProperty(name);
        }

        @Override
        public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
            if (!canRead(context, target, name)) {
                throw new AccessException("Synthetic SOAP wrapper field is not readable: " + name);
            }
            return new TypedValue(target);
        }

        @Override
        public boolean canWrite(EvaluationContext context, Object target, String name) {
            return false;
        }

        @Override
        public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
            throw new AccessException("Synthetic SOAP wrapper fields are read-only");
        }
    }
}
