/*  ByteObf: A Java Bytecode Obfuscator
 *  Copyright (C) 2021 vimasig
 *  Copyright (C) 2025 Mohammad Ali Solhjoo mohammadalisolhjoo@live.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package codes.rayacode.ByteObf.obfuscator.utils;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ASMUtils implements Opcodes {

    private ASMUtils() { }

    
    public static final int CLASS_METHOD_COUNT_THRESHOLD = 400;
    public static final int CLASS_INSTRUCTION_COUNT_THRESHOLD = 100_000;

    /**
     * **NEW**: Checks if a class is too large or complex for heavy transformations.
     * This is the main proactive guard against ClassTooLargeException.
     * @param classNode The class to check.
     * @return True if the class exceeds complexity thresholds, false otherwise.
     */
    public static boolean isClassTooComplex(ClassNode classNode) {
        if (classNode.methods.size() > CLASS_METHOD_COUNT_THRESHOLD) {
            return true;
        }
        long totalInstructions = classNode.methods.stream()
                .filter(m -> m.instructions != null)
                .mapToLong(m -> m.instructions.size())
                .sum();
        return totalInstructions > CLASS_INSTRUCTION_COUNT_THRESHOLD;
    }

    public static class BuiltInstructions {
        public static InsnList getPrintln(String s) {
            final InsnList insnList = new InsnList();
            insnList.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
            insnList.add(new LdcInsnNode(s));
            insnList.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
            return insnList;
        }

        public static InsnList getThrowNull() {
            final InsnList insnList = new InsnList();
            insnList.add(new InsnNode(ACONST_NULL));
            insnList.add(new InsnNode(ATHROW));
            return insnList;
        }
    }

    public static InsnList getCastConvertInsnList(Type type) {
        final InsnList insnList = new InsnList();

        if(type.getDescriptor().equals("V")) {
            insnList.add(new InsnNode(POP));
            return insnList;
        }

        String methodName = switch (type.getDescriptor()) {
            case "I" -> "intValue";
            case "Z" -> "booleanValue";
            case "B" -> "byteValue";
            case "C" -> "charValue";
            case "S" -> "shortValue";
            case "D" -> "doubleValue";
            case "F" -> "floatValue";
            case "J" -> "longValue";
            default -> null;
        };
        if(methodName != null) insnList.add(getCastConvertInsnList(type, getPrimitiveClassType(type), methodName));
        else insnList.add(new TypeInsnNode(CHECKCAST, type.getInternalName()));
        return insnList;
    }

    private static InsnList getCastConvertInsnList(Type type, Type classType, String convertMethodName) {
        return InsnBuilder.createEmpty()
                .insn(new TypeInsnNode(CHECKCAST, classType.getInternalName()))
                .insn(new MethodInsnNode(INVOKEVIRTUAL, classType.getInternalName(), convertMethodName, "()" + type.getDescriptor()))
                .getInsnList();
    }

    private static final Map<String, String> primitives = Map.of(
            "V", "java/lang/Void",
            "I", "java/lang/Integer",
            "Z",  "java/lang/Boolean",
            "B",  "java/lang/Byte",
            "C",  "java/lang/Character",
            "S",  "java/lang/Short",
            "D",  "java/lang/Double",
            "F",  "java/lang/Float",
            "J",  "java/lang/Long"
    );
    public static Type getPrimitiveClassType(Type type) {
        if(!primitives.containsKey(type.getDescriptor()))
            throw new IllegalArgumentException(type + " is not a primitive type");
        return Type.getType("L" + primitives.get(type.getDescriptor()) + ";");
    }

    public static Type getPrimitiveFromClassType(Type type) throws IllegalArgumentException {
        return primitives.entrySet().stream()
                .filter(entry -> entry.getValue().equals(type.getInternalName()))
                .map(Map.Entry::getKey)
                .map(Type::getType)
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public static List<Type> getMethodArguments(String desc) {
        String args = desc.substring(1, desc.indexOf(")"));

        List<Type> typeStrings = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean isClass = false, isArray = false;
        for (char c : args.toCharArray()) {
            if(c == '[') {
                isArray = true;
                continue;
            }

            if(c == 'L') isClass = true;
            if(!isClass) {
                Type type = getPrimitiveClassType(Type.getType(String.valueOf(c)));
                if(isArray) type = Type.getType("[" + type);
                typeStrings.add(type);
                isArray = false;
            }
            else {
                sb.append(c);
                if(c == ';') {
                    typeStrings.add(Type.getType((isArray ? "[" : "") + sb));
                    sb = new StringBuilder();

                    isClass = false;
                    isArray = false;
                }
            }
        }
        return typeStrings;
    }

    public static boolean isClassEligibleToModify(ClassNode classNode) {
        return (classNode.access & ACC_INTERFACE) == 0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isMethodEligibleToModify(ClassNode classNode, MethodNode methodNode) {
        return isClassEligibleToModify(classNode) && (methodNode.access & ACC_ABSTRACT) == 0;
    }

    public static byte[] toByteArrayDefault(ClassNode classNode) {
        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static String getName(ClassNode classNode) {
        return classNode.name.replace("/", ".");
    }

    public static String getName(ClassNode classNode, FieldNode fieldNode) {
        return classNode.name + "." + fieldNode.name;
    }

    public static String getName(ClassNode classNode, MethodNode methodNode) {
        return classNode.name + "." + methodNode.name + methodNode.desc;
    }

    public static InsnList arrayToList(AbstractInsnNode[] insns) {
        final InsnList insnList = new InsnList();
        Arrays.stream(insns).forEach(insnList::add);
        return insnList;
    }

    public static boolean isMethodSizeValid(MethodNode methodNode) {
        return getCodeSize(methodNode) <= 65536;
    }

    public static int getCodeSize(MethodNode methodNode) {
        CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
        methodNode.accept(cse);
        return cse.getMaxSize();
    }

    public static MethodNode findOrCreateInit(ClassNode classNode) {
        MethodNode clinit = findMethod(classNode, "<init>", "()V");
        if (clinit == null) {
            clinit = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
            clinit.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(clinit);
        } return clinit;
    }

    public static MethodNode findOrCreateClinit(ClassNode classNode) {
        MethodNode clinit = findMethod(classNode, "<clinit>", "()V");
        if (clinit == null) {
            clinit = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(new InsnNode(RETURN));
            classNode.methods.add(clinit);
        } return clinit;
    }

    public static MethodNode findMethod(ClassNode classNode, String name, String desc) {
        return classNode.methods
                .stream()
                .filter(methodNode -> name.equals(methodNode.name) && desc.equals(methodNode.desc))
                .findAny()
                .orElse(null);
    }

    public static boolean isInvokeMethod(AbstractInsnNode insn, boolean includeInvokeDynamic) {
        return insn.getOpcode() >= INVOKEVIRTUAL && (includeInvokeDynamic ? insn.getOpcode() <= INVOKEDYNAMIC : insn.getOpcode() < INVOKEDYNAMIC);
    }

    public static boolean isFieldInsn(AbstractInsnNode insn) {
        return insn.getOpcode() >= GETSTATIC && insn.getOpcode() <= PUTFIELD;
    }

    public static boolean isIf(AbstractInsnNode insn) {
        int op = insn.getOpcode();
        return (op >= IFEQ && op <= IF_ACMPNE) || op == IFNULL || op == IFNONNULL;
    }

    public static AbstractInsnNode pushLong(long value) {
        if (value == 0) return new InsnNode(LCONST_0);
        else if (value == 1) return new InsnNode(LCONST_1);
        else return new LdcInsnNode(value);
    }

    public static boolean isPushLong(AbstractInsnNode insn) {
        try {
            getPushedLong(insn);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static long getPushedLong(AbstractInsnNode insn) throws IllegalArgumentException {
        var ex = new IllegalArgumentException("Insn is not a push long instruction");
        return switch (insn.getOpcode()) {
            case LCONST_0 -> 0;
            case LCONST_1 -> 1;
            case LDC -> {
                Object cst = ((LdcInsnNode)insn).cst;
                if (cst instanceof Long)
                    yield (long) cst;
                throw ex;
            }
            default -> throw ex;
        };
    }

    public static AbstractInsnNode pushInt(int value) {
        if (value >= -1 && value <= 5) {
            return new InsnNode(ICONST_0 + value);
        }
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return new IntInsnNode(BIPUSH, value);
        }
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return new IntInsnNode(SIPUSH, value);
        }
        return new LdcInsnNode(value);
    }

    public static boolean isPushInt(AbstractInsnNode insn) {
        try {
            getPushedInt(insn);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static int getPushedInt(AbstractInsnNode insn) throws IllegalArgumentException {
        var ex = new IllegalArgumentException("Insn is not a push int instruction");
        int op = insn.getOpcode();
        return switch (op) {
            case ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5 -> op - ICONST_0;
            case BIPUSH, SIPUSH -> ((IntInsnNode)insn).operand;
            case LDC -> {
                Object cst = ((LdcInsnNode)insn).cst;
                if (cst instanceof Integer)
                    yield  (int) cst;
                throw ex;
            }
            default -> throw ex;
        };
    }
}