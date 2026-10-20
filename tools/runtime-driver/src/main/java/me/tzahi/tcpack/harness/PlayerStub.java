package me.tzahi.tcpack.harness;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Test-only descriptor-preserving stub by Trenly; CC BY-SA 3.0 Unported.
 * Legacy Bukkit exposes both int and double getHealth bridges. java.lang.Proxy
 * rejects these although the JVM allows distinct return descriptors. Generate
 * straight-line delegates without a bundled bytecode library or server internals.
 */
public final class PlayerStub {
    private static final String NAME = "me/tzahi/tcpack/harness/GeneratedPlayer";
    private static final List<Method> METHODS = new ArrayList<Method>();
    private static final Class<?> TYPE = generate();

    private PlayerStub() { }

    public static CommandSender create(InvocationHandler handler) {
        try {
            return (CommandSender) TYPE.getConstructor(InvocationHandler.class).newInstance(handler);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot create descriptor-preserving Player stub", failure);
        }
    }

    public static Object dispatch(InvocationHandler handler, Object receiver, int method, Object[] args)
            throws Throwable {
        return handler.invoke(receiver, METHODS.get(method), args);
    }

    private static Class<?> generate() {
        try {
            Pool pool = new Pool();
            int self = pool.type(NAME);
            int parent = pool.type("java/lang/Object");
            int player = pool.type("org/bukkit/entity/Player");
            int fieldName = pool.text("handler");
            int fieldType = pool.text("Ljava/lang/reflect/InvocationHandler;");
            int field = pool.member(9, NAME, "handler", "Ljava/lang/reflect/InvocationHandler;");
            int objectInit = pool.member(10, "java/lang/Object", "<init>", "()V");
            int dispatch = pool.member(10, "me/tzahi/tcpack/harness/PlayerStub", "dispatch",
                    "(Ljava/lang/reflect/InvocationHandler;Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;");
            int codeName = pool.text("Code");
            List<byte[]> methods = new ArrayList<byte[]>();
            ByteArrayOutputStream init = new ByteArrayOutputStream();
            DataOutputStream constructor = new DataOutputStream(init);
            constructor.writeByte(0x2a); // aload_0
            constructor.writeByte(0xb7); constructor.writeShort(objectInit);
            constructor.writeByte(0x2a); constructor.writeByte(0x2b);
            constructor.writeByte(0xb5); constructor.writeShort(field);
            constructor.writeByte(0xb1);
            methods.add(method(pool, codeName, "<init>", "(Ljava/lang/reflect/InvocationHandler;)V",
                    init.toByteArray(), 2));
            Map<String, Method> unique = new LinkedHashMap<String, Method>();
            for (Method method : Player.class.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    unique.put(method.getName() + descriptor(method), method);
                }
            }
            for (Method method : unique.values()) {
                int index = METHODS.size();
                METHODS.add(method);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream code = new DataOutputStream(bytes);
                code.writeByte(0x2a);
                code.writeByte(0xb4); code.writeShort(field);
                code.writeByte(0x2a);
                number(code, index);
                Class<?>[] parameters = method.getParameterTypes();
                number(code, parameters.length);
                code.writeByte(0xbd); code.writeShort(parent);
                int local = 1;
                for (int i = 0; i < parameters.length; i++) {
                    Class<?> type = parameters[i];
                    code.writeByte(0x59); // dup
                    number(code, i);
                    code.writeByte(type == long.class ? 0x16 : type == float.class ? 0x17
                            : type == double.class ? 0x18 : type.isPrimitive() ? 0x15 : 0x19);
                    code.writeByte(local);
                    if (type.isPrimitive()) {
                        code.writeByte(0xb8);
                        code.writeShort(pool.member(10, wrapper(type), "valueOf",
                                "(" + descriptor(type) + ")L" + wrapper(type) + ";"));
                    }
                    code.writeByte(0x53); // aastore
                    local += type == long.class || type == double.class ? 2 : 1;
                }
                code.writeByte(0xb8); code.writeShort(dispatch);
                Class<?> result = method.getReturnType();
                if (result == void.class) {
                    code.writeByte(0x57); code.writeByte(0xb1);
                } else {
                    code.writeByte(0xc0);
                    code.writeShort(pool.type(result.isPrimitive() ? wrapper(result) : className(result)));
                    if (result.isPrimitive()) {
                        code.writeByte(0xb6);
                        code.writeShort(pool.member(10, wrapper(result), result.getName() + "Value",
                                "()" + descriptor(result)));
                    }
                    code.writeByte(result == long.class ? 0xad : result == float.class ? 0xae
                            : result == double.class ? 0xaf : result.isPrimitive() ? 0xac : 0xb0);
                }
                methods.add(method(pool, codeName, method.getName(), descriptor(method), bytes.toByteArray(), local));
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(0xcafebabe); out.writeShort(0); out.writeShort(49);
            pool.write(out);
            out.writeShort(0x31); out.writeShort(self); out.writeShort(parent);
            out.writeShort(1); out.writeShort(player);
            out.writeShort(1);
            out.writeShort(0x12); out.writeShort(fieldName); out.writeShort(fieldType); out.writeShort(0);
            out.writeShort(methods.size());
            for (byte[] method : methods) { out.write(method); }
            out.writeShort(0);
            final byte[] definition = bytes.toByteArray();
            return new ClassLoader(PlayerStub.class.getClassLoader()) {
                Class<?> define() { return defineClass(NAME.replace('/', '.'), definition, 0, definition.length); }
            }.define();
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot generate test Player stub", failure);
        }
    }

