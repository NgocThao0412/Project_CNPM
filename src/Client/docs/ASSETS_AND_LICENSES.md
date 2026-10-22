# Assets and dependency provenance

All three campus illustrations and the SGU text mark are newly authored SVG resources. They are illustrative, not photographs of SGU or a reproduction of the official university logo. Colors are a blue/sky-blue interpretation requested by the user; no claim of measured official brand hex values.

The two bundled MP3s are locally synthesized readings of original demo narration text using eSpeak, encoded with ffmpeg. They are explicitly labeled as sample narration, not official school content. No commercial music or third-party speech recording was copied. The Vietnamese/English transcript is seeded in NarrationDAO and matches the demo purpose.

No font files are bundled. CSS uses system fonts already on the user's device.

MySQL Connector/J 9.7.0 was extracted unchanged from the user's original supplied backend JAR. Its embedded notices/licenses remain intact; see lib/licenses for copies. Original Spring loader/dependency JARs are reused only in the integrated project's prebuilt-backend, retaining their embedded notices.

Optional OpenStreetMap iframe uses OpenStreetMap's hosted viewer and attribution. It needs Internet; the local Java-rendered POI schematic remains usable without map tiles. Official reference material consulted for standard Java server / browser behavior:
- https://docs.oracle.com/en/java/javase/21/docs/api/jdk.httpserver/com/sun/net/httpserver/HttpServer.html
- https://developer.mozilla.org/en-US/docs/Web/Media/Guides/Audio_and_video_delivery/Cross-browser_audio_basics
- https://developer.mozilla.org/docs/Learn_web_development/Core/CSS_layout/Media_queries
