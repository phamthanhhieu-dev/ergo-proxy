package com.ergoproxy.platform.service;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.subtitle.PreparedSubtitleArchiveDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleArchiveItemRequestDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleArchiveRequestDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleFileDTO;
import com.ergoproxy.platform.exception.LogicException;
import com.ergoproxy.platform.util.Messages;
import com.ergoproxy.platform.util.SubtitleDiagnostics;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class SubtitleArchiveServiceImpl implements SubtitleArchiveService {

    private final OpenSubtitleService openSubtitleService;

    @Override
    public PreparedSubtitleArchiveDTO prepare(String payload) throws Exception {
        SubtitleArchiveRequestDTO archive;
        try {
            if (payload.length() > 200000) {
                throw new LogicException("PSA_00001", "subtitle.archive.request.invalid");
            }
            archive = new ObjectMapper().readValue(payload, SubtitleArchiveRequestDTO.class);
            boolean invalidName = archive.getName() == null || archive.getName().isBlank() || archive.getName().length() > 200;
            boolean invalidFiles = archive.getFiles() == null || archive.getFiles().isEmpty() || archive.getFiles().size() > 2000;
            if (invalidName || invalidFiles) {
                throw new LogicException("PSA_00002", "subtitle.archive.request.invalid");
            }
            for (SubtitleArchiveItemRequestDTO item : archive.getFiles()) {
                if (item == null || item.getFileId() < 1 || item.getEpisode() < 1 || item.getEpisode() > 10000) {
                    throw new LogicException("PSA_00003", "subtitle.archive.request.invalid");
                }
            }
            if (archive.getMissing() != null) {
                if (archive.getMissing().size() > 2000) {
                    throw new LogicException("PSA_00004", "subtitle.archive.request.invalid");
                }
                for (Integer episode : archive.getMissing()) {
                    if (episode == null || episode < 1 || episode > 10000) {
                        throw new LogicException("PSA_00005", "subtitle.archive.request.invalid");
                    }
                }
            }
        } catch (Exception ex) {
            if (ex instanceof LogicException existingLogicException) {
                throw existingLogicException;
            }
            throw new LogicException(ex, "PSA_00006", "subtitle.archive.request.invalid");
        }
        // Fetch the first file before attachment headers so quota/auth failures return a readable
        // error.
        SubtitleFileDTO first = openSubtitleService.download(archive.getFiles().getFirst().getFileId());
        String name = archive.getName().replaceAll(Const.Regex.UNSAFE_FILENAME_CHARACTERS, Const.Common.UNDERSCORE);
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(Const.Common.ZIP_EXTENSION)) {
            name += Const.Common.ZIP_EXTENSION;
        }

        PreparedSubtitleArchiveDTO response = new PreparedSubtitleArchiveDTO(archive, first, name);
        return response;
    }

    @Override
    public void write(PreparedSubtitleArchiveDTO prepared, OutputStream output) throws IOException {
        SubtitleArchiveRequestDTO archive = prepared.getArchive();
        SubtitleFileDTO first = prepared.getFirst();

        List<String> reports = new ArrayList<String>();
        if (archive.getMissing() != null) {
            for (Integer episode : archive.getMissing()) {
                String message = Messages.get("subtitle.episode.not.found", episode);
                reports.add(message);
            }
        }
        Set<String> names = new HashSet<String>();
        long size = 0;
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            for (int i = 0; i < archive.getFiles().size(); i++) {
                SubtitleArchiveItemRequestDTO item = archive.getFiles().get(i);
                SubtitleFileDTO file;
                try {
                    file = i == 0 ? first : openSubtitleService.download(item.getFileId());
                } catch (Exception ex) {
                    reports.add(Messages.get("subtitle.episode.failed", item.getEpisode(), SubtitleDiagnostics.sanitize(Messages.fromException(ex))));
                    reports.add(Messages.get("subtitle.archive.remaining.not.downloaded"));
                    break;
                }
                if (size + file.getBytes().length > 100L * 1024 * 1024) {
                    reports.add(Messages.get("subtitle.archive.size.exceeded"));
                    break;
                }
                String entry = file.getFileName().replaceAll(Const.Regex.UNSAFE_FILENAME_CHARACTERS, Const.Common.UNDERSCORE);
                if (!names.add(entry)) {
                    entry = Const.File.EPISODE_FILE_PREFIX + item.getEpisode() + Const.Common.HYPHEN + i + Const.Common.HYPHEN + entry;
                    names.add(entry);
                }
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(file.getBytes());
                zip.closeEntry();
                zip.flush();
                size += file.getBytes().length;
            }
            if (!reports.isEmpty()) {
                String report = Const.File.SUBTITLE_REPORT;
                while (names.contains(report)) {
                    report = Const.Common.UNDERSCORE + report;
                }
                zip.putNextEntry(new ZipEntry(report));
                zip.write(String.join(Const.Common.NEWLINE, reports).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadArchive(String payload) throws Exception {
        PreparedSubtitleArchiveDTO prepared = prepare(payload);
        ResponseEntity<StreamingResponseBody> response = ResponseEntity.ok().contentType(MediaType.parseMediaType(Const.Common.APPLICATION_ZIP))
            .header(HttpHeaders.CACHE_CONTROL, Const.Common.HTTP_NO_STORE)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(prepared.getName(), StandardCharsets.UTF_8).build().toString())
            .body(output -> write(prepared, output));
        return response;
    }
}
