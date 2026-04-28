package com.th.ipqcmbiz.entity.vo;

import lombok.Data;

@Data
public class FaceFrameInfo {
    private String jpegBase64;
    private FaceRect face;
    private boolean hasFace;

    @Data
    public static class FaceRect {
        private int x;
        private int y;
        private int width;
        private int height;

        public FaceRect() {}

        public FaceRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}