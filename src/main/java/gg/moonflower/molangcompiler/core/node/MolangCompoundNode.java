package gg.moonflower.molangcompiler.core.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.exception.MolangRuntimeException;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangCompoundNode implements MolangExpression {

    private final MolangExpression[] expressions;

    public MolangCompoundNode(MolangExpression... expressions) {
        this.expressions = expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MolangCompoundNode)) return false;
        MolangCompoundNode that = (MolangCompoundNode) o;
        return Objects.deepEquals(expressions, that.expressions);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(expressions);
    }

    @Override
    public float get(MolangEnvironment environment) throws MolangRuntimeException {
        for (int i = 0; i < this.expressions.length; i++) {
            float result = environment.resolve(this.expressions[i]);
            // The last expression is expected to have the `return`
            if (i >= this.expressions.length - 1) {
                return result;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < this.expressions.length; i++) {
            if (i >= this.expressions.length - 1) {
                builder.append("return ");
            }
            builder.append(this.expressions[i]);
            builder.append(';');
            if (i < this.expressions.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }
}