    private static byte[] method(Pool pool, int codeName, String name, String descriptor, byte[] code, int locals)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeShort(0x01); out.writeShort(pool.text(name)); out.writeShort(pool.text(descriptor));
        out.writeShort(1); out.writeShort(codeName); out.writeInt(code.length + 12);
        out.writeShort(16); out.writeShort(locals); out.writeInt(code.length); out.write(code);
        out.writeShort(0); out.writeShort(0);
        return bytes.toByteArray();
    }

    private static void number(DataOutputStream code, int value) throws IOException {
        code.writeByte(0x11); code.writeShort(value); // sipush
    }

    private static String className(Class<?> type) { return type.getName().replace('.', '/'); }

    private static String descriptor(Class<?> type) {
        if (!type.isPrimitive()) { return type.isArray() ? className(type) : "L" + className(type) + ";"; }
        if (type == void.class) { return "V"; }
        if (type == boolean.class) { return "Z"; }
        if (type == byte.class) { return "B"; }
        if (type == char.class) { return "C"; }
        if (type == short.class) { return "S"; }
        if (type == int.class) { return "I"; }
        if (type == long.class) { return "J"; }
        if (type == float.class) { return "F"; }
        return "D";
    }

    private static String descriptor(Method method) {
        StringBuilder text = new StringBuilder("(");
        for (Class<?> type : method.getParameterTypes()) { text.append(descriptor(type)); }
        return text.append(')').append(descriptor(method.getReturnType())).toString();
    }

    private static String wrapper(Class<?> type) {
        if (type == char.class) { return "java/lang/Character"; }
        if (type == int.class) { return "java/lang/Integer"; }
        String name = type.getName();
        return "java/lang/" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static final class Pool {
        private final List<byte[]> entries = new ArrayList<byte[]>();

        private int text(String value) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(1); out.writeUTF(value);
            entries.add(bytes.toByteArray());
            return entries.size();
        }

        private int type(String name) throws IOException { return entry(7, text(name)); }

        private int member(int tag, String owner, String name, String descriptor) throws IOException {
            return entry(tag, type(owner), entry(12, text(name), text(descriptor)));
        }

        private int entry(int tag, int... values) throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(tag);
            for (int value : values) { out.writeShort(value); }
            entries.add(bytes.toByteArray());
            return entries.size();
        }

        private void write(DataOutputStream out) throws IOException {
            out.writeShort(entries.size() + 1);
            for (byte[] entry : entries) { out.write(entry); }
        }
    }
}
