package io.github.ocelot.molangcompiler.api;

import io.github.ocelot.molangcompiler.api.exception.MolangSyntaxException;
import io.github.ocelot.molangcompiler.core.ast.Node;
import io.github.ocelot.molangcompiler.core.compiler.BytecodeCompiler;
import io.github.ocelot.molangcompiler.core.compiler.MolangLexer;
import io.github.ocelot.molangcompiler.core.compiler.MolangTokenizer;

/**
 * <p>Compiles a {@link MolangExpression} from a string input.</p>
 * <p>A compiler instance must be created to allow garbage collection of generated classes when no longer in use.</p>
 *
 * @author Ocelot
 * @since 1.0.0
 */
public class MolangCompiler {

    /**
     * Whether to reduce math to constant values if possible. E.g. <code>4 * 4 + 2</code> would become <code>18</code>. This should almost always be on.
     */
    public static final int OPTIMIZE_FLAG = 0b01;
    /**
     * Whether to write the java bytecode to a class file. This is only for debugging.
     */
    public static final int WRITE_CLASSES_FLAG = 0b10;

    /**
     * All default compilation flags. This may change in future versions as more options are added.
     */
    public static final int DEFAULT_FLAGS = OPTIMIZE_FLAG;

    private final BytecodeCompiler compiler;

    public MolangCompiler() {
        this(DEFAULT_FLAGS);
    }

    public MolangCompiler(int flags) {
        this.compiler = new BytecodeCompiler(flags);
    }

    /**
     * Compiles a {@link MolangExpression} from the specified string input.
     *
     * @param input The data to compile
     * @return The compiled expression
     * @throws MolangSyntaxException If any error occurs
     */
    public MolangExpression compile(String input) throws MolangSyntaxException {
        MolangTokenizer.Token[] tokens = MolangTokenizer.createTokens(input);
        Node node = MolangLexer.parseTokens(tokens);
        return this.compiler.build(node);
    }
}
