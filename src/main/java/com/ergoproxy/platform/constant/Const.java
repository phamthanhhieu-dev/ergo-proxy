package com.ergoproxy.platform.constant;

public final class Const {

    private Const() {
    }

    public static final class Subtitle {

        public static final int CONTENT_INSPECTION_LIMIT = 16384;
        public static final java.util.Set<String> SUPPORTED_EXTENSIONS = java.util.Set.of(
            Common.SRT,
            Common.ASS,
            Common.SSA,
            Common.VTT,
            Common.SUB,
            Common.TXT,
            Common.SMI,
            Common.MPL
        );

        public static final String BEARER_PREFIX = "Bearer ";
        public static final java.time.Duration TOKEN_CACHE_DURATION = java.time.Duration.ofMinutes(10);
        public static final int MAX_SEASON = 1000;
        public static final int MAX_EPISODE = 10000;
        public static final int MAX_PAGE = 100;
        public static final int MAX_API_REDIRECTS = 6;
        public static final int MAX_FILE_REDIRECTS = 4;
        public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
        public static final java.util.Set<String> SUPPORTED_LANGUAGES = java.util.Set.of(Common.VI, Common.EN, Common.JA);

        public static final String API_KEY_HEADER = "Api-Key";
        public static final String USER_AGENT = "ErgoProxy v1.0.0";
        public static final String SEARCH_LOG_PREFIX = "search language=";
        public static final String SEASON_LOG_PREFIX = " season=";
        public static final String EPISODE_LOG_PREFIX = " episode=";
        public static final String PAGE_LOG_PREFIX = " page=";
        public static final String DOWNLOAD_LOG_PREFIX = "download fileId=";
        public static final String API_HOST = "api.opensubtitles.com";
        public static final String VIP_API_HOST = "vip-api.opensubtitles.com";
        public static final String DEFAULT_FILENAME_PREFIX = "subtitle-";
        public static final String DEFAULT_FILENAME = "subtitle";
        public static final String BYTE_ORDER_MARK = "\ufeff";
        public static final String ASS_SCRIPT_HEADER = "[Script Info]";
        public static final String ASS_EVENTS_HEADER = "[Events]";
        public static final String SSA_STYLES_HEADER = "[V4 Styles]";
        public static final String ASS_STYLES_HEADER = "[V4+ Styles]";
        public static final String VTT_HEADER = "WEBVTT";
        public static final String COM_SUBDOMAIN_SUFFIX = ".opensubtitles.com";
        public static final String ORG_SUBDOMAIN_SUFFIX = ".opensubtitles.org";

        private Subtitle() {
        }
    }

    public static final class File {

        public static final String DEFAULT_ANIME_FOLDER = "Anime";
        public static final String PARENT_DIRECTORY = "..";
        public static final String EPISODE_FOLDER_PREFIX = "/EP";
        public static final String EPISODE_PREFIX = "EP";
        public static final String EPISODE_FILE_PREFIX = "E";
        public static final String VIDEO_ERROR_REPORT = "VIDEO-ERROR.txt";
        public static final String SUBTITLE_ERROR_REPORT = "SUBTITLE-ERROR.txt";
        public static final String DOWNLOAD_REPORT_PATH = "/DOWNLOAD-REPORT.txt";
        public static final String SUBTITLE_REPORT = "SUBTITLE-REPORT.txt";
        public static final String DEFAULT_SUBTITLE_FILENAME = "subtitle.txt";
        public static final String SUBTITLE_ERROR_LABEL = " sub: ";

        private File() {
        }
    }

    public static final class Environment {

        public static final String PYTHON_IO_ENCODING = "PYTHONIOENCODING";
        public static final String REANIME_API_URL = "REANIME_API_URL";
        public static final String FLIXCLOUD_API_URL = "FLIXCLOUD_API_URL";
        public static final String API_LOG_ENABLED = "ERGO_API_LOG_ENABLED";
        public static final String API_LOG_LIMIT = "ERGO_API_LOG_LIMIT";

        private Environment() {
        }
    }

    public static final class Python {

        public static final String BRIDGE_RESOURCE = "python/reanimdl_bridge.py";
        public static final String UNBUFFERED_OPTION = "-u";
        public static final String STDIN_SCRIPT = "-";
        public static final String OPTION_PREFIX = "-";
        public static final String EPISODE_OPTION = "-e";
        public static final String QUALITY_OPTION = "-q";
        public static final String OUTPUT_OPTION = "-o";
        public static final String SEARCH_LIMIT = "20";
        public static final String DOWNLOAD_WORKERS = "8";
        public static final int MAX_QUERY_LENGTH = 200;
        public static final int PROGRESS_BUFFER_LIMIT = 16000;
        public static final int MAX_OUTPUT_CHARACTERS = 8 * 1024 * 1024;
        public static final int ERROR_OUTPUT_LIMIT = 3000;
        public static final int OUTPUT_WAIT_SECONDS = 10;
        public static final int STOP_WAIT_SECONDS = 5;
        public static final int SUCCESS_EXIT_CODE = 0;
        public static final java.time.Duration JSON_TIMEOUT = java.time.Duration.ofMinutes(3);
        public static final java.time.Duration DOWNLOAD_TIMEOUT = java.time.Duration.ofHours(2);

