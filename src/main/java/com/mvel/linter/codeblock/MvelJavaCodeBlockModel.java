package com.mvel.linter.codeblock;

import com.intellij.openapi.util.TextRange;
import com.mvel.linter.compiler.MvelDiagnostic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MvelJavaCodeBlockModel {
    private final String prefix;
    private final String javaText;
    private final String suffix;
    private final int[] javaToHostOffsets;
    private final int[] hostToJavaOffsets;
    private final List<MvelDiagnostic> diagnostics;

    public MvelJavaCodeBlockModel(
            @NotNull String prefix,
            @NotNull String javaText,
            @NotNull String suffix,
            int[] javaToHostOffsets,
            int[] hostToJavaOffsets,
            @NotNull List<MvelDiagnostic> diagnostics
    ) {
        this.prefix = prefix;
        this.javaText = javaText;
        this.suffix = suffix;
        this.javaToHostOffsets = javaToHostOffsets;
        this.hostToJavaOffsets = hostToJavaOffsets;
        this.diagnostics = diagnostics;
    }

    public @NotNull String prefix() {
        return prefix;
    }

    public @NotNull String javaText() {
        return javaText;
    }

    public @NotNull String suffix() {
        return suffix;
    }

    public @NotNull List<MvelDiagnostic> diagnostics() {
        return diagnostics;
    }

    public int mapJavaToHostOffset(int javaOffset) {
        if (javaToHostOffsets.length == 0) {
            return 0;
        }
        int normalized = clamp(javaOffset, 0, javaToHostOffsets.length - 1);
        return javaToHostOffsets[normalized];
    }

    public int mapHostToJavaOffset(int hostOffset) {
        if (hostToJavaOffsets.length == 0) {
            return 0;
        }
        int normalized = clamp(hostOffset, 0, hostToJavaOffsets.length - 1);
        int mapped = hostToJavaOffsets[normalized];
        return mapped >= 0 ? mapped : 0;
    }

    public @NotNull TextRange mapJavaRangeToHost(int javaStart, int javaEnd) {
        int hostStart = mapJavaToHostOffset(javaStart);
        int hostEnd = mapJavaToHostOffset(Math.max(javaStart, javaEnd));
        if (hostEnd < hostStart) {
            hostEnd = hostStart;
        }
        return new TextRange(hostStart, hostEnd);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
