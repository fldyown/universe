package com.fldy;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 容器
 */
public class Universe {
    private static Class config;
    private final static Universe instance = new Universe();
    private static Map<String, ObjectDefinition> ods = new HashMap<>();
    private static Map<String, Object> singletons = new HashMap<>();
    private static List<UniverseProcessor> processors = new ArrayList<>();

    private Universe() {
    }

    /**
     * 初始化
     *
     * @param config
     */
    public static void init(Class config) {
        instance.config = config;
        instance.scan();
        instance.create();
    }

    private void scan() {
        ComponentScan cs = (ComponentScan) config.getDeclaredAnnotation(ComponentScan.class);
        ClassLoader cl = Universe.class.getClassLoader();
        String[] pkgs = cs.value();
        for (String pkg : pkgs) {
            pkg = pkg.replace(".", File.separator);
            URL url = cl.getResource(pkg);
            if (null != url) {
                File[] files = new File(url.getFile()).listFiles();
                files(files, pkg, cl);
            }
        }
    }

    private void files(File[] files, String pkg, ClassLoader cl) {
        for (File file : files) {
            if (file.isDirectory()) {
                files(file.listFiles(), pkg, cl);
            } else {
                String path = file.getAbsolutePath();
                if (path.endsWith(".class") && path.indexOf(pkg) > 0) {
                    String cn = path.substring(path.indexOf(pkg)).replace(File.separator, ".").replace(".class", "");
                    try {
                        Class<?> clazz = cl.loadClass(cn);
                        if (clazz.isAnnotationPresent(Component.class)) {
                            if (UniverseProcessor.class.isAssignableFrom(clazz)) {
                                UniverseProcessor up = (UniverseProcessor) clazz.getDeclaredConstructor().newInstance();
                                processors.add(up);
                            }
                            Component component = clazz.getDeclaredAnnotation(Component.class);
                            String name = component.value();
                            if (null == name || "".equals(name)) {
                                name = lowerFirstCase(clazz.getSimpleName());
                            }
                            ObjectDefinition od = new ObjectDefinition();
                            od.clazz = clazz;
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scope = clazz.getDeclaredAnnotation(Scope.class);
                                od.scope = scope.value();
                            } else {
                                od.scope = "singleton";
                            }
                            ods.put(name, od);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void create() {
        for (Map.Entry<String, ObjectDefinition> entry : ods.entrySet()) {
            String name = entry.getKey();
            if ("singleton".equals(entry.getValue().scope)) {
                Object o = create(name, entry.getValue());
                singletons.put(name, o);
            }
        }
    }

    private Object create(String name, ObjectDefinition od) {
        try {
            Object o = od.clazz.getDeclaredConstructor().newInstance();

            for (UniverseProcessor up : processors) {
                up.before(name, o);
            }

            for (Field field : od.clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object bean = get(field.getName());
                    field.setAccessible(true);
                    field.set(o, bean);
                }
            }

            for (UniverseProcessor up : processors) {
                up.after(name, o);
            }

            return o;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过名称获取服务
     *
     * @param name
     * @return
     */
    public static Object get(String name) {
        if (ods.containsKey(name)) {
            ObjectDefinition od = ods.get(name);
            if ("singleton".equals(od.scope)) {
                return singletons.get(name);
            } else {
                return instance.create(name, od);
            }
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * 通过Class获取服务
     *
     * @param clazz
     * @param <O>
     * @return
     */
    public static <O> O get(Class<O> clazz) {
        return (O) get(instance.lowerFirstCase(clazz.getSimpleName()));
    }
}
