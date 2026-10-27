package com.ergoproxy.platform.service;

import com.ergoproxy.platform.config.client.OpenSubtitleHttpClient;
import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.dto.subtitle.SubtitleCandidateResponseDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleDownloadRequestDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleFileDTO;
import com.ergoproxy.platform.dto.subtitle.SubtitleSearchResponseDTO;
import com.ergoproxy.platform.exception.LogicException;
import com.ergoproxy.platform.util.Messages;
import com.ergoproxy.platform.util.SubtitleDiagnostics;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OpenSubtitleServiceImpl implements OpenSubtitleService {

    private final ObjectMapper objectMapper;
    private final OpenSubtitleHttpClient restClient;
    @Value("${opensubtitles.api.url}")
    private String openSubtitlesApiUrl;
    @Value("${opensubtitles.api-key:}")
    private String apiKey;
    @Value("${opensubtitles.username:}")
    private String username;
    @Value("${opensubtitles.password:}")
    private String password;
    @Value("${opensubtitles.token:}")
    private String configuredToken;
    private String token = Const.Common.EMPTY;
    private Instant tokenExpires = Instant.EPOCH;
    private boolean configuredTokenRejected;

    static void validateApiLink(URI uri) {
        String host = Objects.toString(uri.getHost(), Const.Common.EMPTY).toLowerCase(Locale.ROOT);
        String path = uri.getPath();
        String apiPrefix = Const.Path.API + Const.Version.V1 + Const.Common.SLASH;
        boolean secureScheme = Const.Common.HTTPS.equalsIgnoreCase(uri.getScheme());
        boolean allowedPort = uri.getPort() == -1 || uri.getPort() == 443;
        boolean allowedHost = host.equals(Const.Subtitle.API_HOST) || host.equals(Const.Subtitle.VIP_API_HOST);
        boolean apiPath = path != null && path.startsWith(apiPrefix);
        if (!secureScheme || !allowedPort || !allowedHost || !apiPath || uri.getUserInfo() != null) {
            throw new LogicException("VALOS_00001", "subtitle.api.redirect.invalid");
        }
    }

    static SubtitleSearchResponseDTO parseSearch(JsonNode data, String language, int season, int episode, boolean movie) {
        List<SubtitleCandidateResponseDTO> results = new ArrayList<SubtitleCandidateResponseDTO>();
        if (data != null) {
            for (JsonNode item : data.path(Const.Common.DATA)) {
                JsonNode attributes = item.path(Const.Common.ATTRIBUTES),
                    feature = attributes.path(Const.Common.FEATURE_DETAILS);
                if (!language.equals(attributes.path(Const.Common.LANGUAGE).asText())) {
                    continue;
                }
                if (
                    !movie && (feature.path(Const.Common.SEASON_NUMBER).asInt(-1) != season || feature.path(Const.Common.EPISODE_NUMBER).asInt(-1) != episode)
                ) {
                    continue;
                }
                if (movie && !Const.Common.MOVIE_TYPE.equalsIgnoreCase(feature.path(Const.Common.FEATURE_TYPE).asText())) {
                    continue;
                }
                for (JsonNode file : attributes.path(Const.Common.FILES)) {
                    if (file.path(Const.Common.FILE_ID).asLong() > 0) {
                        results.add(
                            new SubtitleCandidateResponseDTO(
                                file.path(Const.Common.FILE_ID).asLong(),
                                file.path(Const.Common.FILE_NAME).asText(),
                                feature.path(Const.Common.TITLE).asText(feature.path(Const.Common.MOVIE_NAME).asText()),
                                attributes.path(Const.Common.RELEASE).asText(),
                                language,
                                feature.path(Const.Common.SEASON_NUMBER).asInt(),
                                feature.path(Const.Common.EPISODE_NUMBER).asInt(),
                                attributes.path(Const.Common.DOWNLOAD_COUNT).asInt(),
                                attributes.path(Const.Common.HEARING_IMPAIRED).asBoolean()
                            )
                        );
                    }
                }
            }
        }
        SubtitleSearchResponseDTO response = new SubtitleSearchResponseDTO(results, data == null ? 0 : data.path(Const.Common.TOTAL_PAGES).asInt(1));
        return response;
    }

    static String subtitleFilename(String original, byte[] bytes) {
        String filename = original.replaceAll(Const.Regex.UNSAFE_FILENAME_CHARACTERS, Const.Common.UNDERSCORE);
        if (filename.isBlank()) {
            filename = Const.Subtitle.DEFAULT_FILENAME;
        }
        int inspectedBytes = Math.min(bytes.length, Const.Subtitle.CONTENT_INSPECTION_LIMIT);
        String content = new String(bytes, 0, inspectedBytes, StandardCharsets.UTF_8);
        content = content.replace(Const.Subtitle.BYTE_ORDER_MARK, Const.Common.EMPTY);
        String detectedExtension = detectSubtitleExtension(content);
        int dot = filename.lastIndexOf('.');
        String existingExtension = Const.Common.EMPTY;
        if (dot >= 0) {
            existingExtension = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        }
        if (detectedExtension == null) {
            if (Const.Subtitle.SUPPORTED_EXTENSIONS.contains(existingExtension)) {
                return filename;
            }
            detectedExtension = Const.Common.TXT;
        }
        String baseName = filename;
        if (dot >= 0) {
            baseName = filename.substring(0, dot);
        }
        String response = baseName + Const.Common.DOT + detectedExtension;
        return response;
    }

    private static String detectSubtitleExtension(String content) {
        String response = null;
        boolean assHeader = content.contains(Const.Subtitle.ASS_SCRIPT_HEADER) || content.contains(Const.Subtitle.ASS_EVENTS_HEADER);
        if (assHeader) {
            boolean legacyStyles = content.contains(Const.Subtitle.SSA_STYLES_HEADER);
            boolean modernStyles = content.contains(Const.Subtitle.ASS_STYLES_HEADER);
            if (legacyStyles && !modernStyles) {
                response = Const.Common.SSA;
            } else {
                response = Const.Common.ASS;
            }
        } else if (content.stripLeading().startsWith(Const.Subtitle.VTT_HEADER)) {
            response = Const.Common.VTT;
        } else {
            Pattern timestamp = Pattern.compile(Const.Regex.SRT_TIMESTAMP);
            Matcher matcher = timestamp.matcher(content);
            if (matcher.find()) {
                response = Const.Common.SRT;
            }
        }
        return response;
    }

    static void validateLink(URI link) {
        String host = Objects.toString(link.getHost(), Const.Common.EMPTY).toLowerCase(Locale.ROOT);
        boolean secureScheme = Const.Common.HTTPS.equals(link.getScheme());
        boolean allowedPort = link.getPort() == -1 || link.getPort() == 443;
        boolean comDomain = host.equals(Const.Common.OPENSUBTITLES_HOST) || host.endsWith(Const.Subtitle.COM_SUBDOMAIN_SUFFIX);
        boolean orgDomain = host.equals(Const.Common.OPENSUBTITLES_ORG_HOST) || host.endsWith(Const.Subtitle.ORG_SUBDOMAIN_SUFFIX);
        boolean allowedHost = comDomain || orgDomain;
        if (!secureScheme || !allowedPort || !allowedHost || link.getUserInfo() != null) {
            throw new LogicException("VLOS_00001", "subtitle.download.host.invalid");
        }
    }

    private String apiUrl() {
        String response = openSubtitlesApiUrl + Const.Path.API + Const.Version.V1;
        return response;
    }

    @Override
    public Map<String, Boolean> configuration() {
        boolean configured = !apiKey.isBlank();
        boolean accountAvailable = !username.isBlank() && !password.isBlank();
        boolean authenticated = !configuredToken.isBlank() || accountAvailable;
        Map<String, Boolean> response = new LinkedHashMap<>();
        response.put(Const.Common.CONFIGURED, configured);
        response.put(Const.Common.AUTHENTICATED, authenticated);
        return response;
    }

    private void requireKey() {
        if (apiKey.isBlank()) {
            throw new LogicException("RKOS_00001", "subtitle.api.key.is.null.or.empty");
        }
    }

    private void configureHeaders(RestClient.RequestHeadersSpec<?> request, String bearer) {
        request.header(Const.Subtitle.API_KEY_HEADER, apiKey);
        request.header(HttpHeaders.USER_AGENT, Const.Subtitle.USER_AGENT);
        request.accept(MediaType.APPLICATION_JSON);
        if (!bearer.isBlank()) {
            String authorization = Const.Subtitle.BEARER_PREFIX + bearer;
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private synchronized String bearer() {
        Instant now = Instant.now();
        if (!token.isBlank() && now.isBefore(tokenExpires)) {
            return token;
        }
        if (!configuredToken.isBlank() && !configuredTokenRejected) {
            String response = configuredToken;
            return response;
        }
        if (username.isBlank() || password.isBlank()) {
            String response = Const.Common.EMPTY;
            return response;
        }
        String endpoint = apiUrl() + Const.Path.LOGIN;
        Map<String, String> credentials = new LinkedHashMap<>();
        credentials.put(Const.Common.USERNAME, username);
        credentials.put(Const.Common.PASSWORD, password);
        RestClient.RequestBodySpec request = restClient.post().uri(endpoint);
        configureHeaders(request, Const.Common.EMPTY);
        request.contentType(MediaType.APPLICATION_JSON);
        request.body(credentials);
        RestClient.ResponseSpec upstream = request.retrieve();
        JsonNode data = upstream.body(JsonNode.class);
        if (data == null || data.path(Const.Common.TOKEN).asText().isBlank()) {
            throw new LogicException("BOS_00001", "subtitle.login.token.is.null.or.empty");
        }
        token = data.path(Const.Common.TOKEN).asText();
        tokenExpires = now.plus(Const.Subtitle.TOKEN_CACHE_DURATION);
        return token;
    }

    @Override
    public SubtitleSearchResponseDTO search(String query, String language, int season, int episode, String imdb, boolean movie, int page) {
        requireKey();
        validateSearch(query, language, season, episode, imdb, page);
        try {
            URI endpoint = createSearchUri(query, language, season, episode, imdb, movie, page);
            JsonNode data = searchJson(endpoint);
            SubtitleSearchResponseDTO response = parseSearch(data, language, season, episode, movie);
            return response;
        } catch (Exception exception) {
            LogicException failure = subtitleFailure(exception);
            throw failure;
        }
    }

    private void validateSearch(String query, String language, int season, int episode, String imdb, int page) {
        boolean validQuery = query != null && !query.isBlank() && query.length() <= Const.Python.MAX_QUERY_LENGTH;
        boolean validLanguage = Const.Subtitle.SUPPORTED_LANGUAGES.contains(language);
        boolean validSeason = season >= 1 && season <= Const.Subtitle.MAX_SEASON;
        boolean validEpisode = episode >= 1 && episode <= Const.Subtitle.MAX_EPISODE;
        boolean validPage = page >= 1 && page <= Const.Subtitle.MAX_PAGE;
        boolean validImdb = imdb == null || imdb.isBlank() || imdb.matches(Const.Regex.IMDB_ID);
        if (!validQuery || !validLanguage || !validSeason || !validEpisode || !validPage || !validImdb) {
            throw new LogicException("VSOS_00001", "subtitle.search.invalid");
        }
    }

    private URI createSearchUri(String query, String language, int season, int episode, String imdb, boolean movie, int page) {
        String endpoint = apiUrl() + Const.Path.OPENSUBTITLES_SEARCH;
        String featureType = Const.Common.EPISODE;
        if (movie) {
            featureType = Const.Common.MOVIE;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endpoint);
        builder.queryParam(Const.Common.LANGUAGES, language);
        builder.queryParam(Const.Common.PAGE, page);
        builder.queryParam(Const.Common.TYPE, featureType);
        if (imdb == null || imdb.isBlank()) {
            builder.queryParam(Const.Common.QUERY, query.toLowerCase(Locale.ROOT));
        } else {
            String imdbField = Const.Common.PARENT_IMDB_ID;
            if (movie) {
                imdbField = Const.Common.IMDB_ID;
            }
            String imdbId = imdb.replaceFirst(Const.Regex.IMDB_PREFIX, Const.Common.EMPTY);
            builder.queryParam(imdbField, imdbId);
        }
        if (!movie) {
            builder.queryParam(Const.Common.SEASON_NUMBER, season);
            builder.queryParam(Const.Common.EPISODE_NUMBER, episode);
        }
        URI response = builder.build().encode().toUri();
        return response;
    }

    private JsonNode searchJson(URI initial) {
        URI current = initial;
        for (int hop = 0; hop < Const.Subtitle.MAX_API_REDIRECTS; hop++) {
            validateApiLink(current);
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(current);
            configureHeaders(request, Const.Common.EMPTY);
            ApiReply reply = request.exchange(this::readSearchResponse);
            if (reply.redirect() == null) {
                JsonNode response = reply.data();
                return response;
            }
            URI target = current.resolve(reply.redirect());
            validateApiLink(target);
            if (target.equals(current)) {
                throw new LogicException("SJOS_00001", "subtitle.redirect.repeated");
            }
            current = target;
        }
        throw new LogicException("SJOS_00002", "subtitle.redirect.too.many");
    }

    private ApiReply readSearchResponse(HttpRequest request, ClientHttpResponse upstream) throws IOException {
        if (upstream.getStatusCode().is3xxRedirection()) {
            URI location = upstream.getHeaders().getLocation();
            if (location == null) {
                throw new LogicException("RSROS_00001", "subtitle.redirect.location.is.null.or.empty");
            }
            ApiReply response = new ApiReply(location, null);
            return response;
        }
        if (!upstream.getStatusCode().is2xxSuccessful()) {
            int status = upstream.getStatusCode().value();
            String message = Messages.get("subtitle.http.failed", status);
            throw new LogicException("RSROS_00002", "subtitle.http.failed", status);
        }
        JsonNode data = objectMapper.readTree(upstream.getBody());
        ApiReply response = new ApiReply(null, data);
        return response;
    }

    @Override
    public SubtitleFileDTO download(long fileId) {
        requireKey();
        if (fileId < 1) {
            throw new LogicException("DOS_00001", "subtitle.file.id.invalid");
        }
        try {
            JsonNode metadata = requestDownload(fileId);
            String downloadLink = metadata.path(Const.Common.LINK).asText();
            URI link = URI.create(downloadLink);
            byte[] bytes = downloadFile(link);
            String defaultName = Const.Subtitle.DEFAULT_FILENAME_PREFIX + fileId;
            String originalName = metadata.path(Const.Common.FILE_NAME).asText(defaultName);
            String filename = subtitleFilename(originalName, bytes);
            SubtitleFileDTO response = new SubtitleFileDTO(bytes, filename);
            return response;
        } catch (Exception exception) {
            LogicException failure = subtitleFailure(exception);
            throw failure;
        }
    }

    private synchronized JsonNode requestDownload(long fileId) {
        String bearerToken = bearer();
        try {
            JsonNode response = requestDownloadWithToken(fileId, bearerToken);
            return response;
        } catch (RestClientResponseException exception) {
            boolean unauthorized = exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value();
            boolean accountAvailable = !username.isBlank() && !password.isBlank();
            if (!unauthorized || !accountAvailable) {
                LogicException failure = apiError(exception);
                throw failure;
            }
            // A rejected token must never be selected again, including an ENV token.
            configuredTokenRejected = true;
            token = Const.Common.EMPTY;
            tokenExpires = Instant.EPOCH;
            String refreshedToken = bearer();
            JsonNode response = requestDownloadWithToken(fileId, refreshedToken);
            return response;
        }
    }

    private JsonNode requestDownloadWithToken(long fileId, String bearerToken) {
        String endpoint = apiUrl() + Const.Path.DOWNLOAD;
        Map<String, Long> parameters = new LinkedHashMap<>();
        parameters.put(Const.Common.FILE_ID, fileId);
        RestClient.RequestBodySpec request = restClient.post().uri(endpoint);
        configureHeaders(request, bearerToken);
        request.contentType(MediaType.APPLICATION_JSON);
        request.body(parameters);
        RestClient.ResponseSpec upstream = request.retrieve();
        JsonNode response = upstream.body(JsonNode.class);
        if (response == null || response.path(Const.Common.LINK).asText().isBlank()) {
            throw new LogicException("RDWTOS_00001", "subtitle.download.link.is.null.or.empty");
        }
        return response;
    }

    private byte[] downloadFile(URI initial) {
        URI current = initial;
        for (int hop = 0; hop < Const.Subtitle.MAX_FILE_REDIRECTS; hop++) {
            validateLink(current);
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(current);
            FileReply reply = request.exchange(this::readFileResponse);
            if (reply.redirect() == null) {
                byte[] response = reply.bytes();
                if (response.length == 0 || response.length > Const.Subtitle.MAX_FILE_BYTES) {
                    throw new LogicException("DFOS_00001", "subtitle.download.file.invalid");
                }
                return response;
            }
            current = current.resolve(reply.redirect());
        }
        throw new LogicException("DFOS_00002", "subtitle.download.file.invalid");
    }

    private FileReply readFileResponse(HttpRequest request, ClientHttpResponse upstream) throws IOException {
        if (upstream.getStatusCode().is3xxRedirection()) {
            URI location = upstream.getHeaders().getLocation();
            if (location == null) {
                throw new LogicException("RFROS_00001", "subtitle.download.redirect.invalid");
            }
            FileReply response = new FileReply(location, null);
            return response;
        }
        if (!upstream.getStatusCode().is2xxSuccessful()) {
            throw new LogicException("RFROS_00002", "subtitle.download.failed");
        }
        byte[] bytes = upstream.getBody().readNBytes(Const.Subtitle.MAX_FILE_BYTES + 1);
        FileReply response = new FileReply(null, bytes);
        return response;
    }

    private LogicException subtitleFailure(Exception exception) {
        Exception failure = exception;
        if (exception instanceof RestClientResponseException upstream) {
            failure = apiError(upstream);
        }
        LogicException response = SubtitleDiagnostics.failure(failure);
        return response;
    }

    private LogicException apiError(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        LogicException response;
        switch (status) {
            case 401, 403 -> response = new LogicException(exception, "AEOS_00001", "subtitle.authentication.failed");
            case 406, 429 -> response = new LogicException(exception, "AEOS_00002", "subtitle.quota.exceeded");
            default -> response = new LogicException(exception, "AEOS_00003", "subtitle.http.failed", status);
        }
        return response;
    }

    @Override
    public ResponseEntity<byte[]> nativeDownload(long fileId) {
        if (fileId < 1) {
            throw new LogicException("NDOS_00001", "subtitle.file.id.invalid");
        }
        SubtitleDownloadRequestDTO request = new SubtitleDownloadRequestDTO(fileId);
        ResponseEntity<byte[]> response = downloadResponse(request);
        return response;
    }

    @Override
    public ResponseEntity<byte[]> downloadResponse(SubtitleDownloadRequestDTO request) {
        SubtitleFileDTO file = download(request.getFileId());
        byte[] bytes = file.getBytes();
        String filename = file.getFileName();
        String encodedFilename = java.net.URLEncoder.encode(filename, StandardCharsets.UTF_8);
        ContentDisposition.Builder dispositionBuilder = ContentDisposition.attachment();
        dispositionBuilder.filename(filename, StandardCharsets.UTF_8);
        ContentDisposition disposition = dispositionBuilder.build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        builder.contentType(MediaType.APPLICATION_OCTET_STREAM);
        builder.contentLength(bytes.length);
        builder.header(Const.Common.HEADER_SUBTITLE_FILENAME, encodedFilename);
        builder.header(HttpHeaders.CACHE_CONTROL, Const.Common.HTTP_NO_STORE);
        builder.header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        ResponseEntity<byte[]> response = builder.body(bytes);
        return response;
    }

    private record ApiReply(URI redirect, JsonNode data) {
    }

    private record FileReply(URI redirect, byte[] bytes) {
    }
}
