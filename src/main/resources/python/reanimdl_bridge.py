"""Run reanimdl with configurable origins and HTTP diagnostics on stderr."""
import json
import os
import sys
import time
import aiohttp
import reanimdl.api
import reanimdl.flixcloud

reanimdl.api.ReAnimeClient.__init__.__defaults__ = (os.environ["REANIME_API_URL"].rstrip("/"),)
reanimdl.flixcloud.FLIX_BASE = os.environ["FLIXCLOUD_API_URL"].rstrip("/")
reanimdl.flixcloud.MEDIA_HEADERS["referer"] = reanimdl.flixcloud.FLIX_BASE + "/"
_request = aiohttp.ClientSession._request
_read = aiohttp.ClientResponse.read
_limit = int(os.environ.get("ERGO_API_LOG_LIMIT", "16384"))
_enabled = os.environ.get("ERGO_API_LOG_ENABLED") == "true"


def emit(response, data=None, error=None, event=None):
    if not _enabled:
        return
    try:
        event = event if event is not None else getattr(response, "_ergo_event", None)
        if event is None or event.get("logged"):
            return
        event["logged"] = True
        event["durationMs"] = int((time.monotonic() - event.pop("started")) * 1000)
        event["status"] = response.status if response is not None else 0
        event["error"] = error
        event["contentType"] = response.content_type if response is not None else ""
        event["truncated"] = data is not None and len(data) > _limit
        event["body"] = data.decode("utf-8", errors="replace") if data is not None and len(data) <= _limit else ""
        print("__ERGO_HTTP__" + json.dumps(event, ensure_ascii=True), file=sys.stderr, flush=True)
    except Exception:
        pass


async def request(self, method, uri, **kwargs):
    event = {"uri": str(uri), "method": method, "started": time.monotonic(),
             "request": {"parameters": kwargs.get("params"), "body": kwargs.get("json")}}
    try:
        response = await _request(self, method, uri, **kwargs)
    except Exception as error:
        emit(None, error=type(error).__name__, event=event)
        raise
    response._ergo_event = event
    # Never pre-read video or HLS content. JSON is logged when reanimdl reads it.
    if "json" not in response.content_type:
        emit(response)
    return response


async def read(self):
    try:
        data = await _read(self)
    except Exception as error:
        emit(self, error=type(error).__name__)
        raise
    emit(self, data=data if "json" in self.content_type else None)
    return data


aiohttp.ClientSession._request = request
aiohttp.ClientResponse.read = read
from reanimdl.cli import main
main()
