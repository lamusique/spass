
# Spaß
A Mock Service Server over HTTP

## name origin
Spaß /ʃpaːs/

↑

Mozart(1787), _Ein Musikalischer Spaß_

↑

mocquere

late Middle English: from Old French mocquer ‘deride’; Modern French ‘mocque’ /mɔ.ke/

↑

mock


## How to run
- Dev Mode for browser-reload mode:
```bash
sbt "run 8080"
```
- Debug Mode; To run in debug mode with the http listener on port 8080, run:
```bash
sbt -jvm-debug 9999 "run 8080"
```
- Prod Mode
```bash
sbt "start -Dhttp.port=8080"
```

