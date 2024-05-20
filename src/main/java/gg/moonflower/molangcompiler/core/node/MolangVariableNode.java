package gg.moonflower.molangcompiler.core.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import gg.moonflower.molangcompiler.api.bridge.MolangVariable;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangVariableNode implements MolangExpression, MolangVariable {

    private final MolangVariable value;

    public MolangVariableNode(MolangVariable value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MolangVariableNode)) return false;
        MolangVariableNode that = (MolangVariableNode) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public float get(MolangEnvironment environment) {
        return this.value.getValue();
    }

    @Override
    public String toString() {
        return Float.toString(this.value.getValue());
    }

    @Override
    public float getValue() {
        return this.value.getValue();
    }

    @Override
    public void setValue(float value) {
        this.value.setValue(value);
    }

    @Override
    public MolangExpression getCopy() {
        return new MolangVariableNode(this.copy());
    }

    @Override
    public MolangVariable copy() {
        return this.value.copy();
    }
}
