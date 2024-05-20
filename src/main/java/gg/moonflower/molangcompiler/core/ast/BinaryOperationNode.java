package gg.moonflower.molangcompiler.core.ast;

import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.api.exception.MolangSyntaxException;
import gg.moonflower.molangcompiler.core.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.core.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * Compares the two values and runs an operation on them.
 *
 * @param operator The operator to apply
 * @param left     The first operand
 * @param right    The second operand
 * @author Buddy
 */
@ApiStatus.Internal
public class BinaryOperationNode implements Node {

    private final BinaryOperation operator;
    private final Node left;
    private final Node right;

    public BinaryOperationNode(BinaryOperation operator, Node left, Node right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return "(" + this.left + " " + this.operator + " " + this.right + ")";
    }

    @Override
    public boolean isConstant() {
        return this.left.isConstant() && (this.operator == BinaryOperation.NULL_COALESCING || this.right.isConstant());
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public float evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        float left = this.left.evaluate(environment);
        float right = this.right.evaluate(environment);
        switch (this.operator) {
            case ADD:
                return left + right;
            case SUBTRACT:
                return left - right;
            case MULTIPLY:
                return left * right;
            case DIVIDE:
                return left / right;
            case AND:
                return left != 0 && right != 0 ? 1.0F : 0.0F;
            case OR:
                return left != 0 || right != 0 ? 1.0F : 0.0F;
            case LESS:
                return left < right ? 1.0F : 0.0F;
            case LESS_EQUALS:
                return left <= right ? 1.0F : 0.0F;
            case GREATER:
                return left > right ? 1.0F : 0.0F;
            case GREATER_EQUALS:
                return left >= right ? 1.0F : 0.0F;
            case EQUALS:
                return left == right ? 1.0F : 0.0F;
            case NOT_EQUALS:
                return left != right ? 1.0F : 0.0F;
            case NULL_COALESCING:
                // If the left is constant, then the value always exists and returns the first value
                return left;
        }
        return 0;
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (environment.optimize()) {
            if (this.isConstant()) {
                BytecodeCompiler.writeFloatConst(method, this.evaluate(environment));
                return;
            }
        }

        switch (this.operator) {
            case AND: {
                Label label_false = new Label();
                Label label_end = new Label();
                writeNode(this.left, method, environment, breakLabel, continueLabel);
                //left == 0: goto false
                method.visitInsn(Opcodes.FCONST_0);
                method.visitInsn(Opcodes.FCMPL);
                method.visitJumpInsn(Opcodes.IFEQ, label_false);

                //right == 0: goto false
                writeNode(this.right, method, environment, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FCONST_0);
                method.visitInsn(Opcodes.FCMPL);
                method.visitJumpInsn(Opcodes.IFEQ, label_false);

                //else: true
                method.visitInsn(Opcodes.FCONST_1);
                method.visitJumpInsn(Opcodes.GOTO, label_end);

                //false:
                method.visitLabel(label_false);
                method.visitInsn(Opcodes.FCONST_0);

                //end:
                method.visitLabel(label_end);

                break;
            }
            case OR: {
                Label label_true = new Label();
                Label label_end = new Label();
                //left != 0: goto true
                writeNode(this.left, method, environment, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FCONST_0);
                method.visitInsn(Opcodes.FCMPL);
                method.visitJumpInsn(Opcodes.IFNE, label_true);

                //right != 0: goto true
                writeNode(this.right, method, environment, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FCONST_0);
                method.visitInsn(Opcodes.FCMPL);
                method.visitJumpInsn(Opcodes.IFNE, label_true);

                //else: false
                method.visitInsn(Opcodes.FCONST_0);
                method.visitJumpInsn(Opcodes.GOTO, label_end);

                //true:
                method.visitLabel(label_true);
                method.visitInsn(Opcodes.FCONST_1);

                //end:
                method.visitLabel(label_end);

                break;
            }
            case NULL_COALESCING: {
                if (!(this.left instanceof VariableGetNode)) {
                    throw new MolangSyntaxException("Expected variable lookup, got " + this.left);
                }
                VariableGetNode lookup = (VariableGetNode)this.left;

                // Test if variable exists
                environment.loadObjectHas(method, lookup.object(), lookup.name());

                // Run branches
                Label label_false = new Label();
                Label label_end = new Label();
                method.visitJumpInsn(Opcodes.IFEQ, label_false);
                writeNode(this.left, method, environment, breakLabel, continueLabel);
                method.visitJumpInsn(Opcodes.GOTO, label_end);
                method.visitLabel(label_false);
                writeNode(this.right, method, environment, breakLabel, continueLabel);
                method.visitLabel(label_end);

                break;
            }
            case MULTIPLY: {
                if (environment.optimize() && this.tryWriteNegate(method, environment, breakLabel, continueLabel)) {
                    return;
                }

                writeNode(this.left, method, environment, breakLabel, continueLabel);
                writeNode(this.right, method, environment, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FMUL);

                break;
            }
            case DIVIDE: {
                if (environment.optimize() && this.tryWriteNegate(method, environment, breakLabel, continueLabel)) {
                    return;
                }

                writeNode(this.left, method, environment, breakLabel, continueLabel);
                writeNode(this.right, method, environment, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FDIV);

                break;
            }
            default: {
                writeNode(this.left, method, environment, breakLabel, continueLabel);
                writeNode(this.right, method, environment, breakLabel, continueLabel);

                switch (this.operator) {
                    case ADD:
                        method.visitInsn(Opcodes.FADD);
                        break;
                    case SUBTRACT:
                        method.visitInsn(Opcodes.FSUB);
                        break;
                    case EQUALS:
                        writeComparision(method, Opcodes.IFNE);
                        break;
                    case NOT_EQUALS:
                        writeComparision(method, Opcodes.IFEQ);
                        break;
                    case LESS_EQUALS:
                        writeComparision(method, Opcodes.IFGT);
                        break;
                    case LESS:
                        writeComparision(method, Opcodes.IFGE);
                        break;
                    case GREATER_EQUALS:
                        writeComparision(method, Opcodes.IFLT);
                        break;
                    case GREATER:
                        writeComparision(method, Opcodes.IFLE);
                        break;
                }
            }
        }
    }

    // Try to replace with the negate operation if multiplying/dividing by -1
    private boolean tryWriteNegate(MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (this.left.isConstant()) {
            float left = this.left.evaluate(environment);
            if (left == -1.0F) {
                this.right.writeBytecode(method, environment, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FNEG);
                return true;
            }
        } else if (this.right.isConstant()) {
            float right = this.right.evaluate(environment);
            if (right == -1.0F) {
                this.left.writeBytecode(method, environment, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FNEG);
                return true;
            }
        }
        return false;
    }

    private static void writeNode(Node node, MethodNode method, MolangBytecodeEnvironment environment, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        if (environment.optimize() && node.isConstant()) {
            BytecodeCompiler.writeFloatConst(method, node.evaluate(environment));
        } else {
            node.writeBytecode(method, environment, breakLabel, continueLabel);
        }
    }

    private static void writeComparision(MethodNode method, int success) {
        Label label_false = new Label();
        Label label_end = new Label();
        method.visitInsn(Opcodes.FCMPL);
        method.visitJumpInsn(success, label_false);
        method.visitInsn(Opcodes.FCONST_1);
        method.visitJumpInsn(Opcodes.GOTO, label_end);
        method.visitLabel(label_false);
        method.visitInsn(Opcodes.FCONST_0);
        method.visitLabel(label_end);
    }
}
