package gg.moonflower.molangcompiler.core.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangConstantNode implements MolangExpression {

    private final float value;

    public MolangConstantNode(float value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MolangConstantNode)) return false;
        MolangConstantNode that = (MolangConstantNode) o;
        return Float.compare(value, that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public float get(@Nullable MolangEnvironment environment) {
        return this.value;
    }

    @Override
    public float getConstant() {
        return this.value;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public String toString() {
        return Float.toString(this.value);
    }
}