        private Python() {
        }
    }

    public static final class Regex {

        public static final String DIAGNOSTIC_URL = "https?://[^\\s\"<>]+";
        public static final String DIAGNOSTIC_BEARER = "(?i)(bearer\\s+)[^\\s,;]+";
        public static final String DIAGNOSTIC_CREDENTIAL = "(?i)([\"']?(?:api[-_]?key|password|token|authorization)[\"']?\\s*[:=]\\s*)[\"']?[^\\s,;}]+";
        public static final String DIAGNOSTIC_WHITESPACE = "[\\r\\n\\t]";

        public static final String FILENAME_EDGE_SPACES = "^[. ]+|[. ]+$";
        public static final String WINDOWS_RESERVED_FILENAME = "(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?";
        public static final String SRT_TIMESTAMP = "\\d{2}:\\d{2}:\\d{2},\\d{3}\\s*-->";

        public static final String IMDB_ID = "(?:tt)?[0-9]{1,12}";

        public static final String UNSAFE_FILENAME_CHARACTERS = "[\\\\/:*?\"<>|\\p{Cntrl}]";
        public static final String IMDB_PREFIX = "^tt";
        public static final String FLIXCLOUD_DOWNLOAD_PATH = "/d/[a-zA-Z0-9_-]+/?";
        public static final String FLIXCLOUD_FETCH_HOST = "fetch[0-9]+\\.flixcloud\\.cc";
        public static final String FILE_IDENTIFIER = "[a-zA-Z0-9-]+";

        public static final String ANSI_ESCAPE = "\u001B\\[[0-?]*[ -/]*[@-~]";
        public static final String TRAILING_SLASH = "/$";
        public static final String ANIME_SLUG = "[a-zA-Z0-9-]{1,180}";
        public static final String FLIXCLOUD_EMBED_PATH = "/e/[a-zA-Z0-9_-]+/?";

        private Regex() {
        }
    }

    public static final class Common {
        public static final String EMBED_URL = "embedUrl";

        public static final String APPLICATION_ZIP = "application/zip";
        public static final String HEADER_SUBTITLE_FILENAME = "X-Subtitle-Filename";

        public static final String VIDEO_MATROSKA = "video/x-matroska";
        public static final String APPLICATION_SUBRIP = "application/x-subrip";

        public static final String UNDERSCORE = "_";
        public static final String HYPHEN = "-";
        public static final String COLON_SPACE = ": ";
        public static final String OPEN_PARENTHESIS = " (";
        public static final String CLOSE_PARENTHESIS_PERIOD = ").";
        public static final String QUERY_TOKEN_PREFIX = "?token=";
        public static final String SSE_DATA_PREFIX = "data:";

        public static final String REANIME_NAME = "ReAnime";
        public static final String SPACE = " ";
        public static final String NEWLINE = "\n";
        public static final String SOFTSUB_PREFIX = "s-";

        public static final String COMMAND_INFO = "info";
        public static final String COMMAND_SEARCH = "search";
        public static final String COMMAND_EPISODES = "episodes";
        public static final String OPTION_JSON = "--json";
        public static final String OPTION_YES = "--yes";
        public static final String OPTION_LIMIT = "--limit";
        public static final String OPTION_AUDIO = "--audio";
        public static final String OPTION_WORKERS = "--workers";
        public static final String PYTHON_HTTP_LOG_PREFIX = "__ERGO_HTTP__";
        public static final String APPLICATION_JSON = "application/json";
        public static final String HTTP_NO_STORE = "no-store";
        public static final String DOWNLOAD_ATTACHMENT = "attachment";
        public static final String UTF_8 = "utf-8";

        public static final String BASE_PACKAGE = "com.ergoproxy";
        public static final String API_LOGGER = BASE_PACKAGE + ".api";

        public static final String EMPTY = "";
        public static final String DOT = ".";
        public static final String SLASH = "/";
        public static final String FLIXCLOUD_HOST = "flixcloud.cc";
        public static final String REANIME_HOST = "reanime.to";
        public static final String OPENSUBTITLES_HOST = "opensubtitles.com";
        public static final String OPENSUBTITLES_ORG_HOST = "opensubtitles.org";

