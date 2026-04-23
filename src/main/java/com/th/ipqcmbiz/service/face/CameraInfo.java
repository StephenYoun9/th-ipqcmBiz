package com.th.ipqcmbiz.service.face;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CameraInfo {
    private int width;
    private int height;
    private double frameRate;
    private String resolution;
    private boolean running;
}