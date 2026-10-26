package com.ergoproxy.platform.service;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.exception.LogicException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NativeFilePickerServiceImpl implements NativeFilePickerService {

    private final MessageSource messageSource;

    @Value("${playback.picker-command:powershell.exe}")
    private String pickerCommand;

    @Override
    public Path choose(String type, Path initialDirectory) {
        if (!Set.of(Const.NativePicker.FILE, Const.NativePicker.FOLDER).contains(type)) {
            throw new LogicException("CNFP_00001", "library.picker.type.invalid");
        }
        if (!System.getProperty(Const.NativePicker.OS_NAME).toLowerCase(Locale.ROOT).startsWith(Const.NativePicker.WINDOWS)) {
            throw new LogicException("CNFP_00002", "library.picker.windows.required");
        }
        Path script = null;
        Path output = null;
        Process process = null;
        try {
            script = Files.createTempFile(Const.NativePicker.TEMP_PREFIX, Const.NativePicker.SCRIPT_EXTENSION);
            output = Files.createTempFile(Const.NativePicker.TEMP_PREFIX, Const.NativePicker.OUTPUT_EXTENSION);
            ClassPathResource resource = new ClassPathResource(Const.NativePicker.RESOURCE);
            try (InputStream input = resource.getInputStream()) {
                Files.copy(input, script, StandardCopyOption.REPLACE_EXISTING);
            }
            String titleKey = Const.NativePicker.FOLDER.equals(type) ? "library.picker.folder.title" : "library.picker.video.title";
            String title = messageSource.getMessage(titleKey, null, Locale.forLanguageTag(Const.Common.VI));
            List<String> command = new ArrayList<>();
            command.add(pickerCommand);
            command.addAll(Const.NativePicker.COMMAND_OPTIONS);
            command.add(script.toString());
            command.add(Const.NativePicker.MODE_OPTION);
            command.add(type);
            command.add(Const.NativePicker.DIRECTORY_OPTION);
            command.add(initialDirectory.toAbsolutePath().toString());
            command.add(Const.NativePicker.TITLE_OPTION);
            command.add(title);
            process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            if (!process.waitFor(10, TimeUnit.MINUTES)) {
                throw new LogicException("CNFP_00003", "library.picker.timeout");
            }
            if (process.exitValue() != 0 || Files.size(output) > Const.NativePicker.MAX_OUTPUT_BYTES) {
                throw new LogicException("CNFP_00004", "library.picker.failed");
            }
            String value = Files.readString(output, StandardCharsets.UTF_8).strip();
            Path response = null;
            if (!value.isBlank()) {
                response = Path.of(value);
            }
            return response;
        } catch (LogicException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LogicException(exception, "CNFP_00005", "library.picker.failed");
        } catch (Exception exception) {
            throw new LogicException(exception, "CNFP_00006", "library.picker.failed");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            deleteTemporaryFile(script);
            deleteTemporaryFile(output);
        }
    }

    private void deleteTemporaryFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (java.io.IOException ignored) {
            /* File tạm còn bị Windows giữ sẽ được hệ điều hành dọn sau. */
        }
    }
}