        public static final String ID = "id";
        public static final String NAME = "name";
        public static final String DATA = "data";
        public static final String STATUS = "status";
        public static final String CODE = "code";
        public static final String MESSAGE = "message";
        public static final String ERROR = "error";
        public static final String BODY = "body";
        public static final String REQUEST = "request";
        public static final String RESPONSE = "response";
        public static final String METHOD = "method";
        public static final String URI = "uri";
        public static final String CONTENT_TYPE = "contentType";
        public static final String DURATION_MS = "durationMs";
        public static final String TRUNCATED = "truncated";
        public static final String REANIME = "reanime";
        public static final String SOURCE = "source";
        public static final String QUERY = "query";
        public static final String TOKEN = "token";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String CONFIGURED = "configured";
        public static final String AUTHENTICATED = "authenticated";
        public static final String ANIME_ID = "anime_id";
        public static final String ANILIST_ID = "anilist_id";
        public static final String ANILIST = "anilist";
        public static final String EPISODE_NUMBER = "episode_number";
        public static final String PLAYABLE = "playable";
        public static final String SERVERS = "servers";
        public static final String DATA_TYPE = "dataType";
        public static final String SERVER_NAME = "serverName";
        public static final String DATA_LINK = "dataLink";
        public static final String NODES = "nodes";
        public static final String VIDEO = "video";
        public static final String DOWNLOAD = "download";
        public static final String FILENAME = "filename";
        public static final String FILE_ID_CAMEL = "fileId";
        public static final String FILE_ID = "file_id";
        public static final String FILE_NAME = "file_name";
        public static final String BASE = "base";
        public static final String PERCENT = "percent";
        public static final String LINK = "link";
        public static final String LANGUAGE = "language";
        public static final String LANGUAGES = "languages";
        public static final String PAGE = "page";
        public static final String TYPE = "type";
        public static final String MOVIE = "movie";
        public static final String EPISODE = "episode";
        public static final String IMDB_ID = "imdb_id";
        public static final String PARENT_IMDB_ID = "parent_imdb_id";
        public static final String SEASON_NUMBER = "season_number";
        public static final String TOTAL_PAGES = "total_pages";
        public static final String ATTRIBUTES = "attributes";
        public static final String FEATURE_DETAILS = "feature_details";
        public static final String FEATURE_TYPE = "feature_type";
        public static final String MOVIE_TYPE = "Movie";
        public static final String FILES = "files";
        public static final String TITLE = "title";
        public static final String MOVIE_NAME = "movie_name";
        public static final String RELEASE = "release";
        public static final String DOWNLOAD_COUNT = "download_count";
        public static final String HEARING_IMPAIRED = "hearing_impaired";
        public static final String QUEUED = "QUEUED";
        public static final String RUNNING = "RUNNING";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
        public static final String WAITING = "WAITING";
        public static final String PREPARING = "PREPARING";
        public static final String READY = "READY";
        public static final String DOWNLOADING = "DOWNLOADING";
        public static final String SOURCE_READY = "ready";
        public static final String SOURCE_FAILED = "failed";
        public static final String VI = "vi";
        public static final String EN = "en";
        public static final String JA = "ja";
        public static final String SUB = "sub";
        public static final String DUB = "dub";
        public static final String DUAL = "dual";
        public static final String HTTPS = "https";
        public static final String JSON = "json";
        public static final String HTML = "html";
        public static final String ASS = "ass";
        public static final String SSA = "ssa";
        public static final String SRT = "srt";
        public static final String VTT = "vtt";
        public static final String TXT = "txt";
        public static final String SMI = "smi";
        public static final String MPL = "mpl";
        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String DELETE = "DELETE";
        public static final String MKV_EXTENSION = ".mkv";
        public static final String SRT_EXTENSION = ".srt";
        public static final String ZIP_EXTENSION = ".zip";

        private Common() {
        }
    }

    public static final class Version {

        public static final String V1 = "/v1";

        private Version() {
        }
    }

    public static final class Playback {

        public static final java.util.Set<String> VIDEO_EXTENSIONS = java.util.Set.of("mp4", "m4v", "mkv", "webm", "mov", "avi", "ts");
        public static final java.util.Set<String> SUBTITLE_EXTENSIONS = java.util.Set.of(Common.ASS, Common.SSA, Common.SRT, Common.VTT);
        public static final java.util.Set<String> FONT_EXTENSIONS = java.util.Set.of("ttf", "otf", "woff", "woff2");
        public static final String EPISODE_PATTERN = "(?i)^EP(?:ISODE)?[ _-]*(\\d+)$";

        private Playback() {
        }
    }

    public static final class Path {

        public static final String OPENSUBTITLES_SEARCH = "/subtitles";
        public static final String EMBED = "/e/";
        public static final String DOWNLOAD_PAGE = "/d/";
        public static final String ANIME = "/anime/";
        public static final String FLIX = "/flix/";
        public static final String LOGIN = "/login";
        public static final String DOWNLOAD = "/download";
        public static final String PROGRESS = "/progress";
        public static final String PAGE_DATA = "/__data.json";
        public static final String API = "/api";
        public static final String PLAYBACK = Path.API + Version.V1 + "/playback";
        public static final String ORIGINAL = "/original";
        public static final String SUBTITLE = "/subtitle";
        public static final String BUNDLE = "/bundle";
        public static final String SEARCH = "/search";
        public static final String NATIVE = "/native";
        public static final String NATIVE_ARCHIVE = "/native-archive";

        private Path() {
        }
    }
}
