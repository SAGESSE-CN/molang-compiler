package io.github.ocelot.molangcompiler.core.ast;

import org.jetbrains.annotations.ApiStatus;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public interface OptionalValueNode extends Node {

    Node withReturnValue();
}
