package gg.moonflower.molangcompiler.core.node;

import gg.moonflower.molangcompiler.api.MolangEnvironment;
import gg.moonflower.molangcompiler.api.MolangExpression;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MolangDynamicNode implements MolangExpression {

    private final Supplier<Float> value;

    public MolangDynamicNode(Supplier<Float> value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MolangDynamicNode)) return false;
        MolangDynamicNode that = (MolangDynamicNode) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public float get(MolangEnvironment environment) {
        return this.value.get();
    }

    @Override
    public String toString() {
        return Float.toString(this.value.get());
    }
}
