package com.ergoproxy.platform.common.constant;

public final class Const {

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
    }

    public static final class Common {
        public static final String EMPTY = "";
        public static final String BASE_PACKAGE = "com.ergoproxy";
        public static final String API_LOGGER = BASE_PACKAGE + ".api";
        public static final String TOKEN = "token";
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String VI = "vi";
        public static final String JSON = "json";
        public static final String DEFAULT_PAGE_INDEX = "0";
        public static final String DEFAULT_PAGE_SIZE = "20";
        public static final String EPISODE = "episode";
    }

    public static final class Version {
        public static final String V1 = "/v1";
    }

    public static final class Path {

        public static final String API = "/api";
        public static final String VIDEO = "/video";
        public static final String SUBTITLE = "/subtitle";
        public static final String SEARCH = "/search";
        public static final String GET_DETAIL = "/get-detail";
        public static final String GET_EMBED_URL = "/get-embed-url";
        public static final String GET_EPISODES = "/get-episodes";
    }

    public static final class Source {
        //subtitle
        public static final String OPEN_SUBTITLE = "open_subtitle";
        //website
        public static final String REANIME = "reanime";
        public static final String ANIMEVSUB = "animevsub";
        //host
        public static final String FLIX_CLOUD = "flix_cloud";
        //cache
        public static final String REDIS = "redis";
    }

    public static final class CacheKey {
        public static final String VIDEO_DATA = "video:data";
        public static final String SEARCH_UUIDS = "search:uuids";
        ;
    }

    public static final class CacheField {
        public static final String VIDEO = "video:";
        public static final String SEARCH = "search:";
    }

    public static class Formatter {
        //Date
        public static final String YYYY = "yyyy";
        public static final String YYYY_MM_DASH = "yyyy-MM";
        public static final String YYYY_MM_SLASH = "yyyy/MM";
        public static final String YYYY_MM_DD = "yyyyMMdd";
        public static final String YYYY_MM_DD_DASH = "yyyy-MM-dd";
        public static final String YYYY_MM_DD_SLASH = "yyyy/MM/dd";
        public static final String DD_MM_YYYY_SLASH = "dd/MM/yyyy";
        public static final String DD_MM_YYYY_DASH = "dd-MM-yyyy";
        //Time
        public static final String HH_MM_SS = "HHmmss";
        public static final String HH_MM_SS_COLON = "HH:mm:ss";
        //Split
        public static final String SPLIT_STR_DOT = ".";
        public static final String SPLIT_STR_UNDERSCORE = "_";
        public static final String SPLIT_STR_SEMICOLON = ";";
        public static final String SPLIT_STR_HYPHEN = "-";
        public static final String SPLIT_STR_SLASH = "/";
        public static final String SPLIT_STR_SPACE = " ";
        public static final String SPLIT_STR_PLUS = "+";
        public static final String SPLIT_STR_COLON = ":";
        public static final String SPLIT_STR_COMMA = ",";
        public static final String SPLIT_STR_QUESTION_MARK = "?";
        public static final String SPLIT_STR_OPEN_BRACE = "{";
        public static final String SPLIT_STR_CLOSE_BRACE = "}";
    }
}
