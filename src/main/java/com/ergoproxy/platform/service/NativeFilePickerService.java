package com.ergoproxy.platform.service;

import java.nio.file.Path;

public interface NativeFilePickerService {
    Path choose(String type, Path initialDirectory);
}
