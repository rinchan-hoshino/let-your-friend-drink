package dev.rinchan.letyourfrienddrink;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Automated architecture contract tests scanning real built mod JARs and source files:
 * 1. Dedicated Server isolation (zero client/GUI constant-pool references outside *.client.* packages).
 * 2. Mixin bidirectional closure (*.mixins.json <-> JAR .class <-> source @Mixin).
 * 3. Clean artifact redline (zero rinlib or com/terraformersmc/modmenu stub leaks).
 */
final class ArchitectureContractTest {
    private static final List<String> FORBIDDEN_SERVER_REFS = List.of(
        "net/minecraft/client/",
        "net/neoforged/neoforge/client/",
        "net/minecraftforge/client/",
        "com/terraformersmc/modmenu/",
        "dev/rinchan/letyourfrienddrink/client/",
        "FriendDrinkConfigScreen"
    );

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern MIXIN_ANNOTATION_PATTERN = Pattern.compile("@Mixin\\s*\\(");
    private static final Pattern JSON_PACKAGE_PATTERN = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"([^\"]+)\"");

    @Test
    void dedicatedServerClassesNeverReferenceClientOrGuiClasses() throws IOException {
        List<Path> jars = discoverBuiltJars();
        List<String> violations = new ArrayList<>();

        for (Path jarPath : jars) {
            try (ZipFile zf = new ZipFile(jarPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.endsWith(".class") || isClientPackageClass(name)) {
                        continue;
                    }
                    byte[] bytes;
                    try (InputStream in = zf.getInputStream(entry)) {
                        bytes = in.readAllBytes();
                    }
                    for (String utf8 : readConstantPoolUtf8s(bytes)) {
                        for (String forbidden : FORBIDDEN_SERVER_REFS) {
                            if (utf8.contains(forbidden)) {
                                violations.add(jarPath.getFileName() + " :: " + name + " -> " + utf8);
                            }
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Dedicated Server isolation violations found:\n" + String.join("\n", violations)
        );
    }

    @Test
    void mixinBidirectionalClosureHoldsAcrossJarsAndSourceTree() throws IOException {
        Path root = locateRepoRoot();
        List<Path> jars = discoverBuiltJars();
        List<String> violations = new ArrayList<>();

        // 1. Forward + JAR-level reverse check: every *.mixins.json entry exists in JAR,
        //    and every top-level class inside that mixin package in the JAR is registered.
        for (Path jarPath : jars) {
            try (ZipFile zf = new ZipFile(jarPath.toFile())) {
                Set<String> entryNames = new HashSet<>();
                List<ZipEntry> mixinConfigs = new ArrayList<>();
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    entryNames.add(entry.getName());
                    if (entry.getName().endsWith(".mixins.json")) {
                        mixinConfigs.add(entry);
                    }
                }
                assertFalse(mixinConfigs.isEmpty(), "No *.mixins.json found in " + jarPath.getFileName());

                for (ZipEntry configEntry : mixinConfigs) {
                    String json;
                    try (InputStream in = zf.getInputStream(configEntry)) {
                        json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    MixinConfig parsed = parseMixinConfig(json, configEntry.getName());
                    String pkgPath = parsed.pkg().replace('.', '/') + "/";

                    for (String mixinClass : parsed.declaredMixins()) {
                        String expectedClassEntry = pkgPath + mixinClass.replace('.', '/') + ".class";
                        if (!entryNames.contains(expectedClassEntry)) {
                            violations.add(
                                jarPath.getFileName() + " :: declared in " + configEntry.getName()
                                    + " but missing from JAR: " + expectedClassEntry
                            );
                        }
                    }

                    for (String entryName : entryNames) {
                        if (entryName.startsWith(pkgPath) && entryName.endsWith(".class") && !entryName.contains("$")) {
                            String rel = entryName.substring(pkgPath.length(), entryName.length() - ".class".length())
                                .replace('/', '.');
                            if (!parsed.declaredMixins().contains(rel)) {
                                violations.add(
                                    jarPath.getFileName() + " :: mixin class in JAR not registered in "
                                        + configEntry.getName() + ": " + rel
                                );
                            }
                        }
                    }
                }
            }
        }

        // 2. Source-level reverse check: every @Mixin-annotated .java class in the repository
        //    must be registered in its loader's *.mixins.json.
        Map<String, Path> loaderConfigs = Map.of(
            "fabric", root.resolve("fabric/src/main/resources/let_your_friend_drink.fabric.mixins.json"),
            "neoforge", root.resolve("neoforge/src/main/templates/let_your_friend_drink.neoforge.mixins.json"),
            "forge", root.resolve("forge/src/main/resources/let_your_friend_drink.forge.mixins.json")
        );
        Map<String, MixinConfig> parsedLoaderConfigs = new HashMap<>();
        for (Map.Entry<String, Path> e : loaderConfigs.entrySet()) {
            assertTrue(Files.isRegularFile(e.getValue()), "Missing mixin config: " + e.getValue());
            parsedLoaderConfigs.put(
                e.getKey(),
                parseMixinConfig(Files.readString(e.getValue(), StandardCharsets.UTF_8), e.getValue().toString())
            );
        }

        int discoveredSourceMixins = 0;
        for (String loader : List.of("fabric", "neoforge", "forge")) {
            Path srcDir = root.resolve(loader + "/src/main/java");
            if (!Files.isDirectory(srcDir)) {
                continue;
            }
            MixinConfig cfg = parsedLoaderConfigs.get(loader);
            try (Stream<Path> walk = Files.walk(srcDir)) {
                List<Path> javaFiles = walk.filter(p -> p.toString().endsWith(".java")).toList();
                for (Path javaFile : javaFiles) {
                    String content = Files.readString(javaFile, StandardCharsets.UTF_8);
                    if (!MIXIN_ANNOTATION_PATTERN.matcher(content).find()) {
                        continue;
                    }
                    discoveredSourceMixins++;
                    Matcher pkgMatcher = PACKAGE_PATTERN.matcher(content);
                    assertTrue(pkgMatcher.find(), "Could not extract package from " + javaFile);
                    String pkg = pkgMatcher.group(1);
                    String simpleName = javaFile.getFileName().toString().replace(".java", "");
                    String fqcn = pkg + "." + simpleName;
                    String expectedPrefix = cfg.pkg() + ".";
                    if (!fqcn.startsWith(expectedPrefix)) {
                        violations.add(
                            "Source @Mixin " + fqcn + " is outside declared mixin package " + cfg.pkg()
                        );
                    } else {
                        String relName = fqcn.substring(expectedPrefix.length());
                        if (!cfg.declaredMixins().contains(relName)) {
                            violations.add(
                                "Source @Mixin " + fqcn + " is NOT registered in " + loaderConfigs.get(loader)
                            );
                        }
                    }
                }
            }
        }
        assertTrue(discoveredSourceMixins >= 6, "Expected at least 6 @Mixin source classes, found " + discoveredSourceMixins);

        assertTrue(
            violations.isEmpty(),
            "Mixin bidirectional closure violations found:\n" + String.join("\n", violations)
        );
    }

    @Test
    void packagedJarsContainZeroRinlibOrModMenuStubLeaks() throws IOException {
        List<Path> jars = discoverBuiltJars();
        List<String> violations = new ArrayList<>();

        for (Path jarPath : jars) {
            try (ZipFile zf = new ZipFile(jarPath.toFile())) {
                Enumeration<? extends ZipEntry> entries = zf.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (lower.contains("rinlib")) {
                        violations.add(jarPath.getFileName() + " :: forbidden rinlib entry: " + name);
                    }
                    if (lower.contains("com/terraformersmc/modmenu/")) {
                        violations.add(jarPath.getFileName() + " :: forbidden modmenu stub leak: " + name);
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Artifact purity violations found:\n" + String.join("\n", violations)
        );
    }

    private static boolean isClientPackageClass(String entryName) {
        return entryName.contains("/client/") || entryName.startsWith("client/");
    }

    private static Path locateRepoRoot() {
        String configured = System.getProperty("lyfd.repo.root");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        Path cur = Path.of("").toAbsolutePath().normalize();
        while (cur != null) {
            if (Files.isRegularFile(cur.resolve("profiles.json")) && Files.isRegularFile(cur.resolve("settings.gradle"))) {
                return cur;
            }
            cur = cur.getParent();
        }
        throw new IllegalStateException("Could not locate repository root containing profiles.json");
    }

    private static List<Path> discoverBuiltJars() throws IOException {
        Path root = locateRepoRoot();
        String explicitDir = System.getProperty("lyfd.audit.dir");
        if (explicitDir != null && !explicitDir.isBlank()) {
            Path dir = Path.of(explicitDir);
            if (!dir.isAbsolute()) {
                dir = root.resolve(dir);
            }
            List<Path> found = listModJars(dir);
            assertFalse(found.isEmpty(), "No built JARs found in lyfd.audit.dir=" + dir);
            return found;
        }

        Path releasesDir = root.resolve("build/releases");
        if (Files.isDirectory(releasesDir)) {
            List<Path> releaseJars = listModJars(releasesDir);
            if (!releaseJars.isEmpty()) {
                return releaseJars;
            }
        }

        Path profilesDir = root.resolve("build/profiles");
        List<Path> profileJars = new ArrayList<>();
        if (Files.isDirectory(profilesDir)) {
            try (Stream<Path> walk = Files.walk(profilesDir, 4)) {
                walk.filter(Files::isRegularFile)
                    .filter(p -> p.getParent() != null && p.getParent().getFileName().toString().equals("libs"))
                    .filter(ArchitectureContractTest::isFinalModJar)
                    .sorted()
                    .forEach(profileJars::add);
            }
        }
        assertFalse(
            profileJars.isEmpty(),
            "No built mod JARs found in build/releases or build/profiles! Build at least one target first."
        );
        return profileJars;
    }

    private static List<Path> listModJars(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> list = Files.list(dir)) {
            return list.filter(Files::isRegularFile)
                .filter(ArchitectureContractTest::isFinalModJar)
                .sorted()
                .toList();
        }
    }

    private static boolean isFinalModJar(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".jar")
            && !name.endsWith("-sources.jar")
            && !name.endsWith("-javadoc.jar")
            && !name.endsWith("-dev.jar")
            && !name.endsWith("-dev-shadow.jar");
    }

    private record MixinConfig(String pkg, Set<String> declaredMixins) {}

    private static MixinConfig parseMixinConfig(String json, String sourceLabel) {
        Matcher pkgMatcher = JSON_PACKAGE_PATTERN.matcher(json);
        assertTrue(pkgMatcher.find(), "Missing \"package\" in " + sourceLabel);
        String pkg = pkgMatcher.group(1);

        Set<String> declared = new LinkedHashSet<>();
        for (String key : List.of("mixins", "client", "server")) {
            Pattern arrayPattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]", Pattern.DOTALL);
            Matcher m = arrayPattern.matcher(json);
            if (m.find()) {
                Matcher item = JSON_STRING_PATTERN.matcher(m.group(1));
                while (item.find()) {
                    declared.add(item.group(1));
                }
            }
        }
        return new MixinConfig(pkg, declared);
    }

    /** Parses a JVM .class file constant pool and returns all CONSTANT_Utf8 strings. */
    private static List<String> readConstantPoolUtf8s(byte[] classBytes) throws IOException {
        List<String> utf8s = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(classBytes))) {
            int magic = in.readInt();
            if (magic != 0xCAFEBABE) {
                throw new IOException("Invalid class magic: 0x" + Integer.toHexString(magic));
            }
            in.readUnsignedShort(); // minor
            in.readUnsignedShort(); // major
            int cpCount = in.readUnsignedShort();
            for (int idx = 1; idx < cpCount; idx++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1 -> utf8s.add(in.readUTF()); // CONSTANT_Utf8
                    case 3, 4 -> in.skipBytes(4);      // Integer, Float
                    case 5, 6 -> {                     // Long, Double (takes 2 slots)
                        in.skipBytes(8);
                        idx++;
                    }
                    case 7, 8, 16, 19, 20 -> in.skipBytes(2); // Class, String, MethodType, Module, Package
                    case 9, 10, 11, 12, 17, 18 -> in.skipBytes(4); // Fieldref, Methodref, InterfaceMethodref, NameAndType, Dynamic, InvokeDynamic
                    case 15 -> in.skipBytes(3);        // MethodHandle
                    default -> throw new IOException("Unsupported constant pool tag: " + tag);
                }
            }
        }
        return utf8s;
    }
}
