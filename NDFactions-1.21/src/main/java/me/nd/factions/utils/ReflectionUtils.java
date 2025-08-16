package me.nd.factions.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

public class ReflectionUtils {
	public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes)
			throws NoSuchMethodException {
		Class[] primitiveTypes = DataType.getPrimitive(parameterTypes);
		Constructor<?>[] constructors;
		for (int length = (constructors = clazz.getConstructors()).length, i = 0; i < length; ++i) {
			Constructor<?> constructor = constructors[i];
			if (DataType.compare(DataType.getPrimitive(constructor.getParameterTypes()), primitiveTypes)) {
				return constructor;
			}
		}
		throw new NoSuchMethodException(
				"There is no such constructor in this class with the specified parameter types");
	}

	public static Constructor<?> getConstructor(String className, PackageType packageType,
			Class<?>... parameterTypes) throws NoSuchMethodException, ClassNotFoundException {
		return getConstructor(packageType.getClass(className), parameterTypes);
	}

	public static Object instantiateObject(Class<?> clazz, Object... arguments)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException {
		return getConstructor(clazz, DataType.getPrimitive(arguments)).newInstance(arguments);
	}

	public static Object instantiateObject(String className, PackageType packageType,
			Object... arguments) throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		return instantiateObject(packageType.getClass(className), arguments);
	}

	public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes)
			throws NoSuchMethodException {
		Class[] primitiveTypes = DataType.getPrimitive(parameterTypes);
		Method[] methods;
		for (int length = (methods = clazz.getMethods()).length, i = 0; i < length; ++i) {
			Method method = methods[i];
			if (method.getName().equals(methodName)
					&& DataType.compare(DataType.getPrimitive(method.getParameterTypes()), primitiveTypes)) {
				return method;
			}
		}
		throw new NoSuchMethodException(
				"There is no such method in this class with the specified name and parameter types");
	}

	public static Method getMethod(String className, PackageType packageType, String methodName,
			Class<?>... parameterTypes) throws NoSuchMethodException, ClassNotFoundException {
		return getMethod(packageType.getClass(className), methodName, parameterTypes);
	}

	public static Object invokeMethod(Object instance, String methodName, Object... arguments)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		return getMethod(instance.getClass(), methodName, DataType.getPrimitive(arguments)).invoke(instance, arguments);
	}

	public static Object invokeMethod(Object instance, Class<?> clazz, String methodName,
			Object... arguments)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException {
		return getMethod(clazz, methodName, DataType.getPrimitive(arguments)).invoke(instance, arguments);
	}

	public static Object invokeMethod(Object instance, String className, PackageType packageType,
			String methodName, Object... arguments) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
		return invokeMethod(instance, packageType.getClass(className), methodName, arguments);
	}

	public static Field getField(Class<?> clazz, boolean declared, String fieldName)
			throws NoSuchFieldException, SecurityException {
		Field field = declared ? clazz.getDeclaredField(fieldName) : clazz.getField(fieldName);
		field.setAccessible(true);
		return field;
	}

	public static Field getField(String className, PackageType packageType, boolean declared,
			String fieldName) throws NoSuchFieldException, SecurityException, ClassNotFoundException {
		return getField(packageType.getClass(className), declared, fieldName);
	}

	public static Object getValue(Object instance, Class<?> clazz, boolean declared,
			String fieldName)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		return getField(clazz, declared, fieldName).get(instance);
	}

	public static Object getValue(Object instance, String className, PackageType packageType,
			boolean declared, String fieldName) throws IllegalArgumentException, IllegalAccessException,
			NoSuchFieldException, SecurityException, ClassNotFoundException {
		return getValue(instance, packageType.getClass(className), declared, fieldName);
	}

	public static Object getValue(Object instance, boolean declared, String fieldName)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		return getValue(instance, instance.getClass(), declared, fieldName);
	}

	public static void setValue(Object instance, Class<?> clazz, boolean declared,
			String fieldName, Object value)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		getField(clazz, declared, fieldName).set(instance, value);
	}

	public static void setValue(Object instance, String className, PackageType packageType,
			boolean declared, String fieldName, Object value) throws IllegalArgumentException,
			IllegalAccessException, NoSuchFieldException, SecurityException, ClassNotFoundException {
		setValue(instance, packageType.getClass(className), declared, fieldName, value);
	}

	public static void setValue(Object instance, boolean declared, String fieldName,
			Object value)
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		setValue(instance, instance.getClass(), declared, fieldName, value);
	}

	public enum DataType {
		BYTE((Class<?>) Byte.TYPE, (Class<?>) Byte.class), SHORT((Class<?>) Short.TYPE,
				(Class<?>) Short.class), INTEGER((Class<?>) Integer.TYPE, (Class<?>) Integer.class), LONG(
						(Class<?>) Long.TYPE,
						(Class<?>) Long.class), CHARACTER((Class<?>) Character.TYPE, (Class<?>) Character.class), FLOAT(
								(Class<?>) Float.TYPE, (Class<?>) Float.class), DOUBLE((Class<?>) Double.TYPE,
										(Class<?>) Double.class), BOOLEAN((Class<?>) Boolean.TYPE,
												(Class<?>) Boolean.class);

		private static Map<Class<?>, DataType> CLASS_MAP;
		private Class<?> primitive;
		private Class<?> reference;

		static {
			CLASS_MAP = new HashMap<Class<?>, DataType>();
			DataType[] values;
			for (int length = (values = values()).length, i = 0; i < length; ++i) {
				DataType type = values[i];
				DataType.CLASS_MAP.put(type.primitive, type);
				DataType.CLASS_MAP.put(type.reference, type);
			}
		}

		private DataType(Class<?> primitive, Class<?> reference) {
			this.primitive = primitive;
			this.reference = reference;
		}

		public Class<?> getPrimitive() {
			return this.primitive;
		}

		public Class<?> getReference() {
			return this.reference;
		}

		public static DataType fromClass(Class<?> clazz) {
			return DataType.CLASS_MAP.get(clazz);
		}

		public static Class<?> getPrimitive(Class<?> clazz) {
			DataType type = fromClass(clazz);
			return (type == null) ? clazz : type.getPrimitive();
		}

		public static Class<?> getReference(Class<?> clazz) {
			DataType type = fromClass(clazz);
			return (type == null) ? clazz : type.getReference();
		}

		public static Class<?>[] getPrimitive(Class<?>[] classes) {
			int length = (classes == null) ? 0 : classes.length;
			Class[] types = new Class[length];
			for (int index = 0; index < length; ++index) {
				types[index] = getPrimitive(classes[index]);
			}
			return (Class<?>[]) types;
		}

		public static Class<?>[] getReference(Class<?>[] classes) {
			int length = (classes == null) ? 0 : classes.length;
			Class[] types = new Class[length];
			for (int index = 0; index < length; ++index) {
				types[index] = getReference(classes[index]);
			}
			return (Class<?>[]) types;
		}

		public static Class<?>[] getPrimitive(Object[] objects) {
			int length = (objects == null) ? 0 : objects.length;
			Class[] types = new Class[length];
			for (int index = 0; index < length; ++index) {
				types[index] = getPrimitive(objects[index].getClass());
			}
			return (Class<?>[]) types;
		}

		public static Class<?>[] getReference(Object[] objects) {
			int length = (objects == null) ? 0 : objects.length;
			Class[] types = new Class[length];
			for (int index = 0; index < length; ++index) {
				types[index] = getReference(objects[index].getClass());
			}
			return (Class<?>[]) types;
		}

		public static boolean compare(Class<?>[] primary, Class<?>[] secondary) {
			if (primary == null || secondary == null || primary.length != secondary.length) {
				return false;
			}
			for (int index = 0; index < primary.length; ++index) {
				Class<?> primaryClass = primary[index];
				Class<?> secondaryClass = secondary[index];
				if (!primaryClass.equals(secondaryClass) && !primaryClass.isAssignableFrom(secondaryClass)) {
					return false;
				}
			}
			return true;
		}
	}

	public enum PackageType {
		MINECRAFT_SERVER("MINECRAFT_SERVER", 0, "net.minecraft"),
		CRAFTBUKKIT("CRAFTBUKKIT", 1, "org.bukkit.craftbukkit"),
		CRAFTBUKKIT_BLOCK("CRAFTBUKKIT_BLOCK", 2, PackageType.CRAFTBUKKIT, "block"),
		CRAFTBUKKIT_CHUNKIO("CRAFTBUKKIT_CHUNKIO", 3, PackageType.CRAFTBUKKIT, "chunkio"),
		CRAFTBUKKIT_COMMAND("CRAFTBUKKIT_COMMAND", 4, PackageType.CRAFTBUKKIT, "command"),
		CRAFTBUKKIT_CONVERSATIONS("CRAFTBUKKIT_CONVERSATIONS", 5, PackageType.CRAFTBUKKIT, "conversations"),
		CRAFTBUKKIT_ENCHANTMENS("CRAFTBUKKIT_ENCHANTMENS", 6, PackageType.CRAFTBUKKIT, "enchantments"),
		CRAFTBUKKIT_ENTITY("CRAFTBUKKIT_ENTITY", 7, PackageType.CRAFTBUKKIT, "entity"),
		CRAFTBUKKIT_EVENT("CRAFTBUKKIT_EVENT", 8, PackageType.CRAFTBUKKIT, "event"),
		CRAFTBUKKIT_GENERATOR("CRAFTBUKKIT_GENERATOR", 9, PackageType.CRAFTBUKKIT, "generator"),
		CRAFTBUKKIT_HELP("CRAFTBUKKIT_HELP", 10, PackageType.CRAFTBUKKIT, "help"),
		CRAFTBUKKIT_INVENTORY("CRAFTBUKKIT_INVENTORY", 11, PackageType.CRAFTBUKKIT, "inventory"),
		CRAFTBUKKIT_MAP("CRAFTBUKKIT_MAP", 12, PackageType.CRAFTBUKKIT, "map"),
		CRAFTBUKKIT_METADATA("CRAFTBUKKIT_METADATA", 13, PackageType.CRAFTBUKKIT, "metadata"),
		CRAFTBUKKIT_POTION("CRAFTBUKKIT_POTION", 14, PackageType.CRAFTBUKKIT, "potion"),
		CRAFTBUKKIT_PROJECTILES("CRAFTBUKKIT_PROJECTILES", 15, PackageType.CRAFTBUKKIT, "projectiles"),
		CRAFTBUKKIT_SCHEDULER("CRAFTBUKKIT_SCHEDULER", 16, PackageType.CRAFTBUKKIT, "scheduler"),
		CRAFTBUKKIT_SCOREBOARD("CRAFTBUKKIT_SCOREBOARD", 17, PackageType.CRAFTBUKKIT, "scoreboard"),
		CRAFTBUKKIT_UPDATER("CRAFTBUKKIT_UPDATER", 18, PackageType.CRAFTBUKKIT, "updater"),
		CRAFTBUKKIT_UTIL("CRAFTBUKKIT_UTIL", 19, PackageType.CRAFTBUKKIT, "util");

		private String path;

		private PackageType(String s, int n, String path) {
			this.path = path;
		}

		private PackageType(String s, int n, PackageType parent, String path) {
			this(s, n, parent + "." + path);
		}

		public String getPath() {
			return this.path;
		}

		public Class<?> getClass(String className) throws ClassNotFoundException {
			return Class.forName(this + "." + className);
		}

		@Override
		public String toString() {
			return this.path;
		}

		public static String getServerVersion() {
			String packageName = Bukkit.getServer().getClass().getPackage().getName();
			// Paper 1.21 uses "org.bukkit.craftbukkit" without version suffix
			if (packageName.equals("org.bukkit.craftbukkit")) {
				return ""; // Return empty string for Paper 1.21
			}
			// For older versions (e.g., 1.8: "org.bukkit.craftbukkit.v1_8_R3")
			return packageName.startsWith("org.bukkit.craftbukkit.") ? packageName.substring(23) : "";
		}
	}
}
