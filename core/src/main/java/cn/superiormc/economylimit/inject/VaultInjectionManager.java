package cn.superiormc.economylimit.inject;

import cn.superiormc.economylimit.EconomyLimitPlugin;
import cn.superiormc.economylimit.utils.TextUtil;
import javassist.ClassPool;
import javassist.ClassClassPath;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.bytecode.MethodInfo;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VaultInjectionManager implements Listener {

    private final EconomyLimitPlugin plugin;
    private final Set<String> instrumentedClasses = ConcurrentHashMap.newKeySet();
    private Instrumentation instrumentation;
    private volatile String lastInjectionStatus = "none";

    public VaultInjectionManager(EconomyLimitPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean start() {
        try {
            instrumentation = ByteBuddyAgent.install();
        } catch (Throwable throwable) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cUnable to install bytecode agent: " + throwable.getMessage());
            throwable.printStackTrace();
            return false;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        hookCurrentProvider();
        return true;
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (event.getProvider().getService() == Economy.class) {
            hookCurrentProvider();
        }
    }

    private void hookCurrentProvider() {
        RegisteredServiceProvider<Economy> registration = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cVault is loaded, but no Economy provider is currently registered.");
            return;
        }

        Class<?> providerClass = registration.getProvider().getClass();
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cDetected Vault economy provider: " + providerClass.getName());

        Class<?> currentClass = providerClass;
        while (currentClass != null && currentClass != Object.class) {
            if (shouldSkipClass(currentClass)) {
                currentClass = currentClass.getSuperclass();
                continue;
            }
            if (declaresDepositMethod(currentClass) && instrumentedClasses.add(currentClass.getName())) {
                instrument(currentClass);
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    private boolean declaresDepositMethod(Class<?> type) {
        return java.util.Arrays.stream(type.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("depositPlayer"));
    }

    private boolean shouldSkipClass(Class<?> type) {
        String className = type.getName();
        return className.startsWith("net.milkbowl.vault.")
                || className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.");
    }

    private void instrument(Class<?> targetClass) {
        if (!instrumentation.isModifiableClass(targetClass)) {
            lastInjectionStatus = "Class not modifiable: " + targetClass.getName();
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §c" + lastInjectionStatus);
            return;
        }

        VaultTransformer transformer = new VaultTransformer(targetClass.getName().replace('.', '/'));
        instrumentation.addTransformer(transformer, true);
        try {
            instrumentation.retransformClasses(targetClass);
            if (transformer.isModified()) {
                lastInjectionStatus = "Injected into " + targetClass.getName();
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cInjected EconomyLimit into " + targetClass.getName());
            } else {
                instrumentedClasses.remove(targetClass.getName());
                lastInjectionStatus = transformer.getErrorMessage() == null
                        ? "No bytecode changes applied to " + targetClass.getName()
                        : "Injection failed for " + targetClass.getName() + ": " + transformer.getErrorMessage();
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §c" + lastInjectionStatus);
            }
        } catch (Throwable throwable) {
            instrumentedClasses.remove(targetClass.getName());
            lastInjectionStatus = "Retransform failed for " + targetClass.getName() + ": " + throwable.getMessage();
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §cFailed to inject into " + targetClass.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        } finally {
            instrumentation.removeTransformer(transformer);
        }
    }

    private static final class VaultTransformer implements ClassFileTransformer {

        private final String targetInternalName;
        private volatile boolean modified;
        private volatile String errorMessage;

        private VaultTransformer(String targetInternalName) {
            this.targetInternalName = targetInternalName;
        }

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            if (!targetInternalName.equals(className)) {
                return null;
            }

            try {
                ClassPool pool = new ClassPool(true);
                if (loader != null) {
                    pool.appendClassPath(new LoaderClassPath(loader));
                }
                if (Economy.class.getClassLoader() != null) {
                    pool.appendClassPath(new LoaderClassPath(Economy.class.getClassLoader()));
                }
                if (EconomyResponse.class.getClassLoader() != null) {
                    pool.appendClassPath(new LoaderClassPath(EconomyResponse.class.getClassLoader()));
                }
                if (Bukkit.class.getClassLoader() != null) {
                    pool.appendClassPath(new LoaderClassPath(Bukkit.class.getClassLoader()));
                }
                pool.appendClassPath(new ClassClassPath(Economy.class));
                pool.appendClassPath(new ClassClassPath(EconomyResponse.class));
                pool.appendClassPath(new ClassClassPath(Bukkit.class));
                pool.appendClassPath(new ClassClassPath(VaultDepositBridge.class));

                javassist.CtClass ctClass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
                boolean modified = false;

                for (CtMethod method : ctClass.getDeclaredMethods()) {
                    if (!method.getName().equals("depositPlayer")) {
                        continue;
                    }
                    String descriptor = method.getMethodInfo().getDescriptor();
                    if (!returnsEconomyResponse(descriptor)) {
                        continue;
                    }
                    int amountIndex = resolveAmountIndex(descriptor);
                    if (amountIndex < 1) {
                        continue;
                    }

                    method.insertBefore(beforeCode(amountIndex));
                    method.insertAfter(afterCode());
                    method.addCatch(catchCode(), pool.get("java.lang.Throwable"));
                    modified = true;
                }

                if (!modified) {
                    ctClass.detach();
                    return null;
                }

                byte[] bytecode = ctClass.toBytecode();
                ctClass.detach();
                this.modified = true;
                return bytecode;
            } catch (Throwable throwable) {
                this.errorMessage = throwable.getClass().getName() + ": " + throwable.getMessage();
                throw new IllegalClassFormatException(throwable.getMessage());
            }
        }

        public boolean isModified() {
            return modified;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        private boolean returnsEconomyResponse(String descriptor) {
            return descriptor.endsWith(")Lnet/milkbowl/vault/economy/EconomyResponse;");
        }

        private int resolveAmountIndex(String descriptor) {
            String parameters = descriptor.substring(1, descriptor.indexOf(')'));
            int count = 0;
            int i = 0;
            char lastType = 0;
            while (i < parameters.length()) {
                char current = parameters.charAt(i);
                if (current == 'L') {
                    int end = parameters.indexOf(';', i);
                    if (end == -1) {
                        return -1;
                    }
                    i = end + 1;
                    count++;
                    lastType = 'L';
                    continue;
                }
                if (current == '[') {
                    i++;
                    while (i < parameters.length() && parameters.charAt(i) == '[') {
                        i++;
                    }
                    if (i < parameters.length() && parameters.charAt(i) == 'L') {
                        int end = parameters.indexOf(';', i);
                        if (end == -1) {
                            return -1;
                        }
                        i = end + 1;
                    } else {
                        i++;
                    }
                    count++;
                    lastType = '[';
                    continue;
                }
                i++;
                count++;
                lastType = current;
            }
            return lastType == 'D' ? count : -1;
        }

        private String beforeCode(int amountIndex) {
            return "{"
                    + "try {"
                    + "  org.bukkit.plugin.Plugin __economyLimitPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin(\"EconomyLimit\");"
                    + "  if (__economyLimitPlugin != null && __economyLimitPlugin.isEnabled()) {"
                    + "    ClassLoader __economyLimitLoader = __economyLimitPlugin.getClass().getClassLoader();"
                    + "    Class __economyLimitBridge = Class.forName(\"cn.superiormc.economylimit.inject.VaultDepositBridge\", true, __economyLimitLoader);"
                    + "    java.lang.reflect.Method __economyLimitBefore = __economyLimitBridge.getMethod(\"beforeDeposit\", new java.lang.Class[] { java.lang.Object[].class });"
                    + "    java.lang.Object __economyLimitDecision = __economyLimitBefore.invoke(null, new java.lang.Object[] { $args });"
                    + "    if (__economyLimitDecision != null) {"
                    + "      if (((java.lang.Boolean) ((java.lang.Object[]) __economyLimitDecision)[0]).booleanValue()) {"
                    + "        return ($r) ((java.lang.Object[]) __economyLimitDecision)[1];"
                    + "      }"
                    + "      $" + amountIndex + " = ((java.lang.Double) ((java.lang.Object[]) __economyLimitDecision)[2]).doubleValue();"
                    + "    }"
                    + "  }"
                    + "} catch (Throwable __economyLimitIgnored) {"
                    + "  try {"
                    + "    org.bukkit.plugin.Plugin __economyLimitPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin(\"EconomyLimit\");"
                    + "    if (__economyLimitPlugin != null) {"
                    + "      ClassLoader __economyLimitLoader = __economyLimitPlugin.getClass().getClassLoader();"
                    + "      Class __economyLimitBridge = Class.forName(\"cn.superiormc.economylimit.inject.VaultDepositBridge\", true, __economyLimitLoader);"
                    + "      java.lang.reflect.Method __economyLimitError = __economyLimitBridge.getMethod(\"recordError\", new java.lang.Class[] { java.lang.Throwable.class });"
                    + "      __economyLimitError.invoke(null, new java.lang.Object[] { __economyLimitIgnored });"
                    + "    }"
                    + "  } catch (Throwable __economyLimitIgnored2) {"
                    + "  }"
                    + "}"
                    + "}";
        }

        private String afterCode() {
            return "{"
                    + "try {"
                    + "  org.bukkit.plugin.Plugin __economyLimitPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin(\"EconomyLimit\");"
                    + "  if (__economyLimitPlugin != null && __economyLimitPlugin.isEnabled()) {"
                    + "    ClassLoader __economyLimitLoader = __economyLimitPlugin.getClass().getClassLoader();"
                    + "    Class __economyLimitBridge = Class.forName(\"cn.superiormc.economylimit.inject.VaultDepositBridge\", true, __economyLimitLoader);"
                    + "    java.lang.reflect.Method __economyLimitAfter = __economyLimitBridge.getMethod(\"afterCurrent\", new java.lang.Class[] { java.lang.Object.class, java.lang.Throwable.class });"
                    + "    __economyLimitAfter.invoke(null, new java.lang.Object[] { $_, null });"
                    + "  }"
                    + "} catch (Throwable __economyLimitIgnored) {"
                    + "  try {"
                    + "    org.bukkit.plugin.Plugin __economyLimitPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin(\"EconomyLimit\");"
                    + "    if (__economyLimitPlugin != null) {"
                    + "      ClassLoader __economyLimitLoader = __economyLimitPlugin.getClass().getClassLoader();"
                    + "      Class __economyLimitBridge = Class.forName(\"cn.superiormc.economylimit.inject.VaultDepositBridge\", true, __economyLimitLoader);"
                    + "      java.lang.reflect.Method __economyLimitError = __economyLimitBridge.getMethod(\"recordError\", new java.lang.Class[] { java.lang.Throwable.class });"
                    + "      __economyLimitError.invoke(null, new java.lang.Object[] { __economyLimitIgnored });"
                    + "    }"
                    + "  } catch (Throwable __economyLimitIgnored2) {"
                    + "  }"
                    + "}"
                    + "}";
        }

        private String catchCode() {
            return "{"
                    + "try {"
                    + "  org.bukkit.plugin.Plugin __economyLimitPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin(\"EconomyLimit\");"
                    + "  if (__economyLimitPlugin != null && __economyLimitPlugin.isEnabled()) {"
                    + "    ClassLoader __economyLimitLoader = __economyLimitPlugin.getClass().getClassLoader();"
                    + "    Class __economyLimitBridge = Class.forName(\"cn.superiormc.economylimit.inject.VaultDepositBridge\", true, __economyLimitLoader);"
                    + "    java.lang.reflect.Method __economyLimitAfter = __economyLimitBridge.getMethod(\"afterCurrent\", new java.lang.Class[] { java.lang.Object.class, java.lang.Throwable.class });"
                    + "    __economyLimitAfter.invoke(null, new java.lang.Object[] { null, $e });"
                    + "  }"
                    + "} catch (Throwable __economyLimitIgnored) {"
                    + "  try {"
                    + "    org.bukkit.plugin.Plugin __economyLimitPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin(\"EconomyLimit\");"
                    + "    if (__economyLimitPlugin != null) {"
                    + "      ClassLoader __economyLimitLoader = __economyLimitPlugin.getClass().getClassLoader();"
                    + "      Class __economyLimitBridge = Class.forName(\"cn.superiormc.economylimit.inject.VaultDepositBridge\", true, __economyLimitLoader);"
                    + "      java.lang.reflect.Method __economyLimitError = __economyLimitBridge.getMethod(\"recordError\", new java.lang.Class[] { java.lang.Throwable.class });"
                    + "      __economyLimitError.invoke(null, new java.lang.Object[] { __economyLimitIgnored });"
                    + "    }"
                    + "  } catch (Throwable __economyLimitIgnored2) {"
                    + "  }"
                    + "}"
                    + "throw $e;"
                    + "}";
        }
    }

    public String getLastInjectionStatus() {
        return lastInjectionStatus;
    }
}
