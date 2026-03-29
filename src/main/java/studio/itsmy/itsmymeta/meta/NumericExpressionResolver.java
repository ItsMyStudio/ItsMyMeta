package studio.itsmy.itsmymeta.meta;

import com.ezylang.evalex.Expression;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class NumericExpressionResolver {

    private static final Pattern SIMPLE_INTEGER = Pattern.compile("[-+]?\\d+");
    private static final Pattern SIMPLE_DECIMAL = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");

    public Object resolve(String expression, boolean decimal) {
        String normalizedExpression = expression.trim();
        if (isSimpleNumber(normalizedExpression)) {
            if (decimal) {
                return Double.parseDouble(normalizedExpression);
            }
            validateIntegerValue(new BigDecimal(normalizedExpression), normalizedExpression);
            return Integer.parseInt(normalizedExpression);
        }

        try {
            BigDecimal result = new Expression(normalizedExpression).evaluate().getNumberValue();
            if (result == null) {
                throw new IllegalArgumentException("Expression did not produce a numeric result: " + normalizedExpression);
            }

            if (decimal) {
                return result.doubleValue();
            }

            validateIntegerValue(result, normalizedExpression);
            return result.intValueExact();
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid numeric expression: " + normalizedExpression, exception);
        }
    }

    private boolean isSimpleNumber(String expression) {
        return SIMPLE_INTEGER.matcher(expression).matches() || SIMPLE_DECIMAL.matcher(expression).matches();
    }

    private void validateIntegerValue(BigDecimal value, String expression) {
        if (value.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("Expression must resolve to a whole number: " + expression);
        }
        value.setScale(0, RoundingMode.UNNECESSARY);
    }
}
