package gg.moonflower.molangcompiler.core.ast;

import gg.moonflower.molangcompiler.api.exception.MolangException;
import gg.moonflower.molangcompiler.core.MolangUtil;
import gg.moonflower.molangcompiler.core.compiler.BytecodeCompiler;
import gg.moonflower.molangcompiler.core.compiler.MolangBytecodeEnvironment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Runs a math function directly from Java if possible.
 *
 * @param function  The function to run
 * @param arguments The parameters to pass into the function
 * @author Ocelot, Buddy
 */
@ApiStatus.Internal
public class MathNode implements Node {

    private static final float RADIANS_TO_DEGREES = (float) (180 / Math.PI);
    private static final float DEGREES_TO_RADIANS = (float) (Math.PI / 180);

    private final MathOperation function;
    private final Node[] arguments;

    public MathNode(MathOperation function, Node... arguments) {
        this.function = function;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        if (this.function.getParameters() == 0) {
            return "math." + this.function.getName();
        }
        return "math." + this.function.getName() + "(" + Arrays.stream(this.arguments).map(Node::toString).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public boolean isConstant() {
        if (!this.function.isDeterministic()) {
            return false;
        }

        for (Node parameter : this.arguments) {
            if (!parameter.isConstant()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public float evaluate(MolangBytecodeEnvironment environment) throws MolangException {
        float[] values = new float[this.arguments.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = this.arguments[i].evaluate(environment);
        }

        switch (this.function) {
            case ABS:
                return Math.abs(values[0]);
            case ACOS:
                return RADIANS_TO_DEGREES * (float) Math.acos(values[0]);
            case ASIN:
                return RADIANS_TO_DEGREES * (float) Math.asin(values[0]);
            case ATAN:
                return RADIANS_TO_DEGREES * (float) Math.atan(values[0]);
            case ATAN2:
                return RADIANS_TO_DEGREES * (float) Math.atan2(values[0], values[1]);
            case CEIL:
                return (float) Math.ceil(values[0]);
            case CLAMP:
                return MolangUtil.clamp(values[0], values[1], values[2]);
            case COS:
                return (float) Math.cos(DEGREES_TO_RADIANS * values[0]);
            case SIN:
                return (float) Math.sin(DEGREES_TO_RADIANS * values[0]);
            case EXP:
                return (float) Math.exp(values[0]);
            case FLOOR:
                return (float) Math.floor(values[0]);
            case HERMITE_BLEND:
                return MolangUtil.hermiteBlend(values[0]);
            case LERP:
                return MolangUtil.lerp(values[0], values[1], values[2]);
            case LERPROTATE:
                return MolangUtil.lerpRotate(values[0], values[1], values[2]);
            case LN:
                return (float) Math.log(values[0]);
            case MAX:
                return Math.max(values[0], values[1]);
            case MIN:
                return Math.min(values[0], values[1]);
            case MIN_ANGLE:
                return MolangUtil.wrapDegrees(values[0]);
            case MOD:
                return values[0] % values[1];
            case PI:
                return (float) Math.PI;
            case POW:
                return (float) Math.pow(values[0], values[1]);
            case ROUND:
                return Math.round(values[0]);
            case SQRT:
                return (float) Math.sqrt(values[0]);
            case TRUNC:
                return (int) values[0];
            case SIGN:
                return Math.signum(values[0]);
            case TRIANGLE_WAVE:
                return MolangUtil.triangleWave(values[0], values[1]);
            default:
                throw new MolangException("Unexpected value: " + this.function);
        }
    }

    @Override
    public void writeBytecode(MethodNode method, MolangBytecodeEnvironment env, @Nullable Label breakLabel, @Nullable Label continueLabel) throws MolangException {
        switch (this.function) {
            // Single-argument Float
            case ABS: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(F)F", false);
                break;
            }
            // Double-argument Float
            case MAX:
            case MIN: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(FF)F", false);
                break;
            }
            // Convert to degrees
            case ACOS:
            case ASIN:
            case ATAN: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2D);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(D)D", false);
                method.visitInsn(Opcodes.D2F);
                BytecodeCompiler.writeFloatConst(method, RADIANS_TO_DEGREES);
                method.visitInsn(Opcodes.FMUL);
                break;
            }
            // Single-argument Double
            case CEIL:
            case EXP:
            case FLOOR:
            case LN:
            case SQRT: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2D);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(D)D", false);
                method.visitInsn(Opcodes.D2F);
                break;
            }
            // Convert to radians
            case COS:
            case SIN: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                BytecodeCompiler.writeFloatConst(method, DEGREES_TO_RADIANS);
                method.visitInsn(Opcodes.FMUL);
                method.visitInsn(Opcodes.F2D);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(D)D", false);
                method.visitInsn(Opcodes.D2F);
                break;
            }
            // Double-argument Double
            case POW: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2D);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2D);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(DD)D", false);
                method.visitInsn(Opcodes.D2F);
                break;
            }
            // Convert to degrees
            case ATAN2: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2D);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2D);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(DD)D", false);
                method.visitInsn(Opcodes.D2F);
                BytecodeCompiler.writeFloatConst(method, RADIANS_TO_DEGREES);
                method.visitInsn(Opcodes.FMUL);
                break;
            }
            // Single-argument Float->Int
            case ROUND: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", this.function.getName(), "(F)I", false);
                method.visitInsn(Opcodes.I2F);
                break;
            }
            // Operations
            case MOD: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.FREM);
                break;
            }
            case PI: {
                BytecodeCompiler.writeFloatConst(method, (float) Math.PI);
                break;
            }
            case TRUNC: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2I);
                method.visitInsn(Opcodes.I2F);
                break;
            }
            // Custom
            case CLAMP: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[2].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "clamp", "(FFF)F", false);
                break;
            }
            case DIE_ROLL: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2I);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[2].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "dieRoll", "(IFF)F", false);
                break;
            }
            case DIE_ROLL_INTEGER: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2I);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2I);
                method.visitInsn(Opcodes.I2F);
                this.arguments[2].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2I);
                method.visitInsn(Opcodes.I2F);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "dieRoll", "(IFF)F", false);
                method.visitInsn(Opcodes.F2I);
                method.visitInsn(Opcodes.I2F);
                break;
            }
            case HERMITE_BLEND: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "hermiteBlend", "(F)F", false);
                break;
            }
            case LERP: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[2].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "lerp", "(FFF)F", false);
                break;
            }
            case LERPROTATE: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[2].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "lerpRotate", "(FFF)F", false);
                break;
            }
            case MIN_ANGLE: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "wrapDegrees", "(F)F", false);
                break;
            }
            case RANDOM: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "random", "(FF)F", false);
                break;
            }
            case RANDOM_INTEGER: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2I);
                method.visitInsn(Opcodes.I2F);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitInsn(Opcodes.F2I);
                method.visitInsn(Opcodes.I2F);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "random", "(FF)F", false);
                method.visitInsn(Opcodes.F2I);
                method.visitInsn(Opcodes.I2F);
                break;
            }
            case SIGN: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "signum", "(F)F", false);
                break;
            }
            case TRIANGLE_WAVE: {
                this.arguments[0].writeBytecode(method, env, breakLabel, continueLabel);
                this.arguments[1].writeBytecode(method, env, breakLabel, continueLabel);
                method.visitMethodInsn(Opcodes.INVOKESTATIC, "gg/moonflower/molangcompiler/core/MolangUtil", "triangleWave", "(FF)F", false);
                break;
            }
        }
    }
}