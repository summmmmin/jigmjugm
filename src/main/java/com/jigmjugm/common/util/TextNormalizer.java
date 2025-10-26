package com.jigmjugm.common.util;

public final class TextNormalizer {
    private TextNormalizer() {}
    /** 앞뒤 공백 제거 + 중간 다중 공백 1칸 */
    public static String normalizeTitle(String title) {
        if (title == null) return null;
        String trimTitle = title.trim().replaceAll("\\s+", " ");
        if (trimTitle.isEmpty()) throw new IllegalArgumentException("제목은 비어있을 수 없습니다.");
        return trimTitle;
    }
}
